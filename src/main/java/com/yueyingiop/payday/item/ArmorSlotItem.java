package com.yueyingiop.payday.item;

import com.yueyingiop.payday.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/** 护甲饰品中的升级插槽，可以提供一种或多种护甲类型。 */
@SuppressWarnings("unused")
public class ArmorSlotItem extends Item {
    public enum Kind { IMPACT, ADAPTIVE, RESISTANCE }

    private static final String CURRENT = "payday_current_armor";
    private static final String PERMANENT = "payday_permanent_damage";
    private static final String BROKEN_AT = "payday_broken_at";
    public static final String UPGRADE_INDEX = "payday_upgrade_index";

    private final String id;
    private final List<ArmorDefinition> armorDefinitions;
    private final Double recoverySecondsOverride;
    private final List<ProvidedAttribute> providedAttributes;
    private final List<ProvidedEffect> providedEffects;

    /** 创建一个使用内置配置的单类型护甲插槽。 */
    public ArmorSlotItem(String id) {
        this(id, List.of(new ArmorDefinition(inferKind(id), null)), null,
                List.of(), List.of(), new Item.Properties());
    }

    /** 创建一个可由 KubeJS 或 Java 指定多种护甲的插槽。 */
    public ArmorSlotItem(String id, List<ArmorDefinition> armorDefinitions,
                         Double recoverySecondsOverride,
                         List<ProvidedAttribute> providedAttributes,
                         List<ProvidedEffect> providedEffects, Item.Properties properties) {
        super(properties.stacksTo(1));
        if (armorDefinitions.isEmpty()) {
            throw new IllegalArgumentException("armorDefinitions must not be empty");
        }
        if (armorDefinitions.stream().anyMatch(definition -> definition.maxArmor() != null
                && definition.maxArmor() < 0.0D)) {
            throw new IllegalArgumentException("max armor must be non-negative");
        }
        this.id = id;
        this.armorDefinitions = List.copyOf(armorDefinitions);
        this.recoverySecondsOverride = recoverySecondsOverride;
        this.providedAttributes = List.copyOf(providedAttributes);
        this.providedEffects = List.copyOf(providedEffects);
    }

    /** 兼容旧 Java 调用方式，并将其转换为单类型定义。 */
    public ArmorSlotItem(String id, Kind kindOverride, Double maxArmorOverride,
                         Double recoverySecondsOverride, List<ProvidedAttribute> providedAttributes,
                         List<ProvidedEffect> providedEffects, Item.Properties properties) {
        this(id, List.of(new ArmorDefinition(kindOverride == null ? inferKind(id) : kindOverride,
                        maxArmorOverride)), recoverySecondsOverride, providedAttributes,
                providedEffects, properties);
    }

    /** 获取该插槽提供的所有护甲定义，顺序就是 KubeJS/Java 添加顺序。 */
    public List<ArmorDefinition> armorDefinitions() {
        return armorDefinitions;
    }

    /** 获取该插槽提供给护甲饰品的属性修饰符定义。 */
    public List<ProvidedAttribute> providedAttributes() {
        return providedAttributes;
    }

    /** 获取该插槽提供给护甲饰品的永久药水效果定义。 */
    public List<ProvidedEffect> providedEffects() {
        return providedEffects;
    }

    /** 获取指定定义的护甲类型。 */
    public Kind kind(int definitionIndex) {
        return armorDefinitions.get(definitionIndex).kind();
    }

    /** 获取指定定义的最大护甲值。 */
    public double maxArmor(int definitionIndex) {
        ArmorDefinition definition = armorDefinitions.get(definitionIndex);
        if (definition.maxArmor() != null) {
            return definition.maxArmor();
        }
        return configuredMax(definition.kind());
    }

    /** 获取指定定义的恢复间隔，单位为秒。 */
    public double recoverySeconds(int definitionIndex) {
        if (recoverySecondsOverride != null) {
            return recoverySecondsOverride;
        }
        return configuredRecovery(kind(definitionIndex));
    }

