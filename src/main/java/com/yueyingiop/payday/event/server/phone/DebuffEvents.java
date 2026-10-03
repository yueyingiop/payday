package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.init.RegistryAttributes;
import com.yueyingiop.payday.init.RegistryEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 处理恐惧、渗透、闪避和全伤害免疫等通用战斗效果。 */
public final class DebuffEvents {
    private static final String FEAR_NO_AI = "payday_fear_no_ai";

    /** 恐惧期间禁用生物 AI、移动和攻击目标。 */
    @SubscribeEvent
    public static void fearTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        if (mob.hasEffect(RegistryEffects.FEAR.get())) {
            if (!mob.getPersistentData().contains(FEAR_NO_AI)) {
                mob.getPersistentData().putBoolean(FEAR_NO_AI, mob.isNoAi());
            }
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
        } else if (mob.getPersistentData().contains(FEAR_NO_AI)) {
            mob.setNoAi(mob.getPersistentData().getBoolean(FEAR_NO_AI));
            mob.getPersistentData().remove(FEAR_NO_AI);
        }
    }

    /**
     * 啸叫：每秒以25%的概率使半径64格内的非玩家生物获得30秒恐惧。
     *
     * <p>啸叫本身是注册的药水效果，触发逻辑集中在这里，
     * 不依赖某个具体特殊升级，因此其他来源也可以复用该效果。</p>
     */
    @SubscribeEvent
    public static void howlTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide
                || !player.hasEffect(RegistryEffects.HOWL.get())
                || player.level().getGameTime() % 20L != 0L
                || player.getRandom().nextFloat() >= 0.25F) {
            return;
        }

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(64.0D), entity -> entity != player
                        && !(entity instanceof Player)
                        && entity.isAlive()
                        && entity.distanceToSqr(player) <= 4096.0D)) {
            target.addEffect(new MobEffectInstance(
                    RegistryEffects.FEAR.get(), 600, 0, false, true, true));
        }
    }

    /** 按 DODGE_CHANCE 属性判定并取消本次攻击。属性值10000代表100%。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dodge(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        AttributeInstance attribute = target.getAttribute(RegistryAttributes.DODGE_CHANCE.get());
        if (attribute == null) {
            return;
        }
        double chance = Math.max(0.0D, Math.min(10000.0D, attribute.getValue())) / 10000.0D;
        if (chance > 0.0D && target.getRandom().nextDouble() < chance) {
            event.setCanceled(true);
        }
    }

    /** 渗透效果使本次伤害变为10倍，并在触发后消失。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void infiltration(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !target.hasEffect(RegistryEffects.INFILTRATION.get())) {
            return;
        }
        event.setAmount(event.getAmount() * 10.0F);
        target.removeEffect(RegistryEffects.INFILTRATION.get());
    }

    /** 处理护甲损坏提供的全伤害免疫，以及恐惧生物的攻击限制。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fearAttack(LivingAttackEvent event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity().getPersistentData().getLong(
                ArmorModifier.FULL_IMMUNITY_END) > event.getEntity().level().getGameTime()) {
            event.setCanceled(true);
            return;
        }

        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living
                ? living : null;
        if (attacker == null && event.getSource().getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity living) {
            attacker = living;
        }
        if (attacker != null && !attacker.level().isClientSide
                && attacker.hasEffect(RegistryEffects.FEAR.get())) {
            event.setCanceled(true);
        }
    }

    /** 在伤害流程结束后清理本次使用过的穿透减伤标记。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearPierceMarker(LivingDamageEvent event) {
        event.getEntity().getPersistentData().remove(PhoneUpgradeAction.PIERCE_REDUCTION);
    }
}
