package com.yueyingiop.payday.network;

import com.yueyingiop.payday.api.PhoneUpgradeAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 请求服务端激活与指定按键绑定的手机升级。 */
public record ActivateHackerSkillPacket(int key) {
    /** 编码客户端发送的按键码。 */
    public static void encode(ActivateHackerSkillPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.key());
    }

    /** 解码客户端发送的按键码。 */
    public static ActivateHackerSkillPacket decode(FriendlyByteBuf buffer) {
        return new ActivateHackerSkillPacket(buffer.readVarInt());
    }

    /** 在服务端验证玩家已安装的按键升级并执行激活回调。 */
    public static void handle(ActivateHackerSkillPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PhoneUpgradeAction.activateBoundKey(player, packet.key());
            }
        });
        context.setPacketHandled(true);
    }
}
