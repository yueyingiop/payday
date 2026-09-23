package com.yueyingiop.payday.block;

import com.yueyingiop.payday.init.RegistryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class SupplyBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SupplyBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryBlockEntities.SUPPLY.get(), pos, state);
    }

    /**
     * 获取补给类型。
     * @return 对应的执行结果。
     */
    public SupplyBlock.SupplyType supplyType() {
        return ((SupplyBlock) getBlockState().getBlock()).supplyType();
    }

    /**
     * 获取数量。
     * @return 对应的执行结果。
     */
    public int amount() {
        return getBlockState().getValue(SupplyBlock.AMOUNT);
    }

    /**
     * 注册补给方块实体的动画控制器。
     * @param controllers 动画控制器。
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    /**
     * 获取补给方块实体的动画实例缓存。
     * @return 对应的执行结果。
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
