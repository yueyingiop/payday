package com.yueyingiop.payday.entity;

import com.yueyingiop.payday.api.TurretTemperature;
import com.yueyingiop.payday.block.TurretBlock;
import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 炮台的可受击生物代理。模型仍由炮台方块实体渲染，受击和 Jade 目标由该实体负责。 */
public final class TurretEntity extends PathfinderMob {
    private static final String TURRET_POS_TAG = "TurretPos";

    private BlockPos turretPos = BlockPos.ZERO;

    public TurretEntity(EntityType<? extends TurretEntity> entityType, Level level) {
        super(entityType, level);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** 将代理实体绑定到炮台方块。 */
    public void bindTo(BlockPos pos) {
        turretPos = pos.immutable();
        setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    public BlockPos getTurretPos() {
        return turretPos;
    }

    public TurretBlockEntity getTurret() {
        return level().getBlockEntity(turretPos) instanceof TurretBlockEntity turret
                ? turret : null;
    }

    public double getTemperature() {
        TurretBlockEntity turret = getTurret();
        return turret == null ? TurretTemperature.INITIAL_TEMPERATURE : turret.getTemperature();
    }

    public double getMaximumTemperature() {
        TurretBlockEntity turret = getTurret();
        return turret == null ? TurretTemperature.getMaximumTemperature()
                : turret.getMaximumTemperature();
    }

    @Override
    protected void registerGoals() {
        // 炮台自身不移动，也不需要 AI；敌对生物会被 tick 设置为攻击目标。
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        BlockState state = level().getBlockState(turretPos);
        if (!(state.getBlock() instanceof TurretBlock)
                || !(level().getBlockEntity(turretPos) instanceof TurretBlockEntity turret)) {
            discard();
            return;
        }

        setPos(turretPos.getX() + 0.5D, turretPos.getY(), turretPos.getZ() + 0.5D);
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (!turret.isOverloaded()) {
            prioritizeHostileTargets();
        } else {
            clearHostileTargets();
        }
    }

    /** 将炮台设置为附近敌对生物的最高优先级目标。 */
    private void prioritizeHostileTargets() {
        AABB area = new AABB(turretPos).inflate(TurretBlockEntity.getAttackRange());
        for (Mob mob : level().getEntitiesOfClass(Mob.class, area,
                candidate -> candidate instanceof Enemy
                        && candidate.isAlive()
                        && !candidate.isInvulnerable())) {
            if (mob.getTarget() != this) {
                mob.setTarget(this);
            }
        }
    }

    /** 过载时清除已经锁定本炮台的敌对生物目标。 */
    public void clearHostileTargets() {
        if (level().isClientSide) {
            return;
        }

        AABB area = getBoundingBox().inflate(TurretBlockEntity.getAttackRange());
        for (Mob mob : level().getEntitiesOfClass(Mob.class, area,
                candidate -> candidate instanceof Enemy
                        && candidate.isAlive()
                        && candidate.getTarget() == this)) {
            mob.setTarget(null);
        }
    }

    public boolean isOverloaded() {
        TurretBlockEntity turret = getTurret();
        return turret == null || turret.isOverloaded();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return true;
        }

        TurretBlockEntity turret = getTurret();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living
                ? living : null;
        if (turret != null && attacker != null
                && TurretTemperature.receiveDamage(turret, attacker, amount)) {
            turret.setChanged();
        }

        // 炮台的生命值不因普通攻击扣除，温度系统代替生命值承受敌对攻击。
        return attacker != null;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitPosition, InteractionHand hand) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            return TurretBlock.pickup(level(), turretPos, player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(com.yueyingiop.payday.init.RegistryItems.TURRET.get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong(TURRET_POS_TAG, turretPos.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TURRET_POS_TAG)) {
            turretPos = BlockPos.of(tag.getLong(TURRET_POS_TAG));
        }
    }
}
