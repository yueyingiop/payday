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
    private static final ResourceLocation HEADSHOT_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath("payday", "headshot_damage");
    private static final ResourceLocation DODGE_CHANCE_ID =
            ResourceLocation.fromNamespaceAndPath("payday", "dodge_chance");
    private static final ResourceLocation BULLET_RESISTANCE_ID =
            ResourceLocation.fromNamespaceAndPath("tacz", "tacz.bullet_resistance");
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

    /** Collects one HUD per whitelisted attribute. */
    private static Map<String, CooldownHud> collect(ServerPlayer player) {
        Map<String, CooldownHud> result = new LinkedHashMap<>();
        Map<ResourceLocation, AttributeIcon> icons = attributeIcons();
        Map<ResourceLocation, Attribute> attributes = new LinkedHashMap<>();

        for (ItemStack phoneStack : com.yueyingiop.payday.api.helper.CurioHelper.stacks(
                player, com.yueyingiop.payday.api.helper.CurioHelper.PHONE_SLOT)) {
            if (!(phoneStack.getItem() instanceof PhoneItem phone)) {
                continue;
            }
            for (ItemStack upgradeStack : phone.getUpgradeStacks(phoneStack)) {
                if (upgradeStack.getItem() instanceof PhoneUpgradeItem upgrade) {
                    collectAttributes(attributes, icons, upgrade);
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
                    ? rangedDamageValue(player, instance)
                    : attributeHudValue(entry.getKey(), instance);
            if (entry.getKey().equals(RANGED_DAMAGE_ID) && value == 0.0D) {
                continue;
            }
            boolean showPercentage = isBasisPointAttribute(entry.getKey());
            CooldownHud hud = CooldownHud.builder()
                    .type(CooldownHud.Type.PROBABILITY)
                    .texture(icon.texture())
                    .texturePosition(icon.u(), icon.v())
                    .value(value)
                    .percentage(false)
                    .showPercentage(showPercentage)
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

    /** Converts TACZ's decimal resistance value to the basis-point value shown by the HUD. */
    private static double attributeHudValue(ResourceLocation id, AttributeInstance instance) {
        double value = instance.getValue();
        return id.equals(BULLET_RESISTANCE_ID) ? value * 10000.0D : value;
    }

    /** Returns whether an automatic attribute HUD should display a percent sign. */
    private static boolean isBasisPointAttribute(ResourceLocation id) {
        return id.equals(HEADSHOT_DAMAGE_ID) || id.equals(DODGE_CHANCE_ID)
                || id.equals(BULLET_RESISTANCE_ID);
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
            boolean tableTexture = isCdAttributeTexture(normalizedIcon);
            if (tableTexture && index < 0) {
                PayDay.LOGGER.warn("Ignoring cd_attribute entry without a known icon index: {}", raw);
                continue;
            }
            int u = tableTexture && index >= 0 ? index % 4 * 16 : 0;
            int v = tableTexture && index >= 0 ? index / 4 * 16 : 0;
            result.put(attribute, new AttributeIcon(normalizedIcon, u, v));
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

    /** Builds a stable value used to avoid resending unchanged automatic HUDs. */
    private static String fingerprint(Map<String, CooldownHud> huds) {
        StringBuilder result = new StringBuilder();
        huds.forEach((id, hud) -> result.append(id).append('|').append(hud.value())
                .append('|').append(hud.texture()).append('|').append(hud.name())
                .append('|').append(hud.showValue()).append('|').append(hud.percentage())
                .append('|').append(hud.showPercentage())
                .append('|').append(hud.textureU()).append('|').append(hud.textureV())
                .append(';'));
        return result.toString();
    }

    /** Describes one attribute icon and its optional texture-table coordinates. */
    private record AttributeIcon(ResourceLocation texture, int u, int v) {
    }
}
