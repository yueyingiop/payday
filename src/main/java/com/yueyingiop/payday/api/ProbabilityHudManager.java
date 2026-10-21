package com.yueyingiop.payday.api;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.helper.PhoneUpgradeHelper;
import com.yueyingiop.payday.api.phone.AttributeDefinition;
import com.yueyingiop.payday.item.PhoneItem;
import com.yueyingiop.payday.item.PhoneUpgradeItem;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Builds the automatic probability HUDs provided by installed phone upgrades. */
public final class ProbabilityHudManager {
    private static final String ID_PREFIX = "payday_auto_probability_";
    private static final ResourceLocation RANGED_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath("payday", "ranged_damage");
    private static final Map<UUID, String> FINGERPRINTS = new ConcurrentHashMap<>();

    /** Prevents construction of the server-side HUD manager. */
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

    /** Collects one HUD per whitelisted attribute and separate HUDs for non-attribute effects. */
    private static Map<String, CooldownHud> collect(ServerPlayer player) {
        Map<String, CooldownHud> result = new LinkedHashMap<>();
        Map<ResourceLocation, AttributeIcon> icons = attributeIcons();
        Map<ResourceLocation, Attribute> attributes = new LinkedHashMap<>();
        int upgradeIndex = 0;

        for (ItemStack phoneStack : com.yueyingiop.payday.api.helper.CurioHelper.stacks(
                player, com.yueyingiop.payday.api.helper.CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgradeStack : phone.getUpgradeStacks(phoneStack)) {
                if (upgradeStack.getItem() instanceof PhoneUpgradeItem upgrade) {
                    collectAttributes(attributes, icons, upgrade);
                    addUpgradeHuds(result, upgrade, upgradeIndex++);
                }
            }
        }

        addAttributeHuds(result, player, attributes, icons);
        return result;
    }

    /** Collects all whitelisted attributes, including conditional movement from Forest. */
    private static void collectAttributes(Map<ResourceLocation, Attribute> result,
                                          Map<ResourceLocation, AttributeIcon> icons,
                                          PhoneUpgradeItem upgrade) {
        for (AttributeDefinition definition : upgrade.action().getAttributes()) {
            collectAttribute(result, icons, definition.attribute());
        }
        if ("special_forest".equals(upgrade.effectKey())) {
            collectAttribute(result, icons, Attributes.MOVEMENT_SPEED);
        }
    }

