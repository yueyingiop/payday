package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 处理肌肉男系列的回血和人肉护盾效果。 */
public final class MuscleEvents {
    public static final String MUSCLE_REDIRECTING = "payday_muscle_redirecting";
    private static final String MUSCLE_HEAL_NEXT = "payday_muscle5_next_heal";
    
    /** 横行霸道：每5秒恢复玩家最大生命值的3%。 */
    public static void tickMuscle5(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        long now = player.level().getGameTime();
        long next = player.getPersistentData().getLong(MUSCLE_HEAL_NEXT);
        if (now >= next) {
            player.heal(player.getMaxHealth() * 0.03F);
            player.getPersistentData().putLong(MUSCLE_HEAL_NEXT, now + 100L);
        }
    }

    /** 人肉护盾：替半径8格内的其他玩家承担5%的最终伤害。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void muscleShield(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide
                || !(event.getEntity() instanceof Player target)
                || event.getAmount() <= 0.0F
                || target.getPersistentData().getBoolean(MUSCLE_REDIRECTING)) {
            return;
        }

        Player protector = target.level().getEntitiesOfClass(
                        Player.class, target.getBoundingBox().inflate(8.0D),
                        player -> player != target && player.isAlive() && !player.isSpectator()
                                && PhoneItem.hasInstalledUpgrade(player, "special_muscle_2"))
                .stream()
                .min(java.util.Comparator.comparingDouble(target::distanceToSqr))
                .orElse(null);
        if (protector == null) {
            return;
        }

        float redirected = event.getAmount() * 0.05F;
        event.setAmount(Math.max(0.0F, event.getAmount() - redirected));
        protector.getPersistentData().putBoolean(MUSCLE_REDIRECTING, true);
        protector.hurt(event.getSource(), redirected);
        protector.getPersistentData().remove(MUSCLE_REDIRECTING);
    }
}
