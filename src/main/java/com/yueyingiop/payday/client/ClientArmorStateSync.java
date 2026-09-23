package com.yueyingiop.payday.client;

import com.yueyingiop.payday.event.server.ArmorCurioEvents;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.network.SyncArmorStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 客户端应用服务端护甲饰品状态同步。 */
@OnlyIn(Dist.CLIENT)
public final class ClientArmorStateSync {
    private ClientArmorStateSync() {
    }

    /** 应用服务端发来的槽位物品和更衣 CD。 */
    /**
     * 执行 <code>apply</code> 对应的操作。
     * @param packet 参数值。
     */
    public static void apply(SyncArmorStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(packet.entityId());
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        ArmorCurioEvents.setClientCooldown(living.getUUID(),
                packet.cooldownTicks() > 0L
                        ? minecraft.level.getGameTime() + packet.cooldownTicks()
                        : 0L);
        ArmorCurioEvents.setClientEquipmentBoxChangeCooldown(living.getUUID(),
                packet.equipmentBoxChangeCooldownTicks() > 0L
                        ? minecraft.level.getGameTime() + packet.equipmentBoxChangeCooldownTicks()
                        : 0L);
        CurioHelper.setStack(living, CurioHelper.ARMOR_SLOT, packet.slotIndex(), packet.stack().copy());
    }
}
