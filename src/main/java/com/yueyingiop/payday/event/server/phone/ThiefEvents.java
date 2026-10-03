package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.api.record.TemporaryBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/** 处理祝君安好的临时红石脉冲效果。 */
public final class ThiefEvents {
    private static final Map<ServerLevel, Map<BlockPos, TemporaryBlockState>> TEMPORARY_REDSTONE =
            new IdentityHashMap<>();

    private ThiefEvents() {
    }

    /**
     * 对目标方块直接施加临时红石状态。
     *
     * <p>这里只修改目标方块已有的红石属性，不会放置红石块；普通方块会返回
     * {@code false}，从而继续执行原本的右键交互。</p>
     */
    public static boolean emitTemporaryRedstoneSignal(ServerLevel level, BlockPos target) {
        BlockState targetState = level.getBlockState(target);
        BlockState poweredState = powerState(targetState);
        if (poweredState == null) {
            return false;
        }

        pulse(level, target, targetState, poweredState);
        if (targetState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos otherPos = targetState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    == DoubleBlockHalf.LOWER ? target.above() : target.below();
            BlockState otherState = level.getBlockState(otherPos);
            BlockState poweredOther = powerState(otherState);
            if (poweredOther != null) {
                pulse(level, otherPos, otherState, poweredOther);
            }
        }
        return true;
    }

    private static BlockState powerState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            BlockState powered = state.setValue(BlockStateProperties.POWERED, true);
            if (powered.hasProperty(BlockStateProperties.OPEN)) {
                powered = powered.setValue(BlockStateProperties.OPEN, true);
            }
            return powered;
        }
        if (state.hasProperty(BlockStateProperties.POWER)) {
            return state.setValue(BlockStateProperties.POWER, 15);
        }
        if (state.hasProperty(BlockStateProperties.LIT)) {
            return state.setValue(BlockStateProperties.LIT, true);
        }
        return null;
    }

    private static void pulse(ServerLevel level, BlockPos pos, BlockState original,
                              BlockState powered) {
        TEMPORARY_REDSTONE.computeIfAbsent(level, ignored -> new java.util.HashMap<>())
                .compute(pos.immutable(), (ignored, previous) -> new TemporaryBlockState(
                        previous == null ? original : previous.originalState(), powered,
                        level.getGameTime() + 20L));
        level.setBlockAndUpdate(pos, powered);
    }

    /** 每秒清理已经结束的临时红石状态。 */
    @SubscribeEvent
    public static void temporaryRedstoneTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            Map<BlockPos, TemporaryBlockState> signals = TEMPORARY_REDSTONE.get(level);
            if (signals == null) {
                continue;
            }

            Iterator<Map.Entry<BlockPos, TemporaryBlockState>> iterator = signals.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, TemporaryBlockState> entry = iterator.next();
                TemporaryBlockState pulse = entry.getValue();
                if (level.getGameTime() < pulse.expiresAt()) {
                    continue;
                }
                if (level.getBlockState(entry.getKey()).equals(pulse.poweredState())) {
                    level.setBlockAndUpdate(entry.getKey(), pulse.originalState());
                }
                iterator.remove();
            }

            if (signals.isEmpty()) {
                TEMPORARY_REDSTONE.remove(level);
            }
        }
    }
}
