package com.yueyingiop.payday.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.yueyingiop.payday.event.server.EquipmentBoxEvents;

import java.util.function.Supplier;

public record UseEquipmentBoxPacket(boolean shift) {
    /**
     * 编码数据。
     * @param packet 数据包。
     * @param buffer 网络缓冲区。
     */
    public static void encode(UseEquipmentBoxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.shift);
    }

    /**
     * 解码数据。
     * @param buffer 网络缓冲区。
     * @return 对应的执行结果。
     */
    public static UseEquipmentBoxPacket decode(FriendlyByteBuf buffer) {
        return new UseEquipmentBoxPacket(buffer.readBoolean());
    }

    /**
     * 处理对应事件。
     * @param packet 数据包。
     * @param supplier 网络上下文提供器。
     */
    public static void handle(UseEquipmentBoxPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                EquipmentBoxEvents.useEquipmentBox(player, packet.shift());
            }
        });
        context.setPacketHandled(true);
    }
}
