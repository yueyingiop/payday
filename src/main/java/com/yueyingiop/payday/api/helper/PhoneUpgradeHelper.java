package com.yueyingiop.payday.api.helper;

import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.PhoneItem;
import com.yueyingiop.payday.init.RegistryEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** 手机升级效果共用的实体、攻击者和击杀判定辅助函数。 */
public final class PhoneUpgradeHelper {
    private static final String DEFAULT_MELEE_HEAL_COOLDOWN_KEY = "payday_melee_heal_cooldown";

    /** 近战回复生命值的计算方式。 */
    public enum HealingMode {
        /** 直接回复指定生命值。 */
        DIRECT,
        /** 按回复者最大生命值百分比回复，{@code 0.20D} 表示20%。 */
        PERCENTAGE
    }

    //#region 属性修饰

    /**
     * 按 Minecraft 属性修饰符的运算类型，将属性修饰应用到一个基础数值上。
     *
     * <p>这里的基础数值可以是伤害、数量或其他需要应用属性修饰的运行时数值，
     * 不要求它本身来自 {@link AttributeInstance} 的基础值。</p>
     *
     * <ul>
     *     <li>{@code ADDITION}：直接加上修饰符数值；</li>
     *     <li>{@code MULTIPLY_BASE}：按基础数值的百分比增加；</li>
     *     <li>{@code MULTIPLY_TOTAL}：按当前累计结果乘以百分比。</li>
     * </ul>
     *
     * @param baseValue 应用修饰前的基础数值
     * @param attribute 属性实例
     * @return 应用所有修饰符后的数值
     */
    public static double applyAttributeModifiers(double baseValue,
                                                  AttributeInstance attribute) {
        double result = baseValue;
        for (AttributeModifier modifier : attribute.getModifiers(
                AttributeModifier.Operation.ADDITION)) {
            result += modifier.getAmount();
        }
        for (AttributeModifier modifier : attribute.getModifiers(
                AttributeModifier.Operation.MULTIPLY_BASE)) {
            result += baseValue * modifier.getAmount();
        }
        for (AttributeModifier modifier : attribute.getModifiers(
                AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            result *= 1.0D + modifier.getAmount();
        }
        return result;
    }

    //#endregion

    //#region 经验与实体判定

    /**
     * 按比例增加玩家击杀生物获得的经验，并向上取整。
     *
     * @param event 生物经验掉落事件
     * @param owner 可能获得经验的实体
     * @param bonus 经验加成比例，例如 {@code 0.25D} 表示增加25%
     */
    public static void applyExperienceBonus(LivingExperienceDropEvent event,
                                             LivingEntity owner, double bonus) {
        if (owner instanceof Player player && event.getAttackingPlayer() == player) {
            event.setDroppedExperience((int) Math.ceil(
                    event.getDroppedExperience() * (1.0D + bonus)));
        }
    }

    /**
     * 统计玩家指定半径内的非友善生物数量。
     *
     * @param player 作为判定中心的玩家
     * @param radius 判定半径
     * @return 半径内非友善生物数量
     */
    public static int countUnfriendly(Player player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), entity ->
                        isUnfriendly(player, entity)
                                && entity.isAlive()
                                && entity.distanceToSqr(player) <= radius * radius).size();
    }

    /**
     * 判断目标是否是当前玩家定义下的非友善生物。
     *
     * @param player 作为敌对关系判断依据的玩家
     * @param target 待判断实体
     * @return 目标是否为非玩家敌对生物
     */
    public static boolean isUnfriendly(Player player, LivingEntity target) {
        return target != player && !(target instanceof Player)
                && (target instanceof Enemy
                || target instanceof NeutralMob neutral && neutral.isAngryAt(player));
    }

    //#endregion

    //#region 攻击来源判定

    /**
     * 从伤害来源解析实际玩家攻击者。
     *
     * <p>同时支持玩家直接造成伤害，以及玩家拥有的投射物造成伤害。</p>
     *
     * @param source 伤害来源
     * @return 解析出的玩家；来源不是玩家攻击时返回 {@code null}
     */
    public static Player getPlayerAttacker(DamageSource source) {
        if (source.getEntity() instanceof Player player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }

    /**
     * 判断伤害来源是否由指定玩家造成。
     *
     * @param source 伤害来源
     * @param player 待匹配玩家
     * @return 是否由指定玩家直接或通过投射物造成
     */
    public static boolean isPlayerAttacker(DamageSource source, Player player) {
        return getPlayerAttacker(source) == player;
    }

    /**
     * 判断一次受伤事件是否由指定实体直接近战造成。
     *
     * @param event 受伤事件
     * @param attacker 预期的近战攻击者
     * @return 伤害来源的直接实体和实体是否都是指定攻击者
     */
    public static boolean isMeleeHit(LivingHurtEvent event, LivingEntity attacker) {
        return event.getSource().getEntity() == attacker && event.getSource().getDirectEntity() == attacker;
    }

    /**
     * 判断本次受伤事件是否带有减伤穿透标记。
     *
     * @param event 受伤事件
     * @return 目标是否被标记为本次伤害无视本模组减伤
     */
    public static boolean isPiercing(LivingHurtEvent event) {
        return event.getEntity().getPersistentData().getBoolean(
                PhoneUpgradeAction.PIERCE_REDUCTION);
    }

    //#endregion

    //#region 生命值与状态效果

    /**
     * 在近战命中后回复攻击者生命值，并使用通用冷却键。
     *
     * @param event 受伤事件
     * @param attacker 近战攻击者，也是生命值回复对象
     * @param amount 回复量；{@link HealingMode#PERCENTAGE} 下为最大生命值比例
     * @param mode 回复效果，直接回复或按最大生命值百分比回复
     * @param cooldownTicks 冷却间隔，单位为游戏刻；小于等于 {@code 0} 表示无冷却
     */
    public static void tryMeleeHeal(LivingHurtEvent event, LivingEntity attacker,
                                    double amount, HealingMode mode, long cooldownTicks) {
        tryMeleeHeal(event, attacker, amount, mode, cooldownTicks,
                DEFAULT_MELEE_HEAL_COOLDOWN_KEY);
    }

    /**
     * 在近战命中后回复攻击者生命值。
     *
     * <p>使用带冷却键的重载，可以让不同升级分别维护自己的冷却时间。</p>
     *
     * @param event 受伤事件
     * @param attacker 近战攻击者，也是生命值回复对象
     * @param amount 回复量；{@link HealingMode#PERCENTAGE} 下为最大生命值比例
     * @param mode 回复效果，直接回复或按最大生命值百分比回复
     * @param cooldownTicks 冷却间隔，单位为游戏刻；小于等于 {@code 0} 表示无冷却
     * @param cooldownKey 存储在攻击者持久化数据中的冷却键
     */
    public static void tryMeleeHeal(LivingHurtEvent event, LivingEntity attacker,
                                    double amount, HealingMode mode, long cooldownTicks,
                                    String cooldownKey) {
        if (!isMeleeHit(event, attacker) || event.getEntity() == attacker
                || amount <= 0.0D || mode == null) {
            return;
        }

        long now = attacker.level().getGameTime();
        String effectiveCooldownKey = cooldownKey == null || cooldownKey.isBlank()
                ? DEFAULT_MELEE_HEAL_COOLDOWN_KEY : cooldownKey;
        if (cooldownTicks > 0L
                && now < attacker.getPersistentData().getLong(effectiveCooldownKey)) {
            return;
        }

        double healing = mode == HealingMode.PERCENTAGE
                ? attacker.getMaxHealth() * amount : amount;
        attacker.heal((float) healing);

        if (cooldownTicks > 0L) {
            attacker.getPersistentData().putLong(effectiveCooldownKey, now + cooldownTicks);
        }
    }

    /**
     * 近战命中目标时给予渗透效果。
     *
     * <p>只有指定实体直接近战命中其他实体时才会触发；不会对攻击者自身施加效果。</p>
     *
     * @param event 受伤事件
     * @param attacker 预期的近战攻击者
     * @param duration 渗透效果持续时间，单位为游戏刻
     */
    public static void applyInfiltrationOnMeleeHit(LivingHurtEvent event,
                                                    LivingEntity attacker, int duration) {
        if (!isMeleeHit(event, attacker) || event.getEntity() == attacker) {
            return;
        }
        event.getEntity().addEffect(new MobEffectInstance(
                RegistryEffects.INFILTRATION.get(), duration, 0, false, true, true));
    }

    //#endregion

    //#region 护甲恢复

    /**
     * 按冷却恢复玩家护甲饰品中每个 ArmorDefinition 的当前护甲值。
     *
     * @param event 生物死亡事件
     * @param player 可能获得护甲恢复的玩家
     * @param amount 每个 ArmorDefinition 恢复的护甲值
     * @param cooldownTicks 冷却间隔，单位为游戏刻；可以为0
     * @param cooldownKey 存储在玩家持久化数据中的冷却键
     */
    public static void restoreArmor(LivingDeathEvent event, Player player,
                                    double amount, long cooldownTicks, String cooldownKey) {
        if (!isPlayerKillOfUnfriendly(event, player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (cooldownTicks > 0L
                && now < player.getPersistentData().getLong(cooldownKey)) {
            return;
        }

        PhoneItem.refreshArmorSlotModifiers(player);
        for (CurioHelper.SlotStack slot : CurioHelper.slots(player, CurioHelper.ARMOR_SLOT)) {
            if (slot.stack().getItem() instanceof ArmorCurioItem curio) {
                new ArmorModifier(ItemStack.EMPTY, slot.stack(), curio, player)
                        .restoreArmor(amount);
            }
        }
        if (cooldownTicks > 0L) {
            player.getPersistentData().putLong(cooldownKey, now + cooldownTicks);
        }
    }

    //#endregion

    //#region 击杀判定

    /**
     * 判断玩家是否击杀了非友善生物。
     *
     * @param event 生物死亡事件
     * @param player 待判断玩家
     * @return 是否由该玩家击杀非友善生物
     */
    public static boolean isPlayerKillOfUnfriendly(LivingDeathEvent event, Player player) {
        return isPlayerAttacker(event.getSource(), player)
                && isUnfriendly(player, event.getEntity());
    }

    /**
     * 判断玩家是否在16格内击杀非玩家生物。
     *
     * @param event 生物死亡事件
     * @param player 待判断玩家
     * @return 是否由该玩家在16格内击杀非玩家生物
     */
    public static boolean isPlayerKillOfNonPlayer(LivingDeathEvent event, Player player) {
        LivingEntity target = event.getEntity();
        return isPlayerAttacker(event.getSource(), player)
                && target != player
                && !(target instanceof Player)
                && target.distanceToSqr(player) <= 256.0D;
    }

    //#endregion
}
