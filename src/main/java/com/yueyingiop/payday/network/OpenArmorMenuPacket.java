package com.yueyingiop.payday.network;

import com.yueyingiop.payday.menu.ArmorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record OpenArmorMenuPacket() {
    /**
     * 编码数据。
     * @param packet 数据包。
     * @param buffer 网络缓冲区。
     */
    public static void encode(OpenArmorMenuPacket packet, FriendlyByteBuf buffer) { }
    /**
     * 解码数据。
     * @param buffer 网络缓冲区。
     * @return 对应的执行结果。
     */
    public static OpenArmorMenuPacket decode(FriendlyByteBuf buffer) { return new OpenArmorMenuPacket(); }
    /**
     * 处理对应事件。
     * @param packet 数据包。
     * @param supplier 网络上下文提供器。
     */
    public static void handle(OpenArmorMenuPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) ArmorMenu.openFor(player);
        });
        context.setPacketHandled(true);
    }
}
