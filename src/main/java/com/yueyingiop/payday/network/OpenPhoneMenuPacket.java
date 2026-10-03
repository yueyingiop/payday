package com.yueyingiop.payday.network;

import com.yueyingiop.payday.menu.PhoneMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenPhoneMenuPacket() {
    public static void encode(OpenPhoneMenuPacket packet, FriendlyByteBuf buffer) { }
    public static OpenPhoneMenuPacket decode(FriendlyByteBuf buffer) { return new OpenPhoneMenuPacket(); }
    public static void handle(OpenPhoneMenuPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) PhoneMenu.openFor(player);
        });
        context.setPacketHandled(true);
    }
}
