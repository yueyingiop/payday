package com.yueyingiop.payday.api.turret;

import com.tacz.guns.sound.SoundManager;
import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.TurretTemperature;
import com.yueyingiop.payday.block.TurretBlock;
import com.yueyingiop.payday.block.TurretBlockEntity;
import com.yueyingiop.payday.entity.TurretEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 炮台的服务端攻击、客户端粒子和动画状态处理。 */
public final class TurretCombatHelper {
    private static final int ACTIVATION_ANIMATION_TICKS = 10;
    private static final ResourceLocation RPK_GUN_ID =
            ResourceLocation.fromNamespaceAndPath("tacz", "rpk");

    private TurretCombatHelper() {
    }

    /** 执行炮台服务端每 tick 的激活、过载、瞄准和攻击逻辑。 */
    public static void serverTick(TurretBlockEntity turret) {
        if (turret.getLevel() == null || turret.getLevel().isClientSide) {
            return;
        }
        if (!turret.isActivated()) {
            activate(turret);
            return;
        }
        TurretEntityBindingHelper.ensure(turret);
        if (turret.getActivationTicks() > 0) {
            int ticks = turret.getActivationTicks() - 1;
            turret.setActivationTicks(ticks);
            if (ticks == 0) {
                setBlockProperty(turret, TurretBlock.ACTIVATING, false);
            }
            turret.setChanged();
            return;
        }

        turret.tickFluids();
        if (turret.isOverloaded()) {
            turret.setTargetEntityId(-1);
            processOverloadLifetime(turret);
            return;
        }
        if (turret.getAttackCooldown() > 0) {
            turret.setAttackCooldown(turret.getAttackCooldown() - 1);
        }
        LivingEntity target = TurretAimHelper.findTarget(turret);
        turret.setTargetEntityId(target == null ? -1 : target.getId());
        if (target != null) {
            TurretAimHelper.updateAim(turret, target);
        }
        if (turret.getAttackCooldown() > 0 || target == null
                || !TurretAimHelper.isAimedAt(turret, target)
                || !target.hurt(turret.getLevel().damageSources().generic(), turret.getAttackDamage())) {
            return;
        }

        turret.setAttackCooldown(Config.TURRET_ATTACK_INTERVAL_TICKS.get());
        Vec3 muzzle = TurretAimHelper.muzzlePosition(turret);
        playShootSound(turret);
        spawnMuzzleFlash(turret, muzzle);
        TurretTemperature.tryAddAttackHeat(turret);
        turret.setChanged();
    }

    /** 执行炮台客户端的瞄准预览和过载粒子逻辑。 */
    public static void clientTick(TurretBlockEntity turret) {
        if (turret.getLevel() == null || !turret.getLevel().isClientSide) {
            return;
        }
        if (turret.isOverloaded()) {
            spawnOverloadSparks(turret);
            return;
        }
        if (!turret.isActivated() || turret.isActivating()) {
            return;
        }
        LivingEntity target = TurretAimHelper.findTarget(turret);
        if (target != null) {
            TurretAimHelper.updateAim(turret, target);
        }
    }

    /** 激活炮台并开始展开动画。 */
    public static void activate(TurretBlockEntity turret) {
        if (turret.getLevel() == null || turret.getLevel().isClientSide
                || turret.isActivated() || turret.isOverloaded()) {
            return;
        }
        turret.setActivatedInternal(true);
        turret.setActivationTicks(ACTIVATION_ANIMATION_TICKS);
        // 把首次开火错开到展开动画之后，保持后续攻击周期的相位差。
        turret.setAttackCooldown(ACTIVATION_ANIMATION_TICKS
                + TurretAimHelper.attackStagger(turret));
        setBlockProperty(turret, TurretBlock.ACTIVE, true);
        setBlockProperty(turret, TurretBlock.ACTIVATING, true);
        turret.setChanged();
    }

    /** 处理炮台过载后的自然销毁倒计时。 */
    private static void processOverloadLifetime(TurretBlockEntity turret) {
        if (turret.getOverloadedAt() == Long.MIN_VALUE) {
            turret.setOverloadedAt(turret.getLevel().getGameTime());
            turret.setChanged();
        } else if (turret.getLevel().getGameTime() - turret.getOverloadedAt()
                >= TurretTemperature.getOverloadLifetimeTicks()) {
            turret.getLevel().removeBlock(turret.getBlockPos(), false);
        }
    }

    /** 在炮口生成开火火焰粒子。 */
    private static void spawnMuzzleFlash(TurretBlockEntity turret, Vec3 muzzle) {
        if (!(turret.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 direction = TurretAimHelper.muzzleDirection(turret);
        serverLevel.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 6,
                direction.x * 0.08D, direction.y * 0.08D, direction.z * 0.08D, 0.02D);
    }

    /** 播放 TACZ RPK 射击音效。 */
    private static void playShootSound(TurretBlockEntity turret) {
        TurretEntity entity = TurretEntityBindingHelper.find(turret);
        if (entity != null) {
            SoundManager.sendSoundToNearby(entity, 64, RPK_GUN_ID, RPK_GUN_ID, "shoot",
                    0.8F, 0.9F + turret.getLevel().random.nextFloat() * 0.125F);
        }
    }

    /** 在过载炮台周围生成电火花、火焰和烟雾。 */
    private static void spawnOverloadSparks(TurretBlockEntity turret) {
        double x = turret.getBlockPos().getX() + 0.2D + turret.getLevel().random.nextDouble() * 0.6D;
        double y = turret.getBlockPos().getY() + 1.0D + turret.getLevel().random.nextDouble();
        double z = turret.getBlockPos().getZ() + 0.2D + turret.getLevel().random.nextDouble() * 0.6D;
        double vx = (turret.getLevel().random.nextDouble() - 0.5D) * 0.08D;
        double vy = turret.getLevel().random.nextDouble() * 0.08D;
        double vz = (turret.getLevel().random.nextDouble() - 0.5D) * 0.08D;
        if (turret.getLevel().random.nextInt(3) == 0) {
            turret.getLevel().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, vx, vy, vz);
        }
        if (turret.getLevel().random.nextInt(2) == 0) {
            turret.getLevel().addParticle(ParticleTypes.FLAME, x, y, z, vx * 0.5D, vy, vz * 0.5D);
        }
        if (turret.getLevel().random.nextInt(3) == 0) {
            turret.getLevel().addParticle(ParticleTypes.SMOKE, x, y + 0.15D, z,
                    vx * 0.35D, vy * 0.6D + 0.02D, vz * 0.35D);
        }
    }

    /** 修改炮台方块状态并同步客户端。 */
    private static void setBlockProperty(TurretBlockEntity turret,
                                         net.minecraft.world.level.block.state.properties.BooleanProperty property,
                                         boolean value) {
        if (turret.getBlockState().getValue(property) != value) {
            turret.getLevel().setBlock(turret.getBlockPos(), turret.getBlockState().setValue(property, value),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }
}
