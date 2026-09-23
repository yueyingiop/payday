package com.yueyingiop.payday.client;

import com.yueyingiop.payday.network.SyncEquipmentBoxStatePacket;
import com.yueyingiop.payday.api.helper.CurioHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 应用服务端同步到客户端的装备箱槽位数据。 */
public final class ClientEquipmentBoxStateSync {
    private ClientEquipmentBoxStateSync() {
    }

    /** 更新客户端玩家的装备箱物品和内部 NBT。 */
    /**
     * 执行 <code>apply</code> 对应的操作。
     * @param packet 参数值。
     */
    public static void apply(SyncEquipmentBoxStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(packet.entityId());
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        CurioHelper.setStack(living, CurioHelper.EQUIPMENT_BOX_SLOT, packet.slotIndex(), packet.stack().copy());
    }
}
