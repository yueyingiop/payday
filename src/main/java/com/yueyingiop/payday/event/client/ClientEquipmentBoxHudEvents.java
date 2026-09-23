package com.yueyingiop.payday.event.client;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.client.render.EquipmentBoxHudRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 处理佩戴装备箱时的内容 HUD。 */
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEquipmentBoxHudEvents {
    private ClientEquipmentBoxHudEvents() {
    }

    /** 在原版快捷栏绘制完成后绘制装备箱内容 HUD。 */
    /**
     * 执行 <code>renderEquipmentBoxHud</code> 对应的操作。
     * @param event 参数值。
     */
    @SubscribeEvent
    public static void renderEquipmentBoxHud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !Config.EQUIPMENT_BOX_HUD_ENABLED.get()) {
            return;
        }

        EquipmentBoxHudRenderer.render(event.getGuiGraphics(), player);
    }
}
