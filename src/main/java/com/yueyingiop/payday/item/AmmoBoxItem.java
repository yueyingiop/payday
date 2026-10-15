package com.yueyingiop.payday.item;

import com.yueyingiop.payday.item.base.EquipmentBoxItem;
import net.minecraft.world.item.ItemStack;

/** Equipment box that stores up to sixteen ammo bags. */
public final class AmmoBoxItem extends EquipmentBoxItem {
    @Override
    public int capacity() {
        return 16;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof AmmoBagItem;
    }
}
