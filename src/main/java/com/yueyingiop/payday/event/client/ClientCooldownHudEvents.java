package com.yueyingiop.payday.event.client;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.CooldownHudClientManager;
import com.yueyingiop.payday.client.render.CooldownHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 处理冷却 HUD 的客户端绘制和客户端缓存清理。 */
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientCooldownHudEvents {
    private ClientCooldownHudEvents() {
    }

    /** 在原版快捷栏绘制后绘制左上角冷却 HUD。 */
    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())
                || Minecraft.getInstance().player == null) {
            return;
        }
        CooldownHudRenderer.render(event.getGuiGraphics());
    }

    /** 客户端断开服务器时清理上一位玩家的 HUD。 */
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        CooldownHudClientManager.clear();
    }
}
