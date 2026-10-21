package com.yueyingiop.payday.network;

import com.yueyingiop.payday.api.CooldownHudClientManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端向客户端同步冷却 HUD 状态。 */
public record CooldownHudSyncPacket(int entityId, String id, boolean remove,
                                    int type, String texture, double value,
                                    Component name, boolean showValue, boolean percentage,
                                    int textureU, int textureV,
                                    long remainingTicks, long totalTicks) {
    /**
     * 编码数据包。
     *
     * @param packet 数据包
     * @param buffer 网络缓冲区
     */
    public static void encode(CooldownHudSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
        buffer.writeUtf(packet.id());
        buffer.writeBoolean(packet.remove());
        buffer.writeInt(packet.type());
        buffer.writeUtf(packet.texture());
        buffer.writeDouble(packet.value());
        buffer.writeComponent(packet.name());
        buffer.writeBoolean(packet.showValue());
        buffer.writeBoolean(packet.percentage());
        buffer.writeInt(packet.textureU());
        buffer.writeInt(packet.textureV());
        buffer.writeLong(packet.remainingTicks());
        buffer.writeLong(packet.totalTicks());
    }

    /**
     * 解码数据包。
     *
     * @param buffer 网络缓冲区
     * @return 解码后的数据包
     */
    public static CooldownHudSyncPacket decode(FriendlyByteBuf buffer) {
        return new CooldownHudSyncPacket(buffer.readInt(), buffer.readUtf(), buffer.readBoolean(),
                buffer.readInt(), buffer.readUtf(), buffer.readDouble(), buffer.readComponent(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readInt(), buffer.readInt(),
                buffer.readLong(), buffer.readLong());
    }

    /**
     * 处理数据包。
     *
     * @param packet 数据包
     * @param supplier 网络上下文提供器
     */
    public static void handle(CooldownHudSyncPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CooldownHudClientManager.apply(packet)));
        context.setPacketHandled(true);
    }
}
