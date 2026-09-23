package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorPlateItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.item.FirstAidKitItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/** 处理护甲板、医疗包以及对应的玩家右键使用行为。 */
public final class SupplyEvents {
    private SupplyEvents() {
    }

    /**
     * 处理玩家手持护甲板、医疗包或护甲饰品右键时的收纳和使用逻辑。
     *
     * @param event 玩家右键物品事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (player.isShiftKeyDown() && (held.getItem() instanceof ArmorPlateItem
                || held.getItem() instanceof FirstAidKitItem)) {
            return;
        }

        boolean used = false;
        if (held.getItem() instanceof ArmorPlateItem) {
            if (EquipmentBoxEvents.storeInBox(player, held, com.yueyingiop.payday.item.ArmorBoxItem.class)) {
                used = true;
            } else if (repairArmor(player)) {
                ServerEventUtil.consumeOne(player, held);
                used = true;
            }
        } else if (held.getItem() instanceof FirstAidKitItem) {
            if (EquipmentBoxEvents.storeInBox(player, held, com.yueyingiop.payday.item.MedicalBoxItem.class)) {
                used = true;
            } else if (healPlayer(player)) {
                ServerEventUtil.consumeOne(player, held);
                used = true;
            }
        } else if (held.getItem() instanceof ArmorCurioItem) {
            used = EquipmentBoxEvents.storeInBox(player, held,
                    com.yueyingiop.payday.item.ClothesBoxItem.class);
        }

        if (used) {
            event.setCanceled(true);
        }
    }

    /** 修复最晚损坏且允许修复的护甲插槽。 */
    /**
     * 执行 <code>repairArmor</code> 对应的操作。
     * @param player 参数值。
     * @return 方法执行结果。
     */
    public static boolean repairArmor(Player player) {
        ItemStack selectedArmorStack = ItemStack.EMPTY;
        List<ItemStack> selectedUpgrades = List.of();
        int repairIndex = -1;
        int repairDefinitionIndex = -1;
        long latestBrokenAt = Long.MIN_VALUE;

        for (ItemStack armorStack : CurioHelper.stacks(player, CurioHelper.ARMOR_SLOT)) {
            if (!(armorStack.getItem() instanceof ArmorCurioItem armorCurio)) {
                continue;
            }

            List<ItemStack> upgrades = armorCurio.getUpgradeStacks(armorStack);
            for (int i = 0; i < upgrades.size(); i++) {
                ItemStack stack = upgrades.get(i);
                if (!(stack.getItem() instanceof ArmorSlotItem armor)) {
                    continue;
                }

                for (int definitionIndex = 0;
                     definitionIndex < armor.armorDefinitions().size(); definitionIndex++) {
                    if (!armor.canRepair(stack, definitionIndex)) {
                        continue;
                    }

                    long brokenAt = armor.brokenAt(stack, definitionIndex);
                    if (repairIndex < 0 || brokenAt >= latestBrokenAt) {
                        latestBrokenAt = brokenAt;
                        repairIndex = i;
                        repairDefinitionIndex = definitionIndex;
                        selectedArmorStack = armorStack;
                        selectedUpgrades = upgrades;
                    }
                }
            }
        }

        if (repairIndex < 0 || selectedArmorStack.isEmpty()) {
            return false;
        }

        ItemStack selected = selectedUpgrades.get(repairIndex);
        ArmorSlotItem armor = (ArmorSlotItem) selected.getItem();
        armor.clearPermanent(selected, repairDefinitionIndex);
        armor.setCurrent(selected, repairDefinitionIndex, armor.maxArmor(repairDefinitionIndex));
        armor.clearBroken(selected, repairDefinitionIndex);
        ((ArmorCurioItem) selectedArmorStack.getItem()).setUpgradeStacks(selectedArmorStack, selectedUpgrades);
        return true;
    }

    /** 恢复玩家最大生命值的百分之五十。 */
    /**
     * 执行 <code>healPlayer</code> 对应的操作。
     * @param player 参数值。
     * @return 方法执行结果。
     */
    public static boolean healPlayer(Player player) {
        if (player.getHealth() >= player.getMaxHealth()) {
            return false;
        }
        player.heal(player.getMaxHealth() * 0.5F);
        return true;
    }
}
