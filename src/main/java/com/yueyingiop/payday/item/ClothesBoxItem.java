package com.yueyingiop.payday.item;

import com.yueyingiop.payday.item.base.EquipmentBoxItem;

import net.minecraft.world.item.ItemStack;

public final class ClothesBoxItem extends EquipmentBoxItem {
    /**
     * 获取容量。
     * @return 对应的执行结果。
     */
    @Override
    public int capacity() {
        return 1;
    }

    /**
     * 判断是否接收该物品。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    @Override
    public boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof ArmorCurioItem;
    }
}
