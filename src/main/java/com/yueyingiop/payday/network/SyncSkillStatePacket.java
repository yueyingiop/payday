package com.yueyingiop.payday.network;

import com.yueyingiop.payday.client.ClientSkillState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端同步玩家技能点和技能等级。 */
public record SyncSkillStatePacket(int skillPoints, byte[] ranks) {
    public static void encode(SyncSkillStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.skillPoints());
        buffer.writeVarInt(packet.ranks().length);
        buffer.writeBytes(packet.ranks());
    }

    public static SyncSkillStatePacket decode(FriendlyByteBuf buffer) {
        int skillPoints = buffer.readVarInt();
        byte[] ranks = new byte[buffer.readVarInt()];
        buffer.readBytes(ranks);
        return new SyncSkillStatePacket(skillPoints, ranks);
    }

    public static void handle(SyncSkillStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientSkillState.apply(packet)));
        context.setPacketHandled(true);
    }
}
