package com.yueyingiop.payday.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yueyingiop.payday.api.TurretFluidManager;
import com.yueyingiop.payday.block.TurretBlockEntity;
import com.yueyingiop.payday.menu.TurretMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

/** 炮台流体装载界面。 */
public final class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/turret_gui.png");
    private static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/inventory.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 188;

    public TurretScreen(TurretMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = -100;
        titleLabelY = -100;
        inventoryLabelX = -100;
        inventoryLabelY = -100;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, 64, 100, 64, 100);
        graphics.blit(INVENTORY, leftPos, topPos + 100, 0, 0, 176, 88, 176, 88);
        TurretBlockEntity turret = menu.turret();
        if (turret != null) {
            renderFluid(graphics, turret.getCoolantFluid(), 11, 11);
            renderFluid(graphics, turret.getOverloadFluid(), 37, 11);
        }
    }

    private void renderFluid(GuiGraphics graphics, FluidStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        int height = Math.max(1, Math.min(56, Math.round(56.0F * stack.getAmount()
                / TurretBlockEntity.FLUID_CAPACITY)));
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation texture = fluidType.getStillTexture(stack);
        int tint = fluidType.getTintColor(stack);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        int bottom = topPos + y + 56;
        int top = bottom - height;
        graphics.enableScissor(leftPos + x, top, leftPos + x + 16, bottom);
        for (int drawY = bottom - 16; drawY >= top - 16; drawY -= 16) {
            graphics.blit(leftPos + x, drawY, 0, 16, 16, sprite);
        }
        graphics.disableScissor();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 标签已经绘制在 GUI 纹理中。
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFluidTooltip(graphics, mouseX, mouseY);
    }

    private void renderFluidTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        int tank = localX >= 11 && localX < 27 && localY >= 11 && localY < 67 ? 0
                : localX >= 37 && localX < 53 && localY >= 11 && localY < 67 ? 1 : -1;
        if (tank < 0) {
            return;
        }
        TurretBlockEntity turret = menu.turret();
        if (turret == null) {
            return;
        }
        FluidStack stack = tank == 0 ? turret.getCoolantFluid() : turret.getOverloadFluid();
        Component name = stack.isEmpty() ? Component.translatable("gui.payday.turret_fluid_empty")
                : stack.getDisplayName();
        Component amount = Component.translatable("gui.payday.turret_fluid_amount",
                stack.getAmount(), TurretBlockEntity.FLUID_CAPACITY);
        var definition = stack.isEmpty() ? null : TurretFluidManager.get(stack.getFluid());
        Component type = Component.translatable(stack.isEmpty()
                ? "gui.payday.turret_fluid_type_empty"
                : definition == null ? "turret_fluid.payday.unknown" : definition.type().translationKey());
        graphics.renderTooltip(font, List.of(name, amount, type), Optional.empty(), mouseX, mouseY);
    }
}
