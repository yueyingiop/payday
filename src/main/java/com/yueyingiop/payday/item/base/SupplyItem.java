package com.yueyingiop.payday.item.base;

import com.yueyingiop.payday.block.SupplyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Shared placement behavior for the two stackable supply items. */
public abstract class SupplyItem extends Item {
    protected SupplyItem() {
        super(new Item.Properties().stacksTo(4));
    }

    /**
     * 获取补给类型。
     * @return 对应的执行结果。
     */
    protected abstract SupplyBlock.SupplyType supplyType();

    /**
     * 处理补给物品对方块的使用。
     * @param context 上下文。
     * @return 对应的执行结果。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        if (!(context.getLevel().getBlockState(pos).getBlock() instanceof SupplyBlock block)
                || block.supplyType() != supplyType()) {
            pos = pos.relative(context.getClickedFace());
        }
        return SupplyBlock.place(context.getLevel(), pos, context.getItemInHand(), supplyType(), context.getPlayer());
    }

    /**
     * 处理使用操作。
     * @param level 世界。
     * @param player 玩家。
     * @param hand 对应参数。
     * @return 对应的执行结果。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = player.blockPosition().relative(player.getDirection());
        boolean matchingSupply = level.getBlockState(pos).getBlock() instanceof SupplyBlock block
                && block.supplyType() == supplyType();
        if (!matchingSupply && !level.getBlockState(pos).canBeReplaced()) {
            pos = pos.above();
        }
        InteractionResult result = SupplyBlock.place(level, pos, stack, supplyType(), player);
        return new InteractionResultHolder<>(result, stack);
    }
}
