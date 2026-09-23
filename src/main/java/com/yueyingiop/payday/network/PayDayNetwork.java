package com.yueyingiop.payday.network;

import com.yueyingiop.payday.PayDay;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PayDayNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(PayDay.MODID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );
    private static int id;

    private PayDayNetwork() {
    }

    /**
     * 注册对应内容。
     */
    public static void register() {
        CHANNEL.registerMessage(
            id++, 
            OpenArmorMenuPacket.class, 
            OpenArmorMenuPacket::encode,
            OpenArmorMenuPacket::decode, 
            OpenArmorMenuPacket::handle
        );
        CHANNEL.registerMessage(
            id++,
            UseEquipmentBoxPacket.class,
            UseEquipmentBoxPacket::encode,
            UseEquipmentBoxPacket::decode,
            UseEquipmentBoxPacket::handle
        );
        CHANNEL.registerMessage(
            id++,
            SyncArmorStatePacket.class,
            SyncArmorStatePacket::encode,
            SyncArmorStatePacket::decode,
            SyncArmorStatePacket::handle
        );
        CHANNEL.registerMessage(
            id++,
            SyncEquipmentBoxStatePacket.class,
            SyncEquipmentBoxStatePacket::encode,
            SyncEquipmentBoxStatePacket::decode,
            SyncEquipmentBoxStatePacket::handle
        );
    }
}
