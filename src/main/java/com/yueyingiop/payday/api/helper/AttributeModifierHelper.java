package com.yueyingiop.payday.api.helper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/** Shared parsing for KubeJS attribute modifier definitions. */
public final class AttributeModifierHelper {
    private static final ResourceLocation TACZ_BULLET_RESISTANCE =
            ResourceLocation.fromNamespaceAndPath("tacz", "tacz.bullet_resistance");

    private AttributeModifierHelper() {
    }

    /**
     * 解析属性标识。
     * @param attributeId 属性标识。
     * @return 对应的执行结果。
     */
    public static Attribute resolveAttribute(String attributeId) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        Attribute attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute: " + attributeId);
        }
        return attribute;
    }

    /** Converts PayDay's basis-point resistance amount to TACZ's decimal storage amount. */
    public static double runtimeAmount(Attribute attribute, double amount,
                                       AttributeModifier.Operation operation) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (TACZ_BULLET_RESISTANCE.equals(id)
                && operation == AttributeModifier.Operation.ADDITION) {
            return amount / 10000.0D;
        }
        return amount;
    }

    /**
     * 解析属性修饰符操作类型。
     * @param type 类型。
     * @return 对应的执行结果。
     */
    public static AttributeModifier.Operation parseOperation(String type) {
        try {
            return AttributeModifier.Operation.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown attribute operation: " + type, exception);
        }
    }
}
