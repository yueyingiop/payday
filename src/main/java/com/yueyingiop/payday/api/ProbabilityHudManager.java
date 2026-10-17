package com.yueyingiop.payday.api;

import com.yueyingiop.payday.api.phone.AttributeDefinition;
import com.yueyingiop.payday.item.PhoneItem;
import com.yueyingiop.payday.item.PhoneUpgradeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Builds the automatic probability HUDs provided by installed phone upgrades. */
public final class ProbabilityHudManager {
    private static final String ID_PREFIX = "payday_auto_probability_";
    private static final Map<UUID, String> FINGERPRINTS = new ConcurrentHashMap<>();

    private ProbabilityHudManager() {
    }

    /** Rebuilds automatic probability HUDs for the currently equipped phone upgrades. */
    public static void refresh(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Map<String, CooldownHud> desired = collect(player);
        String fingerprint = fingerprint(desired);
        if (fingerprint.equals(FINGERPRINTS.get(player.getUUID()))) {
            return;
        }

        CooldownHudManager.removeByPrefix(player, ID_PREFIX);
        for (Map.Entry<String, CooldownHud> entry : desired.entrySet()) {
            CooldownHudManager.showProbability(player, ID_PREFIX + entry.getKey(), entry.getValue());
        }
        FINGERPRINTS.put(player.getUUID(), fingerprint);
    }

    /** Removes automatic probability HUDs when a phone is removed. */
    public static void clear(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        CooldownHudManager.removeByPrefix(player, ID_PREFIX);
        FINGERPRINTS.remove(player.getUUID());
    }

    /** Clears the manager fingerprint when the player leaves the server. */
    public static void forget(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            FINGERPRINTS.remove(player.getUUID());
        }
    }

    private static Map<String, CooldownHud> collect(ServerPlayer player) {
        Map<String, CooldownHud> result = new LinkedHashMap<>();
        int upgradeIndex = 0;
        for (ItemStack phoneStack : com.yueyingiop.payday.api.helper.CurioHelper.stacks(
                player, com.yueyingiop.payday.api.helper.CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgradeStack : phone.getUpgradeStacks(phoneStack)) {
                if (upgradeStack.getItem() instanceof PhoneUpgradeItem upgrade) {
                    addUpgradeHuds(result, upgrade, upgradeIndex++);
                }
            }
        }
        return result;
    }

    private static void addUpgradeHuds(Map<String, CooldownHud> result,
                                       PhoneUpgradeItem upgrade, int upgradeIndex) {
        String baseId = upgrade.effectKey() + "_" + upgradeIndex;
        String texture = texture(upgradeStackItemId(upgrade));
        List<AttributeDefinition> attributes = upgrade.action().getAttributes();
        for (int i = 0; i < attributes.size(); i++) {
            AttributeDefinition definition = attributes.get(i);
            result.put(baseId + "_attribute_" + i,
                    hud(texture, attributeValue(definition),
                            Component.translatable(definition.attribute().getDescriptionId())));
        }

        if (upgrade.action().hasProxyEvent(LivingDropsEvent.class)) {
            result.put(baseId + "_kill_drop", hud(texture, 35.0D,
                    Component.translatable("cooldown_hud.payday.kill_drop")));
        }
        if (upgrade.action().hasProxyEvent(LivingExperienceDropEvent.class)) {
            result.put(baseId + "_experience_drop", hud(texture, experienceBonus(upgrade),
                    Component.translatable("cooldown_hud.payday.experience_drop")));
        }

        addKnownSpecialHud(result, upgrade, baseId, texture);
    }

    private static void addKnownSpecialHud(Map<String, CooldownHud> result,
                                           PhoneUpgradeItem upgrade, String baseId,
                                           String texture) {
        String key = upgrade.effectKey();
        switch (key) {
            case "special_helmet_dance" -> add(result, baseId + "_headshot", texture,
                    25.0D, "cooldown_hud.payday.headshot_damage");
            case "special_forest" -> add(result, baseId + "_movement", texture,
                    15.0D, "cooldown_hud.payday.movement_speed");
            case "special_last_stand" -> {
                add(result, baseId + "_initial_damage", texture, 5.0D,
                        "cooldown_hud.payday.initial_damage");
                add(result, baseId + "_healing", texture, 20.0D,
                        "cooldown_hud.payday.healing");
            }
            case "special_armorer_1", "special_armorer_2", "special_armorer_3",
                    "special_antisocial_2", "special_antisocial_4" -> add(result,
                    baseId + "_shield", texture, 10.0D, "cooldown_hud.payday.shield");
            case "special_leader_4" -> add(result, baseId + "_shield", texture,
                    15.0D, "cooldown_hud.payday.shield");
            case "special_armorer_4" -> add(result, baseId + "_shield_cooldown", texture,
                    10.0D, "cooldown_hud.payday.shield_cooldown");
            case "special_armorer_5" -> {
                add(result, baseId + "_shield", texture, 5.0D,
                        "cooldown_hud.payday.shield");
                add(result, baseId + "_shield_cooldown", texture, 25.0D,
                        "cooldown_hud.payday.shield_cooldown");
            }
            case "special_assassin_1", "special_assassin_2" -> add(result,
                    baseId + "_shield_cooldown", texture, 5.0D,
                    "cooldown_hud.payday.shield_cooldown");
            case "special_assassin_3", "special_assassin_4", "special_assassin_5",
                    "special_thief_5" -> add(result, baseId + "_shield_cooldown", texture,
                    key.equals("special_thief_5") ? 25.0D : 10.0D,
                    "cooldown_hud.payday.shield_cooldown");
            default -> {
            }
        }
    }

    private static void add(Map<String, CooldownHud> result, String id, String texture,
                             double value, String nameKey) {
        result.put(id, hud(texture, value, Component.translatable(nameKey)));
    }

    private static CooldownHud hud(String texture, double value, Component name) {
        return CooldownHud.builder().type(CooldownHud.Type.PROBABILITY)
                .texture(texture).value(value).showName(name).build();
    }

    private static double attributeValue(AttributeDefinition definition) {
        double amount = definition.amount();
        if (definition.operation() != AttributeModifier.Operation.ADDITION) {
            return amount * 100.0D;
        }
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(definition.attribute());
        if (id != null && id.toString().equals("payday:dodge_chance")) {
            return amount / 100.0D;
        }
        return amount;
    }

    private static double experienceBonus(PhoneUpgradeItem upgrade) {
        return "special_wardrobe".equals(upgrade.effectKey()) ? 35.0D : 25.0D;
    }

    private static String upgradeStackItemId(PhoneUpgradeItem upgrade) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(upgrade);
        return id == null ? null : id.toString();
    }

    private static String texture(String itemId) {
        if (itemId == null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        if (path.startsWith("phone_upgrade/")) {
            path = "item/phone_upgrade/generated/" + path.substring("phone_upgrade/".length());
        } else {
            path = "item/" + path;
        }
        return id.getNamespace() + ":" + path;
    }

    private static String fingerprint(Map<String, CooldownHud> huds) {
        StringBuilder result = new StringBuilder();
        huds.forEach((id, hud) -> result.append(id).append('|').append(hud.value())
                .append('|').append(hud.texture()).append('|').append(hud.name()).append(';'));
        return result.toString();
    }
}
