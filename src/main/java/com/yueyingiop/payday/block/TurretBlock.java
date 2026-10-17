package com.yueyingiop.payday.block;

import com.yueyingiop.payday.init.RegistryItems;
import com.yueyingiop.payday.menu.TurretMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** 自动攻击附近敌对生物的炮台方块。 */
@SuppressWarnings("deprecation")
public final class TurretBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty OVERLOADED = BooleanProperty.create("overloaded");
    public static final BooleanProperty ACTIVATING = BooleanProperty.create("activating");
    public static final BooleanProperty FIRING = BooleanProperty.create("firing");
    /** 每次开火翻转一次，用于让客户端能够区分连续开火动画。 */
    public static final BooleanProperty FIRE_TOGGLE = BooleanProperty.create("fire_toggle");

    public TurretBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
                .setValue(OVERLOADED, false)
                .setValue(ACTIVATING, false)
                .setValue(FIRING, false)
                .setValue(FIRE_TOGGLE, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, ACTIVE, OVERLOADED, ACTIVATING, FIRING, FIRE_TOGGLE);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(
            BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return Block.box(0, 0, 0, 16, 32, 16);
    }

    /** 炮台不能通过普通挖掘破坏，只能使用 Shift+空手右键收起。 */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level,
                                    BlockPos pos) {
        return 0.0F;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            return pickup(level, pos, player);
        }

        if (!level.isClientSide && !player.getItemInHand(hand).isEmpty()
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TurretMenu.openFor(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 收起炮台并返还炮台物品。 */
    public static InteractionResult pickup(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockState(pos).getBlock() instanceof TurretBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        ItemStack turretItem = new ItemStack(RegistryItems.TURRET.get());
        if (level.getBlockEntity(pos) instanceof TurretBlockEntity turret) {
            turret.clearFluids();
        }

        level.removeBlock(pos, false);
        if (!player.getAbilities().instabuild) {
            if (!player.addItem(turretItem)) {
                player.drop(turretItem, false);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof TurretBlockEntity turret) {
            turret.clearFluids();
            turret.removeTurretEntity();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 玩家左键不会给炮台增加温度；所有状态都只能使用 Shift+空手右键收起。 */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(OVERLOADED)) {
            super.attack(state, level, pos, player);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof TurretBlockEntity turret) {
                if (tickerLevel.isClientSide) {
                    turret.clientTick();
                } else {
                    turret.serverTick();
                }
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurretBlockEntity turret) {
            turret.setBaseYaw(placer.getYRot());
            if (!turret.isOverloaded()) {
                turret.activate();
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(RegistryItems.TURRET.get()));
    }
}
