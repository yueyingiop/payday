package com.yueyingiop.payday.block;

import com.yueyingiop.payday.event.server.SupplyEvents;
import com.yueyingiop.payday.init.RegistryBlocks;
import com.yueyingiop.payday.item.ArmorPlateItem;
import com.yueyingiop.payday.item.FirstAidKitItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

@SuppressWarnings("deprecation")
public final class SupplyBlock extends BaseEntityBlock {
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, 8);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 8, 15);
    private final SupplyType type;

    public SupplyBlock(SupplyType type) {
        super(BlockBehaviour.Properties.of().noOcclusion().strength(0.25F).sound(SoundType.WOOL));
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(AMOUNT, 1));
    }

    /**
     * 获取补给类型。
     * @return 对应的执行结果。
     */
    public SupplyType supplyType() {
        return type;
    }

    /**
     * 放置对应物品。
     * @param level 世界。
     * @param pos 方块坐标。
     * @param stack 物品堆。
     * @param type 类型。
     * @param player 玩家。
     * @return 对应的执行结果。
     */
    public static InteractionResult place(Level level, BlockPos pos, ItemStack stack, SupplyType type, Player player) {
        BlockState existing = level.getBlockState(pos);
        if (existing.getBlock() instanceof SupplyBlock supplyBlock && supplyBlock.supplyType() == type) {
            int current = existing.getValue(AMOUNT);
            int added = Math.min(8 - current, stack.getCount());
            if (added <= 0) {
                return InteractionResult.FAIL;
            }

            level.setBlock(pos, existing.setValue(AMOUNT, current + added), Block.UPDATE_ALL);
            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(added);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        Block block = type == SupplyType.ARMOR_PLATE
                ? RegistryBlocks.ARMOR_PLATE.get()
                : RegistryBlocks.FIRSTAID_KIT.get();
        int count = Math.min(8, Math.max(1, stack.getCount()));
        level.setBlock(pos, block.defaultBlockState().setValue(AMOUNT, count), Block.UPDATE_ALL);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(count);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 按照玩家当前视线，将补给方块放置在命中的方块表面，或合并到命中的同类补给方块中。
     *
     * @param player 玩家
     * @param stack 要放置的补给物品
     * @param type 补给类型
     * @return 是否成功执行放置
     */
    public static boolean placeAtLook(Player player, ItemStack stack, SupplyType type) {
        Level level = player.level();
        HitResult hit = player.pick(player.getBlockReach(), 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return false;
        }

        BlockPos clickedPos = blockHit.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        boolean matchingSupply = clickedState.getBlock() instanceof SupplyBlock block
                && block.supplyType() == type;
        BlockPos targetPos = matchingSupply
                ? clickedPos
                : clickedPos.relative(blockHit.getDirection());

        if (!matchingSupply && !level.getBlockState(targetPos).canBeReplaced()) {
            return false;
        }
        return place(level, targetPos, stack, type, player).consumesAction();
    }

    /**
     * 定义补给方块的状态属性。
     * @param builder 状态构建器。
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMOUNT);
    }

    /**
     * 获取补给方块的渲染形状。
     * @param state 方块状态。
     * @return 对应的执行结果。
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * 获取补给方块的碰撞形状。
     * @param state 方块状态。
     * @param level 世界。
     * @param pos 方块坐标。
     * @param context 上下文。
     * @return 对应的执行结果。
     */
    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    /**
     * 创建补给方块实体。
     * @param pos 方块坐标。
     * @param state 方块状态。
     * @return 对应的执行结果。
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyBlockEntity(pos, state);
    }

    /**
     * 获取补给方块实体的刻更新器。
     * @param level 世界。
     * @param state 方块状态。
     * @param blockEntityType 对应参数。
     * @return 对应的执行结果。
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        return null;
    }

    /**
     * 处理使用操作。
     * @param state 方块状态。
     * @param level 世界。
     * @param pos 方块坐标。
     * @param player 玩家。
     * @param hand 对应参数。
     * @param hit 对应参数。
     * @return 对应的执行结果。
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        if (!(level.getBlockEntity(pos) instanceof SupplyBlockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isMatchingSupply(held)) {
            return place(level, pos, held, type, player);
        }

        if (player.isShiftKeyDown()) {
            List<ItemStack> stacks = splitStacks(state.getValue(AMOUNT));
            level.removeBlock(pos, false);
            for (ItemStack stack : stacks) {
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
            }
            return InteractionResult.CONSUME;
        }

        boolean used = type == SupplyType.ARMOR_PLATE
                ? SupplyEvents.repairArmor(player)
                : SupplyEvents.healPlayer(player);
        if (!used) {
            return InteractionResult.PASS;
        }

        int amount = state.getValue(AMOUNT);
        if (amount <= 1) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(AMOUNT, amount - 1), Block.UPDATE_ALL);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * 判断物品是否属于当前补给类型。
     * @param stack 物品堆。
     * @return 对应的执行结果。
     */
    private boolean isMatchingSupply(ItemStack stack) {
        return type == SupplyType.ARMOR_PLATE
                ? stack.getItem() instanceof ArmorPlateItem
                : stack.getItem() instanceof FirstAidKitItem;
    }

    /**
     * 根据剩余补给数量生成方块掉落物。
     * @param state 方块状态。
     * @param builder 状态构建器。
     * @return 对应的执行结果。
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return splitStacks(state.getValue(AMOUNT));
    }

    /** 按补给物品的最大堆叠数拆分方块中的数量，避免拾取后得到非法的大堆叠。 */
    private List<ItemStack> splitStacks(int amount) {
        net.minecraft.world.item.Item item = type == SupplyType.ARMOR_PLATE
                ? com.yueyingiop.payday.init.RegistryItems.ARMOR_PLATE.get()
                : com.yueyingiop.payday.init.RegistryItems.FIRSTAID_KIT.get();
        List<ItemStack> result = new java.util.ArrayList<>();
        int remaining = Math.max(0, amount);
        while (remaining > 0) {
            int count = Math.min(item.getMaxStackSize(), remaining);
            result.add(new ItemStack(item, count));
            remaining -= count;
        }
        return result;
    }

    public enum SupplyType {
        ARMOR_PLATE,
        FIRST_AID
    }
}
