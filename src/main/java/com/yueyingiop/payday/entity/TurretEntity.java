package com.yueyingiop.payday.entity;

import com.yueyingiop.payday.api.TurretTemperature;
import com.yueyingiop.payday.block.TurretBlock;
import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 炮台的实体代理，负责受击、目标选择、模型状态和 GeckoLib 渲染。 */
public final class TurretEntity extends PathfinderMob implements GeoEntity {
    private static final String TURRET_POS_TAG = "TurretPos";
    private static final String BASE_YAW_TAG = "BaseYaw";
    private static final RawAnimation ACTIVATE_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.turret.activate");
    private static final RawAnimation STOP_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.turret.stop");
    private static final RawAnimation OVERLOAD_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.turret.overload");
    private static final EntityDataAccessor<Boolean> ACTIVATED =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> OVERLOADED =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ACTIVATING =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BASE_YAW =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_YAW =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_PITCH =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos turretPos = BlockPos.ZERO;

    /** 创建炮台实体代理并关闭 AI 和重力。 */
    public TurretEntity(EntityType<? extends TurretEntity> entityType, Level level) {
        super(entityType, level);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
        setInvulnerable(false);
    }

    /** 创建炮台实体的生命值、抗击退和移动速度属性。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** 将代理实体绑定到炮台方块，并设置支持任意角度的初始朝向。 */
    public void bindTo(BlockPos pos, float baseYaw) {
        turretPos = pos.immutable();
        setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        setYRot(baseYaw);
        yRotO = baseYaw;
        setYBodyRot(baseYaw);
        yBodyRotO = baseYaw;
        setYHeadRot(baseYaw);
        yHeadRotO = baseYaw;
        setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    /** 获取实体绑定的炮台方块位置。 */
    public BlockPos getTurretPos() {
        return turretPos;
    }

    /** 获取实体绑定的炮台方块实体。 */
    public TurretBlockEntity getTurret() {
        return level().getBlockEntity(turretPos) instanceof TurretBlockEntity turret
                ? turret : null;
    }

    /** 将方块实体的逻辑状态同步给实体模型。 */
    public void syncModelState(TurretBlockEntity turret) {
        getEntityData().set(ACTIVATED, turret.isActivated());
        getEntityData().set(OVERLOADED, turret.isOverloaded());
        getEntityData().set(ACTIVATING, turret.isActivating());
        getEntityData().set(BASE_YAW, turret.getBaseYaw());
        getEntityData().set(AIM_YAW, turret.getAimYaw());
        getEntityData().set(AIM_PITCH, turret.getAimPitch());
        setYRot(turret.getBaseYaw());
        yRotO = getYRot();
        setYBodyRot(turret.getBaseYaw());
        yBodyRotO = turret.getBaseYaw();
        setYHeadRot(getYRot());
        yHeadRotO = getYHeadRot();
    }

    /** 获取绑定炮台当前温度，供 Jade 等实体信息提供器使用。 */
    public double getTemperature() {
        TurretBlockEntity turret = getTurret();
        return turret == null ? TurretTemperature.INITIAL_TEMPERATURE : turret.getTemperature();
    }

    /** 获取绑定炮台当前温度上限，供 Jade 等实体信息提供器使用。 */
    public double getMaximumTemperature() {
        TurretBlockEntity turret = getTurret();
        return turret == null ? TurretTemperature.getMaximumTemperature()
                : turret.getMaximumTemperature();
    }

    /** 炮台实体不注册移动或攻击 AI。 */
    @Override
    protected void registerGoals() {
        // 炮台自身不移动，也不需要 AI；敌对生物会被 tick 设置为攻击目标。
    }

    /** 注册炮台的激活、工作和过载动画控制器。 */
    /** 注册炮台激活、工作和过载动画控制器。 */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "turret_controller", 0, state -> {
            if (isModelOverloaded()) {
                return state.setAndContinue(OVERLOAD_ANIMATION);
            }
            if (!isActivated()) {
                return PlayState.STOP;
            }
            if (isActivating()) {
                return state.setAndContinue(ACTIVATE_ANIMATION);
            }
            return state.setAndContinue(STOP_ANIMATION);
        }));
    }

    /** 获取炮台实体的 GeckoLib 动画缓存。 */
    /** 返回炮台实体的 GeckoLib 动画缓存。 */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    /** 获取模型是否已经激活。 */
    public boolean isActivated() {
        return getEntityData().get(ACTIVATED);
    }

    /** 获取模型同步的过载状态。 */
    public boolean isModelOverloaded() {
        return getEntityData().get(OVERLOADED);
    }

    /** 获取绑定炮台的逻辑过载状态。 */
    public boolean isOverloaded() {
        TurretBlockEntity turret = getTurret();
        return turret == null || turret.isOverloaded();
    }

    /** 获取模型是否处于展开动画阶段。 */
    public boolean isActivating() {
        return getEntityData().get(ACTIVATING);
    }

    /** 获取炮台初始朝向。 */
    public float getBaseYaw() {
        return getEntityData().get(BASE_YAW);
    }

    /** 获取炮台当前水平瞄准角度。 */
    public float getAimYaw() {
        return getEntityData().get(AIM_YAW);
    }

    /** 获取炮台当前垂直瞄准角度。 */
    public float getAimPitch() {
        return getEntityData().get(AIM_PITCH);
    }

    /** 定义实体模型需要同步的状态数据。 */
    /** 定义同步给客户端模型的炮台状态字段。 */
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTIVATED, false);
        entityData.define(OVERLOADED, false);
        entityData.define(ACTIVATING, false);
        entityData.define(BASE_YAW, 0.0F);
        entityData.define(AIM_YAW, 0.0F);
        entityData.define(AIM_PITCH, 0.0F);
    }

    /** 每 tick 同步模型状态并维护敌对生物的目标。 */
    /** 同步方块实体状态并清理失效绑定实体。 */
    @Override
    public void tick() {
        super.tick();

        TurretBlockEntity turret = getTurret();
        if (turret != null) {
            setPos(turret.getBlockPos().getX() + 0.5D,
                    turret.getBlockPos().getY(), turret.getBlockPos().getZ() + 0.5D);
            syncModelState(turret);
        }

        if (level().isClientSide) {
            return;
        }

        BlockState state = level().getBlockState(turretPos);
        if (!(state.getBlock() instanceof TurretBlock)
                || !(level().getBlockEntity(turretPos) instanceof TurretBlockEntity boundTurret)) {
            discard();
            return;
        }

        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (boundTurret.isOverloaded()) {
            clearHostileTargets();
        }
    }

    /** 过载时清除已经锁定本炮台的敌对生物目标。 */
    public void clearHostileTargets() {
        if (level().isClientSide) {
            return;
        }

        level().getEntitiesOfClass(Mob.class,
                getBoundingBox().inflate(TurretBlockEntity.getAttackRange()),
                candidate -> candidate instanceof Enemy
                        && candidate.isAlive() && candidate.getTarget() == this)
                .forEach(mob -> {
            mob.setTarget(null);
        });
    }

    /** 处理敌对生物对炮台实体的攻击，并将伤害转为温度。 */
    /** 将敌对生物对炮台实体的伤害转交给温度系统。 */
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

    /** 处理 Shift+空手右键收起炮台的交互。 */
    /** 处理 Shift 加空手右键收起炮台的交互。 */
    @Override
    public InteractionResult interactAt(Player player, Vec3 hitPosition, InteractionHand hand) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            return TurretBlock.pickup(level(), turretPos, player);
        }
        return InteractionResult.PASS;
    }

    /** 返回炮台物品作为创造模式选取结果。 */
    /** 返回炮台物品作为创造模式选取结果。 */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(com.yueyingiop.payday.init.RegistryItems.TURRET.get());
    }

    /** 保存实体与炮台方块的绑定位置和基础朝向。 */
    /** 保存实体与炮台方块的绑定位置和基础朝向。 */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong(TURRET_POS_TAG, turretPos.asLong());
        tag.putFloat(BASE_YAW_TAG, getYRot());
    }

    /** 从存档恢复实体与炮台方块的绑定位置和基础朝向。 */
    /** 从存档恢复实体与炮台方块的绑定位置和基础朝向。 */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TURRET_POS_TAG)) {
            turretPos = BlockPos.of(tag.getLong(TURRET_POS_TAG));
        }
        if (tag.contains(BASE_YAW_TAG)) {
            float baseYaw = tag.getFloat(BASE_YAW_TAG);
            setYRot(baseYaw);
            yRotO = baseYaw;
            setYBodyRot(baseYaw);
            yBodyRotO = baseYaw;
            setYHeadRot(baseYaw);
            yHeadRotO = baseYaw;
        }
    }
}
