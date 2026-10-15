package com.yueyingiop.payday.api;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** 炮台温度系统。温度由 0 开始，达到上限后使炮台过载。 */
public final class TurretTemperature {
    public static final double INITIAL_TEMPERATURE = 0.0D;
    public static final double ATTACK_HEAT_CHANCE = 0.75D;

    private TurretTemperature() {
    }

    /** 获取当前配置的最大温度。 */
    public static double getMaximumTemperature() {
        return Config.TURRET_MAX_TEMPERATURE.get();
    }

    /** 获取包含过载液加成后的炮台最大温度。 */
    public static double getMaximumTemperature(TurretBlockEntity turret) {
        return turret == null ? getMaximumTemperature() : turret.getMaximumTemperature();
    }

    /** 获取当前配置的过载自然销毁时间，单位为游戏刻。 */
    public static long getOverloadLifetimeTicks() {
        return Config.TURRET_OVERLOAD_DESTROY_TIME_SECONDS.get().longValue() * 20L;
    }

    /** 获取炮台当前温度。 */
    public static double get(TurretBlockEntity turret) {
        return turret.getTemperature();
    }

    /** 设置炮台当前温度，并限制在温度范围内。 */
    public static void set(TurretBlockEntity turret, double value) {
        if (turret.isOverloaded()) {
            return;
        }
        double maximum = getMaximumTemperature(turret);
        turret.setTemperatureInternal(Math.max(INITIAL_TEMPERATURE,
                Math.min(maximum, value)));
        if (turret.getTemperature() >= maximum) {
            turret.triggerOverload();
        }
    }

    /** 增加炮台温度；达到上限时触发过载。 */
    public static void add(TurretBlockEntity turret, double amount) {
        if (turret.isOverloaded() || !Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        set(turret, get(turret) + amount);
    }

    /** 炮台开火后按概率增加温度。 */
    public static boolean tryAddAttackHeat(TurretBlockEntity turret) {
        if (turret.isOverloaded() || turret.getLevel() == null
                || turret.getLevel().random.nextDouble() >= ATTACK_HEAT_CHANCE) {
            return false;
        }
        add(turret, 1.0D);
        return true;
    }

    /** 敌对生物伤害炮台时增加等同于伤害值的温度。玩家伤害不会增加温度。 */
    public static boolean receiveDamage(TurretBlockEntity turret,
                                        LivingEntity attacker, double damage) {
        if (!(attacker instanceof Enemy) || attacker instanceof Player || turret.isOverloaded()
                || !Double.isFinite(damage) || damage <= 0.0D) {
            return false;
        }
        add(turret, damage);
        return true;
    }
}