    /** Adds an attribute only when its registry ID has an icon whitelist entry. */
    private static void collectAttribute(Map<ResourceLocation, Attribute> result,
                                         Map<ResourceLocation, AttributeIcon> icons,
                                         Attribute attribute) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (id != null && icons.containsKey(id)) {
            result.putIfAbsent(id, attribute);
        }
    }

    /** Creates one HUD from the player's final value for each collected attribute. */
    private static void addAttributeHuds(Map<String, CooldownHud> result, ServerPlayer player,
                                         Map<ResourceLocation, Attribute> attributes,
                                         Map<ResourceLocation, AttributeIcon> icons) {
        for (Map.Entry<ResourceLocation, Attribute> entry : attributes.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getValue());
            AttributeIcon icon = icons.get(entry.getKey());
            if (instance == null || icon == null) {
                continue;
            }
            double value = entry.getKey().equals(RANGED_DAMAGE_ID)
                    ? rangedDamageValue(player, instance) : instance.getValue();
            CooldownHud hud = CooldownHud.builder()
                    .type(CooldownHud.Type.PROBABILITY)
                    .texture(icon.texture())
                    .texturePosition(icon.u(), icon.v())
                    .value(value)
                    .percentage(false)
                    .showName(Component.translatable(entry.getValue().getDescriptionId()))
                    .build();
            result.put("attribute_" + entry.getKey(), hud);
        }
    }

    /**
     * Returns the current ranged damage using the held TACZ gun as its base value.
     *
     * <p>When no TACZ gun is held, the base value is zero and any registered
     * attribute modifiers are still applied using the normal attribute rules.</p>
     *
     * @param player player whose main-hand item is inspected
     * @param attribute ranged-damage attribute instance
     * @return displayed ranged-damage value
     */
    private static double rangedDamageValue(ServerPlayer player,
                                             AttributeInstance attribute) {
        double baseValue = 0.0D;
        ItemStack heldStack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(heldStack);
        if (gun != null) {
            CommonGunIndex gunIndex = CommonAssetsManager.getInstance()
                    .getGunIndex(gun.getGunId(heldStack));
            if (gunIndex != null && gunIndex.getGunData() != null
                    && gunIndex.getGunData().getBulletData() != null) {
                baseValue = gunIndex.getGunData().getBulletData().getDamageAmount();
            }
        }
        return PhoneUpgradeHelper.applyAttributeModifiers(baseValue, attribute);
    }

    /** Adds non-attribute HUDs belonging to one installed phone upgrade. */
    private static void addUpgradeHuds(Map<String, CooldownHud> result,
                                       PhoneUpgradeItem upgrade, int upgradeIndex) {
        String baseId = upgrade.effectKey() + "_" + upgradeIndex;
        String texture = texture(upgradeStackItemId(upgrade));

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

    /** Adds non-attribute effects that do not exist as player attributes. */
    private static void addKnownSpecialHud(Map<String, CooldownHud> result,
                                           PhoneUpgradeItem upgrade, String baseId,
                                           String texture) {
        String key = upgrade.effectKey();
        switch (key) {
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

    /** Adds a normal probability HUD that still displays its value. */
    private static void add(Map<String, CooldownHud> result, String id, String texture,
                            double value, String nameKey) {
        result.put(id, hud(texture, value, Component.translatable(nameKey)));
    }

    /** Creates a normal probability HUD. */
    private static CooldownHud hud(String texture, double value, Component name) {
        return CooldownHud.builder().type(CooldownHud.Type.PROBABILITY)
                .texture(texture).value(value).showName(name).build();
    }

    /** Reads the configured attribute-to-icon whitelist. */
    private static Map<ResourceLocation, AttributeIcon> attributeIcons() {
        Map<ResourceLocation, AttributeIcon> result = new LinkedHashMap<>();
        for (String raw : Config.PROBABILITY_HUD_ATTRIBUTE_ICONS.get()) {
            if (raw == null) {
                continue;
            }
            int separator = raw.indexOf(".-");
            if (separator <= 0 || separator + 2 >= raw.length()) {
                PayDay.LOGGER.warn("Ignoring invalid probability HUD attribute icon entry: {}", raw);
                continue;
            }
            ResourceLocation attribute = ResourceLocation.tryParse(raw.substring(0, separator).trim());
            ResourceLocation icon = ResourceLocation.tryParse(raw.substring(separator + 2).trim());
            if (attribute == null || icon == null) {
                PayDay.LOGGER.warn("Ignoring invalid probability HUD attribute icon entry: {}", raw);
                continue;
            }
            ResourceLocation normalizedIcon = normalizeTexture(icon);
            int index = cdAttributeIndex(attribute);
            if (isCdAttributeTexture(normalizedIcon) && index < 0) {
                PayDay.LOGGER.warn("Ignoring cd_attribute entry without a known icon index: {}", raw);
                continue;
            }
            result.put(attribute, new AttributeIcon(normalizedIcon,
                    index < 0 ? 0 : index % 4 * 16,
                    index < 0 ? 0 : index / 4 * 16));
        }
        return result;
    }

    /** Returns the icon-table index for the six built-in attribute icons. */
    private static int cdAttributeIndex(ResourceLocation attribute) {
        return switch (attribute.toString()) {
            case "minecraft:generic.movement_speed" -> 0;
            case "minecraft:generic.attack_damage" -> 1;
            case "payday:ranged_damage" -> 2;
            case "payday:headshot_damage" -> 3;
            case "payday:dodge_chance" -> 4;
            case "tacz:tacz.bullet_resistance" -> 5;
            default -> -1;
        };
    }

    /** Checks whether an icon entry points at the built-in 64x64 icon table. */
    private static boolean isCdAttributeTexture(ResourceLocation texture) {
        return "payday".equals(texture.getNamespace())
                && "textures/gui/cd_attribute.png".equals(texture.getPath());
    }

    /** Normalizes a config icon path to the texture resource format. */
    private static ResourceLocation normalizeTexture(ResourceLocation texture) {
        String path = texture.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);
    }

    /** Returns the experience bonus represented by an installed upgrade. */
    private static double experienceBonus(PhoneUpgradeItem upgrade) {
        return "special_wardrobe".equals(upgrade.effectKey()) ? 35.0D : 25.0D;
    }

    /** Returns the registry ID of a phone upgrade item. */
    private static String upgradeStackItemId(PhoneUpgradeItem upgrade) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(upgrade);
        return id == null ? null : id.toString();
    }

    /** Converts a phone upgrade item ID to its generated item texture path. */
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

    /** Builds a stable value used to avoid resending unchanged automatic HUDs. */
    private static String fingerprint(Map<String, CooldownHud> huds) {
        StringBuilder result = new StringBuilder();
        huds.forEach((id, hud) -> result.append(id).append('|').append(hud.value())
                .append('|').append(hud.texture()).append('|').append(hud.name())
                .append('|').append(hud.showValue()).append('|').append(hud.percentage())
                .append('|').append(hud.textureU()).append('|').append(hud.textureV())
                .append(';'));
        return result.toString();
    }

    /** Describes one attribute icon and its optional texture-table coordinates. */
    private record AttributeIcon(ResourceLocation texture, int u, int v) {
    }
}
