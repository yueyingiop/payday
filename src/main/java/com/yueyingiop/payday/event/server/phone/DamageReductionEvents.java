package com.yueyingiop.payday.event.server.phone;

import com.tacz.guns.init.ModAttributes;
import com.yueyingiop.payday.api.ProbabilityHudManager;
import com.yueyingiop.payday.api.helper.PhoneUpgradeHelper;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

/** Refreshes conditional phone damage-reduction modifiers on the TACZ attribute. */
public final class DamageReductionEvents {
    private static final String MODIFIER_PREFIX = "payday_phone_upgrade_damage_reduction_";

    private DamageReductionEvents() {
    }

    /** Refreshes conditional resistance and its automatic probability HUD each player tick. */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        refresh(event.player);
        ProbabilityHudManager.refresh(event.player);
    }

    /** Rebuilds all PayDay damage-reduction modifiers for one player. */
    public static void refresh(Player target) {
        AttributeInstance resistance = target.getAttribute(ModAttributes.BULLET_RESISTANCE.get());
        if (resistance == null) {
            return;
        }
        removePayDayModifiers(resistance);
        addLeaderReduction(target, resistance);
        addConditionalReduction(target, resistance, "special_infiltrator_1",
                PhoneUpgradeHelper.countUnfriendly(target, 8.0D) >= 3, 1200.0D);
        addConditionalReduction(target, resistance, "special_infiltrator_2",
                PhoneUpgradeHelper.countUnfriendly(target, 16.0D) >= 1, 800.0D);
        addConditionalReduction(target, resistance, "special_infiltrator_3",
                PhoneUpgradeHelper.countUnfriendly(target, 16.0D) >= 1, 800.0D);
        addConditionalReduction(target, resistance, "special_infiltrator_4",
                PhoneUpgradeHelper.countUnfriendly(target, 16.0D) >= 1, 800.0D);
        addConditionalReduction(target, resistance, "special_antisocial_3",
                PhoneUpgradeHelper.countUnfriendly(target, 16.0D) >= 1, 800.0D);
    }

    /** Adds the active leader's range, health and series-level reductions. */
    private static void addLeaderReduction(Player target, AttributeInstance resistance) {
        Player leader = LeaderEvents.bestLeaderProvider(target);
        if (leader == null || !PhoneItem.hasInstalledUpgrade(leader, "special_leader_1")) {
            return;
        }

        int level = PhoneItem.installedSeriesLevel(leader, "leader");
        double reduction = leader == target && target.getHealth() <= target.getMaxHealth() * 0.5D
                ? 1600.0D : 800.0D;
        if (leader == target && level >= 2) {
            reduction += 500.0D;
        }
        if (leader == target && level >= 5) {
            reduction += 1000.0D;
        }
        addModifier(resistance, leader.getUUID() + "|leader", reduction);
    }

    /** Adds one conditional reduction when its upgrade and condition are both active. */
    private static void addConditionalReduction(Player target, AttributeInstance resistance,
                                                String upgradeId, boolean active,
                                                double basisPoints) {
        if (active && PhoneItem.hasInstalledUpgrade(target, upgradeId)) {
            addModifier(resistance, target.getUUID() + "|" + upgradeId, basisPoints);
        }
    }

    /** Adds a TACZ-compatible decimal modifier from a PayDay basis-point value. */
    private static void addModifier(AttributeInstance resistance, String key,
                                    double basisPoints) {
        UUID id = UUID.nameUUIDFromBytes((MODIFIER_PREFIX + key)
                .getBytes(StandardCharsets.UTF_8));
        resistance.addTransientModifier(new AttributeModifier(id, MODIFIER_PREFIX + key,
                basisPoints / 10000.0D, AttributeModifier.Operation.ADDITION));
    }

    /** Removes all conditional PayDay resistance modifiers before rebuilding them. */
    private static void removePayDayModifiers(AttributeInstance resistance) {
        for (AttributeModifier modifier : new ArrayList<>(resistance.getModifiers())) {
            if (modifier.getName().startsWith(MODIFIER_PREFIX)) {
                resistance.removeModifier(modifier.getId());
            }
        }
    }
}
