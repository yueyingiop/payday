package com.yueyingiop.payday.event.client;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.client.render.ArmorHudRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 处理护甲 HUD 的客户端绘制。 */
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientArmorHudEvents {
    private ClientArmorHudEvents() {
    }

    /**
     * 在原版生命值或护甲值 HUD 附近绘制 PayDay 护甲值。
     *
     * @param event GUI overlay 绘制结束事件
     */
    @SubscribeEvent
    public static void renderArmorHud(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.isCreative() || !ClientEventUtil.hasArmorCurio(player)) return;
        boolean hasVanillaArmor = player.getArmorValue() > 0;
        if (hasVanillaArmor) {
            if (!event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) return;
        } else if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            return;
        }

        ArmorHudRenderer.render(event.getGuiGraphics(), player);
    }
}
