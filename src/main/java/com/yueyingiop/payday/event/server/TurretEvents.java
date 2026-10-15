package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.block.TurretBlock;
import com.yueyingiop.payday.block.TurretBlockEntity;
import com.yueyingiop.payday.entity.TurretEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 炮台的玩家交互限制和敌对生物目标选择。 */
public final class TurretEvents {
    private TurretEvents() {
    }

    /** 炮台任何状态都不能通过普通挖掘破坏，只能 Shift+空手右键收起。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventTurretMining(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof TurretBlock) {
            event.setCanceled(true);
        }
    }

    /** 在服务器最终处理方块破坏时再次拦截，防止任何挖掘路径拆下炮台。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventTurretBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof TurretBlock) {
            event.setCanceled(true);
        }
    }

    /** 将攻击范围内未过载的炮台作为敌对生物的最高优先级目标。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void prioritizeTurretTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob instanceof Enemy)
                || mob.level().isClientSide) {
            return;
        }

        double attackRange = TurretBlockEntity.getAttackRange();
        AABB searchArea = mob.getBoundingBox().inflate(attackRange);
        TurretEntity turret = mob.level().getEntitiesOfClass(TurretEntity.class, searchArea,
                        candidate -> candidate.isAlive()
                                && !candidate.isOverloaded()
                                && mob.distanceToSqr(candidate)
                                <= attackRange * attackRange)
                .stream()
                .min((left, right) -> Double.compare(
                        mob.distanceToSqr(left), mob.distanceToSqr(right)))
                .orElse(null);
        if (turret != null) {
            event.setNewTarget(turret);
        }
    }
}
