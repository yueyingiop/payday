package com.yueyingiop.payday.menu;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.item.PhoneItem;
import com.yueyingiop.payday.item.PhoneUpgradeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Menu for the phone's 18 basic and 9 special upgrade slots. */
public class PhoneMenu extends AbstractContainerMenu {
    public static final int BASIC_SLOTS = 18;
    public static final int SPECIAL_SLOTS = 9;
    public static final int TOTAL_SLOTS = BASIC_SLOTS + SPECIAL_SLOTS;
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, PayDay.MODID);
    public static final RegistryObject<MenuType<PhoneMenu>> TYPE = MENUS.register("phone_menu",
            () -> IForgeMenuType.create((windowId, inventory, data) -> new PhoneMenu(
                    windowId, inventory, data.readItem(), data.readVarInt())));

    private final SimpleContainer upgrades = new SimpleContainer(TOTAL_SLOTS);
    private final ItemStack phoneStack;
    private final int phoneSlotIndex;

    public static void openFor(ServerPlayer player) {
        CurioHelper.SlotStack phoneSlot = CurioHelper.slots(player, CurioHelper.PHONE_SLOT).stream()
                .filter(slot -> slot.stack().getItem() instanceof PhoneItem)
                .findFirst().orElse(null);
        if (phoneSlot == null) return;

        ItemStack phone = phoneSlot.stack();
        NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("menu.payday.phone");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new PhoneMenu(id, inventory, phone, phoneSlot.index());
            }
        }, buffer -> {
            buffer.writeItem(phone);
            buffer.writeVarInt(phoneSlot.index());
        });
    }

    public PhoneMenu(int id, Inventory inventory, ItemStack phone) {
        this(id, inventory, phone, 0);
    }

    public PhoneMenu(int id, Inventory inventory, ItemStack phone, int phoneSlotIndex) {
        super(TYPE.get(), id);
        this.phoneStack = phone;
        this.phoneSlotIndex = phoneSlotIndex;
        if (phone.getItem() instanceof PhoneItem phoneItem) {
            List<ItemStack> installed = phoneItem.getUpgradeStacks(phone);
            for (int i = 0; i < Math.min(TOTAL_SLOTS, installed.size()); i++) {
                upgrades.setItem(i, installed.get(i));
            }
        }
        for (int i = 0; i < BASIC_SLOTS; i++) {
            addSlot(new PhoneUpgradeSlot(upgrades, i, basicX(i), basicY(i), PhoneUpgradeItem.Category.BASIC));
        }
        for (int i = 0; i < SPECIAL_SLOTS; i++) {
            addSlot(new PhoneUpgradeSlot(upgrades, BASIC_SLOTS + i,
                    specialX(i), specialY(i), PhoneUpgradeItem.Category.SPECIAL));
        }
        addPlayerInventory(inventory);
    }

    public int basicX(int index) {
        int[] xs = {32, 63, 94, 125, 156, 187,
                24, 55, 86, 117, 148, 179,
                16, 48, 79, 110, 141, 172};
        return xs[index];
    }

    public int basicY(int index) {
        return index < 6 ? 45 : index < 12 ? 71 : 97;
    }

    public int specialX(int index) { return 233 + (index % 3) * 25; }
    public int specialY(int index) { return 46 + (index / 3) * 25; }
    public ItemStack phoneDisplay() { return phoneStack; }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                // 背包物品图标相对背景向上移动1px。
                addSlot(new Slot(inventory, col + row * 9 + 9, 76 + col * 18, 130 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 76 + col * 18, 188));
        }
    }

    private boolean hasPrerequisite(PhoneUpgradeItem upgrade, int slotIndex) {
        if (upgrade.category() == PhoneUpgradeItem.Category.SPECIAL) {
            for (int i = BASIC_SLOTS; i < TOTAL_SLOTS; i++) {
                ItemStack installed = upgrades.getItem(i);
                if (installed.getItem() == upgrade) return false;
            }
        }
        if (!upgrade.requiresPrerequisite()) return true;
        for (int i = 0; i < BASIC_SLOTS; i++) {
            ItemStack installed = upgrades.getItem(i);
            if (installed.getItem() instanceof PhoneUpgradeItem other
                    && other.series().equals(upgrade.series())
                    && other.level() == upgrade.level() - 1) return true;
        }
        for (int i = BASIC_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack installed = upgrades.getItem(i);
            if (installed.getItem() instanceof PhoneUpgradeItem other
                    && other.series().equals(upgrade.series())
                    && other.level() == upgrade.level() - 1) return true;
        }
        return false;
    }

    private Component placementFailure(ItemStack stack, int slotIndex) {
        if (!(stack.getItem() instanceof PhoneUpgradeItem upgrade)) return null;
        if (slotIndex < 0 || slotIndex >= TOTAL_SLOTS) return null;
        PhoneUpgradeItem.Category category = slotIdCategory(slotIndex);
        if (upgrade.category() != category) {
            return Component.translatable("message.payday.phone_wrong_category");
        }
        if (upgrade.category() == PhoneUpgradeItem.Category.SPECIAL) {
            for (int i = BASIC_SLOTS; i < TOTAL_SLOTS; i++) {
                if (upgrades.getItem(i).getItem() == upgrade) {
                    return Component.translatable("message.payday.phone_duplicate_upgrade");
                }
            }
        }
        if (upgrade.requiresPrerequisite() && !hasPrerequisite(upgrade, slotIndex)) {
            return Component.translatable("message.payday.upgrade_prerequisite");
        }
        return null;
    }

    private PhoneUpgradeItem.Category slotIdCategory(int slotId) {
        return slotId < BASIC_SLOTS ? PhoneUpgradeItem.Category.BASIC : PhoneUpgradeItem.Category.SPECIAL;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        ItemStack copy = source.getItem().copy();
        int count = copy.getCount();
        if (index < TOTAL_SLOTS) {
            ItemStack removed = source.getItem().copy();
            if (!moveItemStackTo(copy, TOTAL_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            if (removed.getItem() instanceof PhoneUpgradeItem upgrade) {
                removeHigherLevelUpgrades(player, upgrade);
            }
        } else if (copy.getItem() instanceof PhoneUpgradeItem) {
            if (!moveItemStackTo(copy, 0, TOTAL_SLOTS, false)) {
                Component failure = installationFailure((PhoneUpgradeItem) copy.getItem());
                if (!player.level().isClientSide && failure != null) {
                    player.displayClientMessage(failure, true);
                }
                return ItemStack.EMPTY;
            }
        } else return ItemStack.EMPTY;
        source.getItem().shrink(count - copy.getCount());
        source.setChanged();
        syncPhoneState(player);
        return source.getItem().copy();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!player.level().isClientSide && slotId >= 0 && slotId < TOTAL_SLOTS
                && !getCarried().isEmpty()) {
            Component failure = placementFailure(getCarried(), slotId);
            if (failure != null) {
                player.displayClientMessage(failure, true);
                return;
            }
        }
        ItemStack before = slotId >= 0 && slotId < TOTAL_SLOTS
                ? slots.get(slotId).getItem().copy() : ItemStack.EMPTY;
        super.clicked(slotId, button, clickType, player);
        if (before.getItem() instanceof PhoneUpgradeItem upgrade
                && slotId >= 0 && slotId < TOTAL_SLOTS
                && (slots.get(slotId).getItem().isEmpty()
                || slots.get(slotId).getItem().getItem() != before.getItem())) {
            removeHigherLevelUpgrades(player, upgrade);
        }
        syncPhoneState(player);
    }

    /** 将菜单中的升级列表立即写回真实手机，并刷新手机提供的属性效果。 */
    private void syncPhoneState(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        ItemStack equippedPhone = CurioHelper.stackAt(player, CurioHelper.PHONE_SLOT, phoneSlotIndex);
        if (!(equippedPhone.getItem() instanceof PhoneItem phone)) {
            return;
        }

        List<ItemStack> list = new ArrayList<>(TOTAL_SLOTS);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            list.add(upgrades.getItem(i).copy());
        }
        phone.setUpgradeStacks(equippedPhone, list);
        PhoneItem.refreshEffects(player);
    }

    private void removeHigherLevelUpgrades(Player player, PhoneUpgradeItem removedUpgrade) {
        if (removedUpgrade.category() != PhoneUpgradeItem.Category.SPECIAL) return;
        for (int i = BASIC_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack installed = upgrades.getItem(i);
            if (installed.getItem() instanceof PhoneUpgradeItem upgrade
                    && upgrade.category() == PhoneUpgradeItem.Category.SPECIAL
                    && upgrade.series().equals(removedUpgrade.series())
                    && upgrade.level() > removedUpgrade.level()) {
                ItemStack returned = installed.copy();
                upgrades.setItem(i, ItemStack.EMPTY);
                if (!player.getInventory().add(returned)) player.drop(returned, false);
            }
        }
    }

    private Component installationFailure(PhoneUpgradeItem upgrade) {
        if (upgrade.category() == PhoneUpgradeItem.Category.SPECIAL) {
            for (int i = BASIC_SLOTS; i < TOTAL_SLOTS; i++) {
                if (upgrades.getItem(i).getItem() == upgrade) {
                    return Component.translatable("message.payday.phone_duplicate_upgrade");
                }
            }
        }
        if (upgrade.requiresPrerequisite() && !hasPrerequisite(upgrade, -1)) {
            return Component.translatable("message.payday.upgrade_prerequisite");
        }
        return Component.translatable("message.payday.phone_full");
    }

    @Override public boolean stillValid(Player player) {
        return player.isAlive() && phoneStack.getItem() instanceof PhoneItem;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        // 覆盖最后一次容器变化和异常关闭路径。
        syncPhoneState(player);
    }

    private final class PhoneUpgradeSlot extends Slot {
        private final PhoneUpgradeItem.Category category;
        private PhoneUpgradeSlot(Container container, int index, int x, int y,
                                 PhoneUpgradeItem.Category category) {
            super(container, index, x, y);
            this.category = category;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof PhoneUpgradeItem upgrade
                    && upgrade.category() == category
                    && hasPrerequisite(upgrade, getContainerSlot());
        }

        @Override public int getMaxStackSize() { return 1; }
    }
}
