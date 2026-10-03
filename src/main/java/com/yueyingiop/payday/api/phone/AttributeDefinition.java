package com.yueyingiop.payday.api.phone;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.Supplier;

/**
 * 手机升级使用的属性修饰符定义。
 *
 * <p>属性通过 {@link Supplier} 延迟解析，避免在 Forge 注册表尚未完成时过早调用
 * {@code RegistryObject#get()}。</p>
 *
 * @param attributeSupplier 属性提供器
 * @param amount 属性修饰数值
 * @param operation 属性修饰运算方式
 */
public record AttributeDefinition(Supplier<Attribute> attributeSupplier, double amount, AttributeModifier.Operation operation) {
    /** 创建属性定义并校验必要参数。 */
    public AttributeDefinition {
        if (attributeSupplier == null || operation == null) {
            throw new IllegalArgumentException("attributeSupplier and operation must not be null");
        }
    }

    /** 使用已经解析出的属性创建定义。 */
    public AttributeDefinition(Attribute attribute, double amount,
                               AttributeModifier.Operation operation) {
        this(() -> attribute, amount, operation);
    }

    /** 返回当前定义对应的属性。 */
    public Attribute attribute() {
        return attributeSupplier.get();
    }
}
