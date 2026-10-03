package com.yueyingiop.payday.event.client;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.client.render.ExposureRiskHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 在玩家打开 E 物品栏时显示暴露风险 HUD。 */
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientExposureRiskHudEvents {
    private ClientExposureRiskHudEvents() {
    }

    /**
     * 绘制物品栏暴露风险 HUD。
     * @param event 事件。
     */
    @SubscribeEvent
    public static void renderInventory(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen)
                && !(event.getContainerScreen() instanceof CreativeModeInventoryScreen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !Config.EXPOSURE_RISK_HUD_ENABLED.get()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        // Foreground 事件位于 AbstractContainerScreen 的局部坐标系中，
        // 这里抵消 guiLeft/guiTop 平移，使 HUD 继续使用屏幕绝对坐标，
        // 从而与 PayDayConfigScreen 中的预览位置保持一致。
        graphics.pose().pushPose();
        graphics.pose().translate(-event.getContainerScreen().getGuiLeft(),
                -event.getContainerScreen().getGuiTop(), 0.0D);
        ExposureRiskHudRenderer.render(graphics, minecraft.player);
        graphics.pose().popPose();
    }
}
