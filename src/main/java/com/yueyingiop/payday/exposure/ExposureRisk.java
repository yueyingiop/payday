package com.yueyingiop.payday.exposure;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.ExposureRiskModifier;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.init.RegistryAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Calculates the server-authoritative exposure risk for a player. */
public final class ExposureRisk {
    private static final String PERMANENT_MODIFIER_KEY = "payday_exposure_permanent_modifier";

    private ExposureRisk() {
    }

    /**
     * 获取对应内容。
     * @param player 玩家。
     * @return 对应的执行结果。
     */
    public static int get(Player player) {
        Contributions contributions = contributions(player);
        int min = Math.min(Config.EXPOSURE_RISK_MIN.get(), Config.EXPOSURE_RISK_MAX.get());
        int max = Math.max(Config.EXPOSURE_RISK_MIN.get(), Config.EXPOSURE_RISK_MAX.get());
        // 手机升级通过 AttributeDefinition 提供暴露风险修正，未来其他升级可直接复用同一属性。
        double attributeModifier = player.getAttributeValue(RegistryAttributes.EXPOSURE_RISK.get());
        return Mth.clamp((int) Math.ceil(Config.EXPOSURE_RISK_DEFAULT.get()
                + Math.ceil(contributions.stackableCount() / 32.0D)
                + Math.ceil(contributions.unstackableNbtCharacters() / 16.0D)
                + contributions.specialRisk()
                + contributions.temporaryModifier()
                + getPermanentModifier(player) + attributeModifier), min, max);
    }

    /** The requested 9^((risk - 30) / 30) target-distance multiplier. */
    public static double targetDistanceMultiplier(Player player) {
        return Math.pow(9.0D, (get(player) - 30.0D) / 30.0D);
    }

    /**
     * 获取永久风险修正值。
     * @param player 玩家。
     * @return 对应的执行结果。
     */
    public static double getPermanentModifier(Player player) {
        return player.getPersistentData().getDouble(PERMANENT_MODIFIER_KEY);
    }

    /**
     * 设置永久风险修正值。
     * @param player 玩家。
     * @param value 数值。
     */
    public static void setPermanentModifier(Player player, double value) {
        player.getPersistentData().putDouble(PERMANENT_MODIFIER_KEY, value);
    }

    /**
     * 增加永久风险修正值。
     * @param player 玩家。
     * @param value 数值。
     */
    public static void addPermanentModifier(Player player, double value) {
        setPermanentModifier(player, getPermanentModifier(player) + value);
    }

    /**
     * 汇总玩家物品栏、装备栏及饰品栏的风险贡献。
     * @param player 玩家。
     * @return 对应的执行结果。
     */
    private static Contributions contributions(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        player.getInventory().items.forEach(stacks::add);
        player.getInventory().armor.forEach(stacks::add);
        player.getInventory().offhand.forEach(stacks::add);
        stacks.addAll(CurioHelper.allStacks(player));

        List<SpecialRisk> specialRisks = specialRisks();
        int stackableCount = 0;
        int unstackableNbtCharacters = 0;
        double specialRisk = 0.0D;
        double temporaryModifier = 0.0D;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag tag = stack.getTag();
            temporaryModifier += tag != null && tag.contains(ExposureRiskModifier.TEMPORARY_MODIFIER_KEY)
                    ? tag.getDouble(ExposureRiskModifier.TEMPORARY_MODIFIER_KEY) : 0.0D;
            SpecialRisk special = findSpecialRisk(stack, specialRisks);
            if (special != null) {
                specialRisk += special.value() * stack.getCount();
            } else if (stack.getMaxStackSize() > 1) {
                stackableCount += stack.getCount();
            } else {
                unstackableNbtCharacters += tag == null ? 0 : tag.toString().length();
            }
        }
        return new Contributions(stackableCount, unstackableNbtCharacters, specialRisk, temporaryModifier);
    }

    /**
     * 查找物品匹配的特殊风险规则。
     * @param stack 物品堆。
     * @param risks 对应参数。
     * @return 对应的执行结果。
     */
    private static SpecialRisk findSpecialRisk(ItemStack stack, List<SpecialRisk> risks) {
        for (SpecialRisk risk : risks) {
            if (risk.itemId() != null && ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(risk.itemId())) {
                return risk;
            }
            if (risk.tag() != null && stack.is(risk.tag())) {
                return risk;
            }
        }
        return null;
    }

    /**
     * 解析配置中的物品及标签风险规则。
     * @return 对应的执行结果。
     */
    private static List<SpecialRisk> specialRisks() {
        List<SpecialRisk> result = new ArrayList<>();
        for (String entry : Config.EXPOSURE_SPECIAL_ITEM_RISKS.get()) {
            int separator = entry.lastIndexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                PayDay.LOGGER.warn("Ignoring invalid exposure special risk '{}'", entry);
                continue;
            }
            try {
                String target = entry.substring(0, separator).trim();
                double value = Double.parseDouble(entry.substring(separator + 1).trim());
                ResourceLocation id = ResourceLocation.tryParse(target.startsWith("#") ? target.substring(1) : target);
                if (id == null) {
                    throw new IllegalArgumentException("invalid resource location");
                }
                result.add(target.startsWith("#")
                        ? new SpecialRisk(null, TagKey.create(net.minecraft.core.registries.Registries.ITEM, id), value)
                        : new SpecialRisk(id, null, value));
            } catch (RuntimeException exception) {
                PayDay.LOGGER.warn("Ignoring invalid exposure special risk '{}'", entry, exception);
            }
        }
        return result;
    }

    private record Contributions(int stackableCount, int unstackableNbtCharacters,
                                 double specialRisk, double temporaryModifier) { }
    private record SpecialRisk(ResourceLocation itemId, TagKey<Item> tag, double value) { }
}
