package com.yueyingiop.payday.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yueyingiop.payday.menu.PhoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Screen for installing phone upgrades. Coordinates match phone_upgrade_gui.png. */
public class PhoneScreen extends AbstractContainerScreen<PhoneMenu> {
    public static final int GUI_WIDTH = 313;
    public static final int GUI_HEIGHT = 230;
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/phone_upgrade_gui.png");
    private static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/inventory.png");
    private static final int INVENTORY_TOP = 123;

    public PhoneScreen(PhoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = -12;
        inventoryLabelX = 8;
        inventoryLabelY = 126;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(BG, leftPos, topPos, 0, 0, GUI_WIDTH, 123, GUI_WIDTH, 123);
        // 背包背景紧贴手机 HUD 的底边，不再保留原来的 10px 空隙。
        graphics.blit(INVENTORY, leftPos + 68, topPos + INVENTORY_TOP, 0, 0, 176, 88, 176, 88);
        if (!menu.phoneDisplay().isEmpty()) {
            graphics.renderItem(menu.phoneDisplay(), leftPos + 276, topPos + 6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The supplied background contains the visual labels.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredSlot != null && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(font, hoveredSlot.getItem(), mouseX, mouseY);
        }
    }
}