    /** 兼容旧调用方式，返回第一个护甲定义的类型。 */
    public Kind kind() {
        return kind(0);
    }

    /** 兼容旧调用方式，返回第一个护甲定义的最大值。 */
    public double maxArmor() {
        return maxArmor(0);
    }

    /** 兼容旧调用方式，返回第一个护甲定义的恢复间隔。 */
    public double recoverySeconds() {
        return recoverySeconds(0);
    }

    /** 获取指定护甲定义当前剩余值。 */
    public double current(ItemStack stack, int definitionIndex) {
        String key = stateKey(CURRENT, definitionIndex);
        if (!stack.hasTag() || !stack.getTag().contains(key)) {
            // 兼容旧版本单类型插槽保存的 payday_current_armor。
            if (definitionIndex == 0 && armorDefinitions.size() == 1
                    && stack.hasTag() && stack.getTag().contains(CURRENT)) {
                return clamp(stack.getTag().getDouble(CURRENT), effectiveMax(stack, definitionIndex));
            }
            return effectiveMax(stack, definitionIndex);
        }
        return clamp(stack.getTag().getDouble(key), effectiveMax(stack, definitionIndex));
    }

    /** 设置指定护甲定义当前剩余值。 */
    public void setCurrent(ItemStack stack, int definitionIndex, double value) {
        stack.getOrCreateTag().putDouble(stateKey(CURRENT, definitionIndex),
                clamp(value, effectiveMax(stack, definitionIndex)));
    }

    /** 获取指定护甲定义的永久损伤。 */
    public double permanent(ItemStack stack, int definitionIndex) {
        String key = stateKey(PERMANENT, definitionIndex);
        if (!stack.hasTag() || !stack.getTag().contains(key)) {
            if (definitionIndex == 0 && armorDefinitions.size() == 1
                    && stack.hasTag() && stack.getTag().contains(PERMANENT)) {
                return clamp(stack.getTag().getDouble(PERMANENT), maxArmor(definitionIndex));
            }
            return 0.0D;
        }
        return clamp(stack.getTag().getDouble(key), maxArmor(definitionIndex));
    }

    /** 为指定护甲定义增加永久损伤。 */
    public void addPermanent(ItemStack stack, int definitionIndex, double value) {
        double damage = clamp(permanent(stack, definitionIndex) + Math.max(0.0D, value),
                maxArmor(definitionIndex));
        stack.getOrCreateTag().putDouble(stateKey(PERMANENT, definitionIndex), damage);
        setCurrent(stack, definitionIndex, current(stack, definitionIndex));
    }

    /** 清除指定护甲定义的永久损伤。 */
    public void clearPermanent(ItemStack stack, int definitionIndex) {
        if (stack.hasTag()) {
            stack.getTag().remove(stateKey(PERMANENT, definitionIndex));
            if (definitionIndex == 0 && armorDefinitions.size() == 1) {
                stack.getTag().remove(PERMANENT);
            }
        }
    }

    /** 获取指定护甲定义扣除永久损伤后的最大值。 */
    public double effectiveMax(ItemStack stack, int definitionIndex) {
        return Math.max(0.0D, maxArmor(definitionIndex) - permanent(stack, definitionIndex));
    }

    /** 获取指定护甲定义的损坏顺序编号。 */
    public long brokenAt(ItemStack stack, int definitionIndex) {
        String key = stateKey(BROKEN_AT, definitionIndex);
        return stack.hasTag() && stack.getTag().contains(key)
                ? stack.getTag().getLong(key) : Long.MIN_VALUE;
    }

    /** 标记指定护甲定义的损坏顺序。 */
    public void markBroken(ItemStack stack, int definitionIndex, long tick) {
        stack.getOrCreateTag().putLong(stateKey(BROKEN_AT, definitionIndex), tick);
    }

    /** 清除指定护甲定义的损坏标记。 */
    public void clearBroken(ItemStack stack, int definitionIndex) {
        if (stack.hasTag()) {
            stack.getTag().remove(stateKey(BROKEN_AT, definitionIndex));
        }
    }

