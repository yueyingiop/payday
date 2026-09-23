package com.yueyingiop.payday.network;

import com.yueyingiop.payday.client.ClientEquipmentBoxStateSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端向客户端同步装备箱饰品槽位和其中物品数据。 */
public record SyncEquipmentBoxStatePacket(int entityId, int slotIndex, ItemStack stack) {
    /**
     * 编码数据。
     * @param packet 数据包。
     * @param buffer 网络缓冲区。
     */
    public static void encode(SyncEquipmentBoxStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
        buffer.writeInt(packet.slotIndex());
        buffer.writeItem(packet.stack());
    }

    /**
     * 解码数据。
     * @param buffer 网络缓冲区。
     * @return 对应的执行结果。
     */
    public static SyncEquipmentBoxStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncEquipmentBoxStatePacket(buffer.readInt(), buffer.readInt(), buffer.readItem());
    }

    /**
     * 处理对应事件。
     * @param packet 数据包。
     * @param supplier 网络上下文提供器。
     */
    public static void handle(SyncEquipmentBoxStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEquipmentBoxStateSync.apply(packet)));
        context.setPacketHandled(true);
    }
}
