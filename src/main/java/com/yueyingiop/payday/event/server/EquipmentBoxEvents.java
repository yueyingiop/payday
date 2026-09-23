package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.block.SupplyBlock;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorPlateItem;
import com.yueyingiop.payday.item.ClothesBoxItem;
import com.yueyingiop.payday.item.FirstAidKitItem;
import com.yueyingiop.payday.item.base.EquipmentBoxItem;
import com.yueyingiop.payday.network.PayDayNetwork;
import com.yueyingiop.payday.network.SyncEquipmentBoxStatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/** 处理护甲箱、医疗箱和衣帽箱的服务端操作。 */
public final class EquipmentBoxEvents {
    private EquipmentBoxEvents() {
    }

    /**
     * 响应 V 或 Shift+V，使用、丢出或更换装备箱中的物品。
     *
     * @param player 使用快捷键的玩家
     * @param shift 是否按下 Shift
     */
    public static void useEquipmentBox(Player player, boolean shift) {
        Optional<SlotResult> result = CurioHelper.findFirst(player, CurioHelper.EQUIPMENT_BOX_SLOT,
                stack -> stack.getItem() instanceof EquipmentBoxItem);
        if (result.isEmpty() || !(result.get().stack().getItem() instanceof EquipmentBoxItem box)) {
            return;
        }

        ItemStack boxStack = result.get().stack();
        if (box instanceof ClothesBoxItem) {
            if (shift) {
                ItemStack stored = box.takeOne(boxStack);
                if (!stored.isEmpty()) {
                    player.drop(stored, false);
                    syncClientState(player);
                }
            } else if (ArmorCurioEvents.hasClothesBoxCooldown(player)) {
                ArmorCurioEvents.sendClothesBoxCooldownMessage(player);
            } else {
                changeArmorFromClothesBox(player, boxStack);
            }
            return;
        }

        if (!shift && ServerEventUtil.hasEquipmentBoxUseCooldown(player)) {
            ServerEventUtil.sendEquipmentBoxUseCooldownMessage(player);
            return;
        }

        ItemStack stored = box.takeOne(boxStack);
        if (stored.isEmpty()) {
            return;
        }

        boolean used = shift ? dropStoredSupply(player, stored)
                : stored.getItem() instanceof ArmorPlateItem
                ? SupplyEvents.repairArmor(player)
                : stored.getItem() instanceof FirstAidKitItem && SupplyEvents.healPlayer(player);
        if (!used) {
            box.insertOne(boxStack, stored);
        } else if (!shift) {
            ServerEventUtil.setEquipmentBoxUseCooldown(player);
        }
        syncClientState(player);
    }

    /** 将玩家手中的物品存入对应类型的装备箱。 */
    /**
     * 执行 <code>storeInBox</code> 对应的操作。
     * @param player 参数值。
     * @param source 参数值。
     * @param boxType 参数值。
     * @return 方法执行结果。
     */
    public static boolean storeInBox(Player player, ItemStack source,
                                     Class<? extends EquipmentBoxItem> boxType) {
        Optional<SlotResult> result = CurioHelper.findFirst(player, CurioHelper.EQUIPMENT_BOX_SLOT,
                stack -> boxType.isInstance(stack.getItem()));
        if (result.isEmpty()) {
            return false;
        }

        EquipmentBoxItem box = (EquipmentBoxItem) result.get().stack().getItem();
        if (!box.insertOne(result.get().stack(), source)) {
            return false;
        }
        ServerEventUtil.consumeOne(player, source);
        syncClientState(player);
        return true;
    }

    /** 从衣帽箱取出护甲饰品并强制替换当前护甲饰品。 */
    /**
     * 执行 <code>changeArmorFromClothesBox</code> 对应的操作。
     * @param player 参数值。
     * @param boxStack 参数值。
     */
    private static void changeArmorFromClothesBox(Player player, ItemStack boxStack) {
        ItemStack replacement = ((EquipmentBoxItem) boxStack.getItem()).peek(boxStack);
        if (!(replacement.getItem() instanceof ArmorCurioItem)) {
            return;
        }

        Optional<SlotResult> armorResult = CurioHelper.findFirst(player, CurioHelper.ARMOR_SLOT,
                stack -> stack.getItem() instanceof ArmorCurioItem);
        int armorIndex = armorResult.map(result -> result.slotContext().index()).orElse(0);
        ItemStack previous = armorResult.map(result -> result.stack().copy()).orElse(ItemStack.EMPTY);
        ItemStack equippedBefore = CurioHelper.stackAt(player, CurioHelper.ARMOR_SLOT, armorIndex).copy();
        ItemStack forcedReplacement = replacement.copyWithCount(1);
        ArmorCurioEvents.markForcedArmorChange(player, forcedReplacement);
        if (!CurioHelper.setStack(player, CurioHelper.ARMOR_SLOT, armorIndex, forcedReplacement.copy())) {
            return;
        }

        ItemStack equippedAfter = CurioHelper.stackAt(player, CurioHelper.ARMOR_SLOT, armorIndex);
        if (!ItemStack.isSameItemSameTags(equippedAfter, replacement) || equippedAfter.getCount() != 1) {
            CurioHelper.setStack(player, CurioHelper.ARMOR_SLOT, armorIndex, equippedBefore);
            return;
        }

        ListTag contents = new ListTag();
        if (!previous.isEmpty()) {
            contents.add(previous.save(new CompoundTag()));
        }
        boxStack.getOrCreateTag().put(EquipmentBoxItem.CONTENTS, contents);
        ArmorCurioEvents.setClothesBoxCooldown(player);
        syncClientState(player);
    }

    /** 将服务端装备箱槽位的最新物品数据立即同步到客户端。 */
    /**
     * 执行 <code>syncClientState</code> 对应的操作。
     * @param player 参数值。
     */
    public static void syncClientState(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        for (CurioHelper.SlotStack slot : CurioHelper.slots(serverPlayer,
                CurioHelper.EQUIPMENT_BOX_SLOT)) {
            PayDayNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncEquipmentBoxStatePacket(serverPlayer.getId(), slot.index(),
                            slot.stack().copy()));
        }
    }

    /** 将装备箱中的补给放到玩家当前指向的方块上。 */
    /**
     * 执行 <code>dropStoredSupply</code> 对应的操作。
     * @param player 参数值。
     * @param stored 参数值。
     * @return 方法执行结果。
     */
    private static boolean dropStoredSupply(Player player, ItemStack stored) {
        SupplyBlock.SupplyType type;
        if (stored.getItem() instanceof ArmorPlateItem) {
            type = SupplyBlock.SupplyType.ARMOR_PLATE;
        } else if (stored.getItem() instanceof FirstAidKitItem) {
            type = SupplyBlock.SupplyType.FIRST_AID;
        } else {
            return false;
        }
        return SupplyBlock.placeAtLook(player, stored, type);
    }
}