    /** 判断指定护甲定义是否损坏。冲击护甲永远不会进入损坏状态。 */
    public boolean isBroken(ItemStack stack, int definitionIndex) {
        return kind(definitionIndex) != Kind.IMPACT && current(stack, definitionIndex) <= 0.0D;
    }

    /** 判断指定护甲定义是否可以被护甲板修复。 */
    public boolean canRepair(ItemStack stack, int definitionIndex) {
        return isBroken(stack, definitionIndex)
                || (kind(definitionIndex) == Kind.RESISTANCE
                && permanent(stack, definitionIndex) > 0.0D)
                || (kind(definitionIndex) == Kind.IMPACT
                && current(stack, definitionIndex) < effectiveMax(stack, definitionIndex));
    }

    // 旧 API 保留给外部 Java/KJS 兼容调用。
    /**
     * 获取当前数值。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public double current(ItemStack stack) { return current(stack, 0); }
    /**
     * 设置指定护甲定义的当前耐久值。
     * @param stack 物品堆。
     * @param value 数值。
     */
    public void setCurrent(ItemStack stack, double value) { setCurrent(stack, 0, value); }
    /**
     * 获取永久数值。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public double permanent(ItemStack stack) { return permanent(stack, 0); }
    /**
     * 增加指定护甲定义的永久损伤。
     * @param stack 物品堆。
     * @param value 数值。
     */
    public void addPermanent(ItemStack stack, double value) { addPermanent(stack, 0, value); }
    /**
     * 清除指定护甲定义的永久损伤。
     * @param stack 物品堆。
     */
    public void clearPermanent(ItemStack stack) { clearPermanent(stack, 0); }
    /**
     * 获取扣除永久损伤后的有效护甲上限。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public double effectiveMax(ItemStack stack) { return effectiveMax(stack, 0); }
    /**
     * 获取护甲损坏顺序编号。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public long brokenAt(ItemStack stack) { return brokenAt(stack, 0); }
    /**
     * 记录护甲损坏顺序。
     * @param stack 物品堆。
     * @param tick 游戏刻。
     */
    public void markBroken(ItemStack stack, long tick) { markBroken(stack, 0, tick); }
    /**
     * 清除护甲损坏记录。
     * @param stack 物品堆。
     */
    public void clearBroken(ItemStack stack) { clearBroken(stack, 0); }
    /**
     * 判断指定护甲定义是否已损坏。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public boolean isBroken(ItemStack stack) { return isBroken(stack, 0); }
    /**
     * 判断指定护甲定义是否可以修复。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public boolean canRepair(ItemStack stack) { return canRepair(stack, 0); }

    /**
     * 获取升级物品记录的护甲插槽索引。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    public static int upgradeIndex(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(UPGRADE_INDEX)
                ? stack.getTag().getInt(UPGRADE_INDEX) : -1;
    }

    /**
     * 在升级物品 NBT 中记录护甲插槽索引。
     * @param stack 物品堆。
     * @param index 索引。
     */
    public static void setUpgradeIndex(ItemStack stack, int index) {
        stack.getOrCreateTag().putInt(UPGRADE_INDEX, index);
    }

    /**
     * 清除升级物品记录的插槽索引。
     * @param stack 物品堆。
     */
    public static void clearUpgradeIndex(ItemStack stack) {
        if (stack.hasTag()) stack.getTag().remove(UPGRADE_INDEX);
    }

    /**
     * 向物品提示添加护甲信息。
     * @param stack 物品堆。
     * @param level 世界。
     * @param tooltip 对应参数。
     * @param flag 对应参数。
     */
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        for (ArmorDefinition definition : armorDefinitions) {
            tooltip.add(Component.translatable("tooltip.payday.armor_entry",
                    Component.translatable(typeTranslationKey(definition.kind())),
                    formatArmorValue(definition.maxArmor() == null
                            ? configuredMax(definition.kind()) : definition.maxArmor()))
                    .withStyle(ChatFormatting.AQUA));
        }

