package com.yueyingiop.payday.client;

import com.yueyingiop.payday.tooltip.ArmorSlotTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ClientArmorSlotTooltip implements ClientTooltipComponent {
    private static final int COLUMNS = 9;
    private static final int SLOT_SIZE = 18;
    private final List<ItemStack> items;

    public ClientArmorSlotTooltip(ArmorSlotTooltip tooltip) {
        this.items = tooltip.items();
    }

    /**
     * 获取提示组件高度。
     * @return 对应的执行结果。
     */
    @Override
    public int getHeight() {
        return ((items.size() + COLUMNS - 1) / COLUMNS) * SLOT_SIZE;
    }

    /**
     * 计算提示组件宽度。
     * @param font 字体。
     * @return 对应的执行结果。
     */
    @Override
    public int getWidth(Font font) {
        return Math.min(COLUMNS, Math.max(1, items.size())) * SLOT_SIZE;
    }

    /**
     * 绘制护甲插槽提示图像。
     * @param font 字体。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param graphics 图形渲染上下文。
     */
    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        for (int i = 0; i < items.size(); i++) {
            int slotX = x + (i % COLUMNS) * SLOT_SIZE;
            int slotY = y + (i / COLUMNS) * SLOT_SIZE;
            graphics.renderItem(items.get(i), slotX + 1, slotY + 1, i);
            graphics.renderItemDecorations(font, items.get(i), slotX + 1, slotY + 1);
        }
    }
}
