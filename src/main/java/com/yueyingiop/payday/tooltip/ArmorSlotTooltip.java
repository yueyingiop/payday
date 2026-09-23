package com.yueyingiop.payday.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tooltip data for armor upgrade items installed in an armor curio. */
public final class ArmorSlotTooltip implements TooltipComponent {
    private final List<ItemStack> items;

    public ArmorSlotTooltip(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            copies.add(item.copyWithCount(1));
        }
        this.items = Collections.unmodifiableList(copies);
    }

    /**
     * 返回提示组件包含的物品列表。
     * @return 对应的执行结果。
     */
    public List<ItemStack> items() {
        return items;
    }
}
