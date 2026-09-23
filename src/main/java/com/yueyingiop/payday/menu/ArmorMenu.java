package com.yueyingiop.payday.menu;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.Optional;

public class ArmorMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, PayDay.MODID);
    public static final RegistryObject<MenuType<ArmorMenu>> TYPE = MENUS.register("armor_menu", () -> IForgeMenuType.create((windowId, inventory, data) -> new ArmorMenu(windowId, inventory, data.readItem())));
    private final SimpleContainer upgrades;
    private final ItemStack armorStack;

    /**
     * 查找玩家护甲并打开升级界面。
     * @param player 玩家。
     */
    public static void openFor(ServerPlayer player) {
        ItemStack armor = findArmor(player).orElse(ItemStack.EMPTY);
        if (armor.isEmpty()) return;
        NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override public net.minecraft.network.chat.Component getDisplayName() { return net.minecraft.network.chat.Component.translatable("menu.payday.armor"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ArmorMenu(id, inventory, armor); }
        }, buffer -> buffer.writeItem(armor));
    }

    public ArmorMenu(int id, Inventory inventory, ItemStack armor) {
        super(TYPE.get(), id);
        this.armorStack = armor;
        int size = armor.getItem() instanceof ArmorCurioItem curio ? curio.upgradeSlots() : 0;
        this.upgrades = new SimpleContainer(size);
        if (armor.getItem() instanceof ArmorCurioItem curio) {
            var list = curio.getUpgradeStacks(armor);
            for (int i = 0; i < Math.min(size, list.size()); i++) upgrades.setItem(i, list.get(i));
        }
        for (int i = 0; i < size; i++) addSlot(new UpgradeSlot(upgrades, i, slotX(i), slotY(i)));
        addPlayerInventory(inventory);
    }

    /**
     * 获取当前界面的升级槽位数量。
     * @return 对应的执行结果。
     */
    public int upgradeSize() { return upgrades.getContainerSize(); }
    /**
     * 计算指定升级槽位的横坐标。
     * @param index 索引。
     * @return 对应的执行结果。
     */
    public int slotX(int index) { return 106 + (index % 6) * 16; }
    /**
     * 计算指定升级槽位的纵坐标。
     * @param index 索引。
     * @return 对应的执行结果。
     */
    public int slotY(int index) { return 18 + (index / 6) * 16; }
    /**
     * 获取界面展示的护甲物品。
     * @return 对应的执行结果。
     */
    public ItemStack armorDisplay() { return armorStack; }
    /**
     * 获取护甲物品的模型标识。
     * @return 对应的执行结果。
     */
    public ResourceLocation armorModelId() {
        return armorStack.getItem() instanceof ArmorCurioItem
                ? ForgeRegistries.ITEMS.getKey(armorStack.getItem()) : null;
    }

    /**
     * 向菜单添加玩家背包及快捷栏槽位。
     * @param inventory 玩家背包。
     */
    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 25 + col * 18, 99 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 25 + col * 18, 157));
    }

    /**
     * 查找玩家护甲。
     * @param player 玩家。
     * @return 对应的执行结果。
     */
    private static Optional<ItemStack> findArmor(ServerPlayer player) {
        return CurioHelper.stacks(player, CurioHelper.ARMOR_SLOT).stream()
                .filter(stack -> stack.getItem() instanceof ArmorCurioItem)
                .findFirst();
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (source == null || !source.hasItem()) return empty;
        ItemStack copy = source.getItem().copy();
        int amount = copy.getCount();
        if (index < upgrades.getContainerSize()) {
            ArmorSlotItem.clearUpgradeIndex(copy);
            if (!moveItemStackTo(copy, upgrades.getContainerSize(), slots.size(), true)) return empty;
        } else if (copy.getItem() instanceof ArmorSlotItem) {
            if (!moveItemStackTo(copy, 0, upgrades.getContainerSize(), false)) return empty;
        } else return empty;
        source.getItem().shrink(amount - copy.getCount());
        source.setChanged();
        return source.getItem().copy();
    }

    @Override public boolean stillValid(Player player) { return player.isAlive() && armorStack.getItem() instanceof ArmorCurioItem; }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && armorStack.getItem() instanceof ArmorCurioItem curio) {
            java.util.List<ItemStack> list = new java.util.ArrayList<>();
            for (int i = 0; i < upgrades.getContainerSize(); i++) if (!upgrades.getItem(i).isEmpty()) list.add(upgrades.getItem(i));
            curio.setUpgradeStacks(armorStack, list);
            com.yueyingiop.payday.event.server.ArmorCurioEvents.refreshArmorAttributes(player);
        }
    }

    private static final class UpgradeSlot extends Slot {
        private final int upgradeIndex;

        private UpgradeSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.upgradeIndex = index;
        }

        @Override public void set(ItemStack stack) {
            if (stack.getItem() instanceof ArmorSlotItem) {
                ArmorSlotItem.setUpgradeIndex(stack, upgradeIndex);
            }
            super.set(stack);
        }

        @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ArmorSlotItem; }
        @Override public int getMaxStackSize() { return 1; }

        @Override public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            ArmorSlotItem.clearUpgradeIndex(stack);
        }
    }
}
