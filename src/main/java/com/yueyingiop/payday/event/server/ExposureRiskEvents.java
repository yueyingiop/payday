package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.exposure.ExposureRisk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Applies exposure risk to mob target acquisition while preserving retaliation targets. */
public final class ExposureRiskEvents {
    private static final double CONTACT_DISTANCE_SQR = 4.0D;

    private ExposureRiskEvents() {
    }

    /**
     * 调整生物仇恨目标。
     * @param event 事件。
     */
    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getNewTarget() instanceof Player player)
                || player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean hostile = mob instanceof Enemy;
        boolean neutralEligible = mob instanceof NeutralMob && ExposureRisk.get(player) >= 50;
        if (!hostile && !neutralEligible) {
            return;
        }
        // 由受伤反击机制指定的目标在任何风险值下都必须保持有效。
        if (mob.getLastHurtByMob() == player) {
            return;
        }
        double multiplier = ExposureRisk.targetDistanceMultiplier(player);
        double allowedDistance = mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
                * multiplier;
        if (mob.distanceToSqr(player) > Math.max(CONTACT_DISTANCE_SQR, allowedDistance * allowedDistance)) {
            event.setNewTarget(null);
        }
    }

    /**
     * Vanilla nearest-target goals never inspect players outside their configured follow range.
     * Recheck once per second so the exposure multiplier can also expand acquisition, not just
     * suppress targets that vanilla already selected.
     */
    @SubscribeEvent
    public static void acquireExpandedTargets(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || event.player.tickCount % 20 != 0 || event.player.isCreative() || event.player.isSpectator()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.player;
        int risk = ExposureRisk.get(player);
        double multiplier = ExposureRisk.targetDistanceMultiplier(player);
        // 32 格是常见敌对生物在原版中的玩家索敌距离。
        double searchRadius = 32.0D * multiplier;
        AABB searchArea = player.getBoundingBox().inflate(searchRadius);
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, searchArea,
                candidate -> candidate.isAlive() && candidate.getTarget() == null
                        && (candidate instanceof Enemy
                        || (risk >= 50 && candidate instanceof NeutralMob)))) {
            double allowedDistance = mob.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE) * multiplier;
            if (mob.distanceToSqr(player) <= Math.max(CONTACT_DISTANCE_SQR, allowedDistance * allowedDistance)) {
                mob.setTarget(player);
            }
        }
    }

    /**
     * 复制玩家永久风险修正值。
     * @param event 事件。
     */
    @SubscribeEvent
    public static void copyPermanentModifier(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        ExposureRisk.setPermanentModifier(event.getEntity(), ExposureRisk.getPermanentModifier(event.getOriginal()));
    }
}
