package com.yueyingiop.payday.compat.kubejs;

import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.api.helper.AttributeModifierHelper;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** KubeJS 用于创建护甲升级插槽的物品 Builder。 */
public class ArmorSlotBuilder extends ItemBuilder {
    private final List<ArmorSlotItem.ArmorDefinition> armorDefinitions = new ArrayList<>();
    private Double recoverySeconds;
    private final List<ArmorSlotItem.ProvidedAttribute> providedAttributes = new ArrayList<>();
    private final List<ArmorSlotItem.ProvidedEffect> providedEffects = new ArrayList<>();

    public ArmorSlotBuilder(ResourceLocation id) {
        super(id);
    }

    /** 添加一种护甲类型和最大护甲值，可重复调用以创建多类型插槽。 */
    public ArmorSlotBuilder addArmor(String type, double value) {
        if (value < 0.0D) {
            throw new IllegalArgumentException("armor value must be non-negative");
        }
        armorDefinitions.add(new ArmorSlotItem.ArmorDefinition(parseKind(type), value));
        return this;
    }

    /** 设置该插槽的恢复间隔，单位为秒。 */
    public ArmorSlotBuilder recoverySeconds(double recoverySeconds) {
        if (recoverySeconds < 0.0D) {
            throw new IllegalArgumentException("recoverySeconds must be non-negative");
        }
        this.recoverySeconds = recoverySeconds;
        return this;
    }

    /** 添加一个由该护甲插槽提供的属性修饰符。 */
    public ArmorSlotBuilder addAttribute(String attributeId, String type, double value) {
        var attribute = AttributeModifierHelper.resolveAttribute(attributeId);
        AttributeModifier.Operation operation = AttributeModifierHelper.parseOperation(type);
        providedAttributes.add(new ArmorSlotItem.ProvidedAttribute(attributeId, attribute, operation, value));
        return this;
    }

    /** 添加一个佩戴期间持续存在的永久药水效果。 */
    private ArmorSlotBuilder addEffect(String effectId, int amplifier) {
        return addEffect(effectId, amplifier, false, true, true);
    }

    /** 添加一个可控制显示参数的永久药水效果。 */
    public ArmorSlotBuilder addEffect(String effectId, int amplifier, boolean ambient,
                                      boolean showParticles, boolean showIcon) {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        net.minecraft.world.effect.MobEffect effect = id == null ? null
                : ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            throw new IllegalArgumentException("Unknown mob effect: " + effectId);
        }
        if (amplifier < 0) {
            throw new IllegalArgumentException("amplifier must be non-negative");
        }
        providedEffects.add(new ArmorSlotItem.ProvidedEffect(effect, amplifier, ambient,
                showParticles, showIcon));
        return this;
    }

    /** 添加永久药水效果的语义化别名。 */
    public ArmorSlotBuilder addPotionEffect(String effectId, int amplifier) {
        return addEffect(effectId, amplifier);
    }

    /**
     * 根据 Builder 配置创建物品对象。
     * @return 对应的执行结果。
     */
    @Override
    public Item createObject() {
        List<ArmorSlotItem.ArmorDefinition> definitions = new ArrayList<>(armorDefinitions);
        if (definitions.isEmpty()) {
            definitions.add(new ArmorSlotItem.ArmorDefinition(ArmorSlotItem.Kind.IMPACT, null));
        }
        return new ArmorSlotItem(id.getPath(), definitions, recoverySeconds,
                providedAttributes, providedEffects, createItemProperties());
    }

    /**
     * 解析护甲类型名称。
     * @param type 类型。
     * @return 对应的执行结果。
     */
    private static ArmorSlotItem.Kind parseKind(String type) {
        return switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "impact" -> ArmorSlotItem.Kind.IMPACT;
            case "adaptive" -> ArmorSlotItem.Kind.ADAPTIVE;
            case "resistance" -> ArmorSlotItem.Kind.RESISTANCE;
            default -> throw new IllegalArgumentException("Unknown armor type: " + type);
        };
    }
}
