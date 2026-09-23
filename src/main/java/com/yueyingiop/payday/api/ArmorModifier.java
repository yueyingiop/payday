package com.yueyingiop.payday.api;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.item.ArmorCurioItem;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class ArmorModifier {
    private final ArmorCurioItem item;
    public ArmorModifier(String itemId) {
        Item found = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
        if (!(found instanceof ArmorCurioItem armor)) { PayDay.LOGGER.error("{} is not a registered ArmorCurioItem", itemId); throw new IllegalArgumentException(itemId); }
        this.item = armor;
    }
    /**
     * 修改护甲属性修饰符。
     * @param attributeId 属性标识。
     * @param type 类型。
     * @param value 数值。
     * @return 对应的执行结果。
     */
    public ArmorModifier modifyAttribute(String attributeId, String type, float value) { return change(attributeId, type, value, true); }
    /**
     * 添加护甲属性修饰符。
     * @param attributeId 属性标识。
     * @param type 类型。
     * @param value 数值。
     * @return 对应的执行结果。
     */
    public ArmorModifier addAttribute(String attributeId, String type, float value) { return change(attributeId, type, value, false); }
    /**
     * 移除护甲属性修饰符。
     * @param attributeId 属性标识。
     * @param type 类型。
     * @return 对应的执行结果。
     */
    public ArmorModifier removeAttribute(String attributeId, String type) { return change(attributeId, type, 0, true); }
    /**
     * 更新对应状态。
     * @param attributeId 属性标识。
     * @param type 类型。
     * @param value 数值。
     * @param replace 是否替换已有修饰符。
     * @return 对应的执行结果。
     */
    private ArmorModifier change(String attributeId, String type, float value, boolean replace) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(attributeId));
        if (attribute == null) { PayDay.LOGGER.error("Unknown attribute {}", attributeId); return this; }
        ArmorModifierRegistry.change(item, attributeId + "|" + type.toUpperCase(java.util.Locale.ROOT), attribute, type, value, replace);
        return this;
    }
}
