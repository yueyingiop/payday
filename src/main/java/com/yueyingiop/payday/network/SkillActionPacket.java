package com.yueyingiop.payday.network;

import com.yueyingiop.payday.skill.SkillProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端发送的技能界面操作。所有条件检查均在服务端执行。 */
public record SkillActionPacket(int action, int category, int series, int node) {
    public static final int LEARN = 0;
    public static final int FORGET = 1;
    public static final int RESET_CATEGORY = 2;
    public static final int RESET_ALL = 3;
    public static final int CONVERT_LEVEL = 4;
    public static final int CONVERT_ALL = 5;

    public static void encode(SkillActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action());
        buffer.writeVarInt(packet.category());
        buffer.writeVarInt(packet.series());
        buffer.writeVarInt(packet.node());
    }

    public static SkillActionPacket decode(FriendlyByteBuf buffer) {
        return new SkillActionPacket(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SkillActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            switch (packet.action()) {
                case LEARN -> SkillProgress.learn(player, packet.category(), packet.series(), packet.node());
                case FORGET -> SkillProgress.forget(player, packet.category(), packet.series(), packet.node());
                case RESET_CATEGORY -> SkillProgress.resetCategory(player, packet.category());
                case RESET_ALL -> SkillProgress.resetAll(player);
                case CONVERT_LEVEL -> SkillProgress.convertTenLevels(player);
                case CONVERT_ALL -> SkillProgress.convertAllExperience(player);
                default -> {
                    return;
                }
            }
            SkillProgress.sync(player);
        });
        context.setPacketHandled(true);
    }
}
