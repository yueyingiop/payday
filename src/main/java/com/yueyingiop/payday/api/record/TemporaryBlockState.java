package com.yueyingiop.payday.api.record;

import net.minecraft.world.level.block.state.BlockState;

// 临时方块状态
public record TemporaryBlockState(BlockState originalState, BlockState poweredState, long expiresAt) {

}
