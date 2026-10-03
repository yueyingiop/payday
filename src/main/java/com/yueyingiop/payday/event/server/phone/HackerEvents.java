package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.init.RegistryAttributes;
import com.yueyingiop.payday.init.RegistryEffects;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

/** 处理黑客系列的闪避属性和范围效果。 */
public final class HackerEvents {
    public static final String HACKER4_DODGE_END = "payday_hacker4_dodge_end";
    public static final UUID HACKER3_DODGE_MODIFIER = UUID.nameUUIDFromBytes(
            "payday_phone_upgrade_hacker3_dodge".getBytes(StandardCharsets.UTF_8));
    public static final UUID HACKER4_DODGE_MODIFIER = UUID.nameUUIDFromBytes(
            "payday_phone_upgrade_hacker4_dodge".getBytes(StandardCharsets.UTF_8));

    private static final String HACKER5_DODGE_PREFIX = "payday_phone_upgrade_hacker5_dodge";

    private HackerEvents() {
    }

    /** 每个玩家 tick 刷新黑客5的范围闪避属性。 */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        refreshHacker5Dodge(event.player);
    }

    /** 为玩家添加一个可重复刷新的临时闪避属性修饰符。 */
    public static void addDodgeChanceModifier(Player player, UUID id,
                                               String name, double amount) {
        AttributeInstance dodge = player.getAttribute(RegistryAttributes.DODGE_CHANCE.get());
        if (dodge == null) {
            return;
        }
        dodge.removeModifier(id);
        dodge.addTransientModifier(new AttributeModifier(
                id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    /**
     * 刷新目标玩家受到黑客5提供的范围闪避属性。
     *
     * <p>刷新前会清理本类旧修饰符，因此范围变化、啸叫结束或升级拆除后，
     * 不会残留黑客5的闪避效果。</p>
     *
     * @param target 要刷新范围闪避属性的玩家
     */
    public static void refreshHacker5Dodge(Player target) {
        if (target.level().isClientSide) {
            return;
        }
        AttributeInstance dodge = target.getAttribute(RegistryAttributes.DODGE_CHANCE.get());
        if (dodge == null) {
            return;
        }

        for (AttributeModifier modifier : new ArrayList<>(dodge.getModifiers())) {
            if (modifier.getName().startsWith(HACKER5_DODGE_PREFIX)) {
                dodge.removeModifier(modifier);
            }
        }

        for (Player provider : target.level().getEntitiesOfClass(Player.class,
                target.getBoundingBox().inflate(64.0D), player -> player.isAlive()
                        && !player.isSpectator()
                        && player.hasEffect(RegistryEffects.HOWL.get())
                        && PhoneItem.hasInstalledUpgrade(player, "special_hacker_5")
                        && player.distanceToSqr(target) <= 4096.0D)) {
            UUID id = UUID.nameUUIDFromBytes((HACKER5_DODGE_PREFIX + "|"
                    + provider.getUUID()).getBytes(StandardCharsets.UTF_8));
            addDodgeChanceModifier(target, id, HACKER5_DODGE_PREFIX, 1500.0D);
        }
    }
}
