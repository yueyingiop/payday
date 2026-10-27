package com.yueyingiop.payday.network;

import com.yueyingiop.payday.skill.SkillProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端打开技能界面时请求当前技能状态。 */
public record RequestSkillStatePacket() {
    public static void encode(RequestSkillStatePacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestSkillStatePacket decode(FriendlyByteBuf buffer) {
        return new RequestSkillStatePacket();
    }

    public static void handle(RequestSkillStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SkillProgress.sync(player);
            }
        });
        context.setPacketHandled(true);
    }
}