        for (ProvidedEffect effect : providedEffects) {
            tooltip.add(Component.translatable("tooltip.payday.armor_effect", effect.effect().getDisplayName(),
                    effect.amplifier() + 1).withStyle(ChatFormatting.GREEN));
        }

        for (ProvidedAttribute attribute : providedAttributes) {
            Component attributeName = Component.translatable(attribute.attribute().getDescriptionId());
            tooltip.add(Component.translatable("tooltip.payday.armor_attribute", attributeName,
                    formatArmorValue(attribute.value()), attribute.operation().name())
                    .withStyle(ChatFormatting.BLUE));
        }

        int index = upgradeIndex(stack);
        if (index >= 0) {
            tooltip.add(Component.translatable("tooltip.payday.armor_slot_index", index + 1)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * 根据物品标识推断护甲类型。
     * @param id 标识。
     * @return 对应的执行结果。
     */
    private static Kind inferKind(String id) {
        return switch (id) {
            case "adaptive_slot" -> Kind.ADAPTIVE;
            case "resistance_slot" -> Kind.RESISTANCE;
            default -> Kind.IMPACT;
        };
    }

    /**
     * 读取指定类型的配置护甲上限。
     * @param kind 护甲类型。
     * @return 对应的执行结果。
     */
    private static double configuredMax(Kind kind) {
        return switch (kind) {
            case ADAPTIVE -> Config.ADAPTIVE_ARMOR.get();
            case RESISTANCE -> Config.RESISTANCE_ARMOR.get();
            case IMPACT -> Config.IMPACT_ARMOR.get();
        };
    }

    /**
     * 读取指定类型的配置恢复间隔。
     * @param kind 护甲类型。
     * @return 对应的执行结果。
     */
    private static double configuredRecovery(Kind kind) {
        return switch (kind) {
            case ADAPTIVE -> Config.ADAPTIVE_RECOVERY.get();
            case RESISTANCE -> Config.RESISTANCE_RECOVERY.get();
            case IMPACT -> Config.IMPACT_RECOVERY.get();
        };
    }

    /**
     * 限制数值范围。
     * @param value 数值。
     * @param maximum 最大值。
     * @return 对应的执行结果。
     */
    private static double clamp(double value, double maximum) {
        return Math.min(Math.max(0.0D, value), Math.max(0.0D, maximum));
    }

    /**
     * 生成指定护甲定义对应的 NBT 键。
     * @param base 对应参数。
     * @param definitionIndex 护甲定义索引。
     * @return 对应的执行结果。
     */
    private static String stateKey(String base, int definitionIndex) {
        return base + "_" + definitionIndex;
    }

    /**
     * 获取护甲类型对应的翻译键。
     * @param kind 护甲类型。
     * @return 对应的执行结果。
     */
    private static String typeTranslationKey(Kind kind) {
        return switch (kind) {
            case IMPACT -> "tooltip.payday.armor_type.impact";
            case ADAPTIVE -> "tooltip.payday.armor_type.adaptive";
            case RESISTANCE -> "tooltip.payday.armor_type.resistance";
        };
    }

    /**
     * 格式化护甲数值以供提示显示。
     * @param value 数值。
     * @return 对应的执行结果。
     */
    private static String formatArmorValue(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** 一条护甲定义，null 最大值表示使用对应配置项。 */
    public record ArmorDefinition(Kind kind, Double maxArmor) {
        public ArmorDefinition {
            if (kind == null) {
                throw new IllegalArgumentException("kind must not be null");
            }
        }
    }

    /** 护甲插槽提供的属性修饰符定义。 */
    public record ProvidedAttribute(String attributeId, Attribute attribute,
                                    AttributeModifier.Operation operation, double value) {
    }

    /** 护甲插槽提供的永久药水效果定义。 */
    public record ProvidedEffect(MobEffect effect, int amplifier, boolean ambient,
                                 boolean showParticles, boolean showIcon) {
    }
}
