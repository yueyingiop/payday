package com.yueyingiop.payday.network;

import com.yueyingiop.payday.client.ClientArmorStateSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端向客户端同步护甲饰品槽位和更衣 CD 的数据包。 */
public record SyncArmorStatePacket(int entityId, int slotIndex, long cooldownTicks,
                                   long equipmentBoxChangeCooldownTicks, ItemStack stack) {
    /**
     * 编码数据。
     * @param packet 数据包。
     * @param buffer 网络缓冲区。
     */
    public static void encode(SyncArmorStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
        buffer.writeInt(packet.slotIndex());
        buffer.writeLong(packet.cooldownTicks());
        buffer.writeLong(packet.equipmentBoxChangeCooldownTicks());
        buffer.writeItem(packet.stack());
    }

    /**
     * 解码数据。
     * @param buffer 网络缓冲区。
     * @return 对应的执行结果。
     */
    public static SyncArmorStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncArmorStatePacket(buffer.readInt(), buffer.readInt(), buffer.readLong(),
                buffer.readLong(), buffer.readItem());
    }

    /**
     * 处理对应事件。
     * @param packet 数据包。
     * @param supplier 网络上下文提供器。
     */
    public static void handle(SyncArmorStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientArmorStateSync.apply(packet)));
        context.setPacketHandled(true);
    }
}
