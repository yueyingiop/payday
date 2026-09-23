package com.yueyingiop.payday.compat.kubejs;

import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.helper.AttributeModifierHelper;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** KubeJS 用于创建护甲饰品的物品 Builder。 */
public class ArmorCurioBuilder extends ItemBuilder {
    private int upgradeSlots;
    private final List<ArmorCurioItem.AttributeDefinition> attributes = new ArrayList<>();

    public ArmorCurioBuilder(ResourceLocation id) {
        super(id);
        tag(ResourceLocation.fromNamespaceAndPath(PayDay.MODID, "armor_curios"));
    }

    /** 设置该护甲饰品可以装载的升级插槽数量。 */
    public ArmorCurioBuilder upgradeSlots(int upgradeSlots) {
        if (upgradeSlots < 0) {
            throw new IllegalArgumentException("upgradeSlots must be non-negative");
        }
        this.upgradeSlots = upgradeSlots;
        return this;
    }

    /** 添加护甲饰品自身的默认属性修饰符。 */
    public ArmorCurioBuilder addAttribute(String attributeId, String type, double value) {
        var attribute = AttributeModifierHelper.resolveAttribute(attributeId);
        AttributeModifier.Operation operation = AttributeModifierHelper.parseOperation(type);
        attributes.add(new ArmorCurioItem.AttributeDefinition(attribute, operation, value));
        return this;
    }

    /**
     * 根据 Builder 配置创建物品对象。
     * @return 对应的执行结果。
     */
    @Override
    public Item createObject() {
        return new ArmorCurioItem(upgradeSlots, attributes, createItemProperties());
    }
}
