package com.yueyingiop.payday.event.client;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.init.PayDayClient;
import com.yueyingiop.payday.network.OpenArmorMenuPacket;
import com.yueyingiop.payday.network.PayDayNetwork;
import com.yueyingiop.payday.network.UseEquipmentBoxPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** 处理 PayDay 客户端快捷键。 */
@Mod.EventBusSubscriber(modid = PayDay.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientInputEvents {
    private ClientInputEvents() {
    }

    /**
     * 处理 Shift+C 打开护甲界面，以及 V 或 Shift+V 使用装备箱。
     *
     * @param event 客户端键盘输入事件
     */
    @SubscribeEvent
    public static void keyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (event.getKey() == GLFW.GLFW_KEY_H && minecraft.screen == null) {
            minecraft.setScreen(new com.yueyingiop.payday.client.screen.HudConfigScreen());
            return;
        }

        boolean shift = PayDayClient.MODIFIER.isDown();
        if (PayDayClient.OPEN_ARMOR.matches(event.getKey(), event.getScanCode()) && shift) {
            PayDayNetwork.CHANNEL.sendToServer(new OpenArmorMenuPacket());
        } else if (PayDayClient.USE_EQUIPMENT_BOX.matches(event.getKey(), event.getScanCode())) {
            PayDayNetwork.CHANNEL.sendToServer(new UseEquipmentBoxPacket(shift));
        }
    }
}
