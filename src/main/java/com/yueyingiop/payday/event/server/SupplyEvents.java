package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.PhoneUpgradeAction;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.AmmoBagItem;
import com.yueyingiop.payday.item.ArmorPlateItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.item.FirstAidKitItem;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

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
                || held.getItem() instanceof FirstAidKitItem
                || held.getItem() instanceof AmmoBagItem)) {
            return;
        }

        if (!player.isShiftKeyDown()
                && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && held.getItem() instanceof AmmoBagItem
                && supplyAmmoFromOffhandGun(player)) {
            ServerEventUtil.consumeOne(player, held);
            event.setCanceled(true);
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
        } else if (held.getItem() instanceof AmmoBagItem) {
            if (EquipmentBoxEvents.storeInBox(player, held,
                    com.yueyingiop.payday.item.AmmoBoxItem.class)) {
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

    /** 处理主手弹药包配合副手枪械对普通方块的右键使用。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getEntity().isShiftKeyDown()
                || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        if (event.getLevel().getBlockState(event.getPos()).getBlock()
                instanceof com.yueyingiop.payday.block.SupplyBlock supply
                && supply.supplyType() == com.yueyingiop.payday.block.SupplyBlock.SupplyType.AMMO_BAG) {
            return;
        }

        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof AmmoBagItem)
                || !supplyAmmoFromOffhandGun(player)) {
            return;
        }

        ServerEventUtil.consumeOne(player, player.getMainHandItem());
        event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    /** 修复最晚损坏且允许修复的护甲插槽。 */
    /**
     * 执行 <code>repairArmor</code> 对应的操作。
     * @param player 参数值。
     * @return 方法执行结果。
     */
    public static boolean repairArmor(Player player) {
        PhoneItem.refreshArmorSlotModifiers(player);
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
                    double current = ArmorModifier.current(player, armorStack, armor, stack,
                            definitionIndex);
                    double maximum = ArmorModifier.effectiveMax(player, armorStack, armor,
                            stack, definitionIndex);
                    boolean canRepair = armor.canRepair(stack, definitionIndex)
                            || current < maximum;
                    if (!canRepair) {
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
        ArmorModifier.setCurrent(player, selectedArmorStack, armor, selected,
                repairDefinitionIndex,
                ArmorModifier.effectiveMax(player, selectedArmorStack, armor, selected,
                        repairDefinitionIndex));
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

    /** 给玩家当前主手枪械对应的弹药，数量为最终弹匣容量的两倍。 */
    public static boolean supplyAmmo(Player player) {
        if (player.level().isClientSide) {
            return false;
        }

        ItemStack gunStack = player.getMainHandItem();
        return supplyAmmo(player, gunStack);
    }

    /** 使用副手 TACZ 枪械为弹药包补充两倍弹匣容量的弹药。 */
    public static boolean supplyAmmoFromOffhandGun(Player player) {
        if (player.level().isClientSide) {
            return false;
        }

        ItemStack gunStack = player.getOffhandItem();
        if (IGun.getIGunOrNull(gunStack) == null) {
            return false;
        }
        return supplyAmmo(player, gunStack);
    }

    private static boolean supplyAmmo(Player player, ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return false;
        }

        ResourceLocation gunId = gun.getGunId(gunStack);
        CommonGunIndex gunIndex = CommonAssetsManager.getInstance().getGunIndex(gunId);
        if (gunIndex == null || gunIndex.getGunData() == null
                || gunIndex.getGunData().getAmmoId() == null) {
            return false;
        }

        // Refresh first so an installed PayDay ammo expansion is reflected immediately.
        PhoneUpgradeAction.refreshAmmoExpansion(player, gunStack);
        int capacity = PhoneUpgradeAction.getEffectiveAmmoCapacity(gunStack);
        if (capacity <= 0) {
            return false;
        }

        Item ammoItem = ModItems.AMMO.get();
        ItemStack ammoStack = new ItemStack(ammoItem);
        IAmmo ammo = IAmmo.getIAmmoOrNull(ammoStack);
        if (ammo == null) {
            return false;
        }
        ammo.setAmmoId(ammoStack, gunIndex.getGunData().getAmmoId());
        // TACZ 1.20.1 的参数顺序是 gunStack, ammoStack；传反会导致始终判定为不匹配。
        if (!ammo.isAmmoOfGun(gunStack, ammoStack)) {
            return false;
        }

        int amount = capacity * 2;
        int maxStackSize = Math.max(1, ammoStack.getMaxStackSize());
        while (amount > 0) {
            ItemStack supplied = ammoStack.copyWithCount(Math.min(amount, maxStackSize));
            int before = supplied.getCount();
            if (!player.addItem(supplied) && !supplied.isEmpty()) {
                player.drop(supplied, false);
            }
            amount -= before;
        }
        return true;
    }
}
