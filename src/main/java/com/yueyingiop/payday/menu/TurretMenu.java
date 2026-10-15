package com.yueyingiop.payday.menu;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 炮台流体装载界面菜单。 */
public final class TurretMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, PayDay.MODID);
    public static final RegistryObject<MenuType<TurretMenu>> TYPE = MENUS.register("turret_menu",
            () -> IForgeMenuType.create((windowId, inventory, data) ->
                    new TurretMenu(windowId, inventory, data.readBlockPos())));

    private final BlockPos turretPos;
    private final Inventory playerInventory;
    private final SimpleContainer displaySlots = new SimpleContainer(2);
    private boolean processingFluidSlot;

    /** 在玩家右键炮台时打开流体界面。 */
    public static void openFor(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof TurretBlockEntity)) {
            return;
        }
        NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("menu.payday.turret");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new TurretMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }

    public TurretMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public TurretMenu(int id, Inventory inventory, BlockPos turretPos) {
        super(TYPE.get(), id);
        this.turretPos = turretPos.immutable();
        this.playerInventory = inventory;
        displaySlots.addListener(this::slotsChanged);
        addSlot(new FluidContainerSlot(displaySlots, 0, 11, 73));
        addSlot(new FluidContainerSlot(displaySlots, 1, 37, 73));
        addPlayerInventory(inventory);
    }

    /** 获取炮台位置。 */
    public BlockPos turretPos() {
        return turretPos;
    }

    /** 获取当前客户端或服务端炮台。 */
    public TurretBlockEntity turret() {
        return playerInventory.player.level().getBlockEntity(turretPos) instanceof TurretBlockEntity turret
                ? turret : null;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
        if (!player.level().isClientSide) {
            processFluidSlots();
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == displaySlots && !processingFluidSlot
                && !playerInventory.player.level().isClientSide) {
            processFluidSlots();
        }
    }

    /** 处理 GUI 中放入的流体容器，只接受能够完整倒入的一整桶流体。 */
    private void processFluidSlots() {
        if (processingFluidSlot || playerInventory.player.level().isClientSide) {
            return;
        }
        processingFluidSlot = true;
        try {
            TurretBlockEntity turret = turret();
            if (turret == null) {
                return;
            }
            for (int i = 0; i < displaySlots.getContainerSize(); i++) {
                ItemStack containerStack = displaySlots.getItem(i);
                if (containerStack.isEmpty()) {
                    continue;
                }
                IFluidHandler handler = turret.getFluidHandler(i);
                if (handler == null) {
                    continue;
                }
                var contained = FluidUtil.getFluidContained(containerStack);
                if (contained.isEmpty()) {
                    continue;
                }
                FluidStack fluid = contained.get();
                if (fluid.getAmount() != 1000
                        || handler.fill(fluid, IFluidHandler.FluidAction.SIMULATE) != fluid.getAmount()) {
                    continue;
                }

                ItemStack singleContainer = containerStack.copyWithCount(1);
                FluidActionResult simulated = FluidUtil.tryEmptyContainer(
                        singleContainer, handler, fluid.getAmount(), null, false);
                if (!simulated.isSuccess()) {
                    continue;
                }
                FluidActionResult result = FluidUtil.tryEmptyContainer(
                        singleContainer, handler, fluid.getAmount(), playerInventory.player, true);
                if (!result.isSuccess()) {
                    continue;
                }

                containerStack.shrink(1);
                displaySlots.setItem(i, containerStack);
                ItemStack emptyContainer = result.getResult();
                if (!emptyContainer.isEmpty() && !playerInventory.player.getInventory().add(emptyContainer)) {
                    playerInventory.player.drop(emptyContainer, false);
                }
            }
        } finally {
            processingFluidSlot = false;
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 107 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 165));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = sourceSlot.getItem();
        ItemStack result = source.copy();
        if (index < 2) {
            if (!moveItemStackTo(source, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, 2, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }
        for (int i = 0; i < displaySlots.getContainerSize(); i++) {
            ItemStack remaining = displaySlots.removeItemNoUpdate(i);
            if (!remaining.isEmpty() && !player.getInventory().add(remaining)) {
                player.drop(remaining, false);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.distanceToSqr(
                turretPos.getX() + 0.5D, turretPos.getY() + 0.5D,
                turretPos.getZ() + 0.5D) <= 64.0D
                && player.level().getBlockEntity(turretPos) instanceof TurretBlockEntity;
    }

    private final class FluidContainerSlot extends Slot {
        private final int tank;

        private FluidContainerSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.tank = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            return FluidUtil.getFluidContained(stack)
                    .filter(fluid -> fluid.getAmount() == 1000)
                    .map(fluid -> {
                        IFluidHandler handler = turret() == null ? null : turret().getFluidHandler(tank);
                        return handler != null
                                && handler.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount();
                    })
                    .orElse(false);
        }

        @Override
        public int getMaxStackSize() { return 1; }
    }
}
