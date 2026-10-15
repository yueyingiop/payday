package com.yueyingiop.payday.block;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.TurretTemperature;
import com.yueyingiop.payday.api.TurretFluid;
import com.yueyingiop.payday.api.TurretFluidManager;
import com.yueyingiop.payday.entity.TurretEntity;
import com.yueyingiop.payday.init.RegistryBlockEntities;
import com.yueyingiop.payday.init.RegistryEntities;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;

/** 炮台的攻击、瞄准、过载和 GeckoLib 动画状态。 */
public final class TurretBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements GeoBlockEntity {
    /** 单个流体槽的最大容量，单位为 mB。 */
    public static final int FLUID_CAPACITY = 4000;
    private static final int ACTIVATION_ANIMATION_TICKS = 10;
    private static final float MAX_YAW_STEP = 12.0F;
    private static final float MAX_PITCH_STEP = 8.0F;
    private static final float AIM_TOLERANCE = 3.0F;
    /* laser 的起始面是枪口；这些坐标来自 turret.geo.json，单位为方块。 */
    private static final double MODEL_UNIT = 1.0D / 16.0D;
    private static final Vec3 LASER_MUZZLE_LOCAL = new Vec3(-1.0D * MODEL_UNIT,
            6.5D * MODEL_UNIT, 9.0D * MODEL_UNIT);
    private static final Vec3 IN_SUPPORT_PIVOT = new Vec3(-2.0D * MODEL_UNIT,
            1.0D * MODEL_UNIT, 7.0D * MODEL_UNIT);
    private static final Vec3 OUT_SUPPORT_PIVOT = new Vec3(0.0D,
            3.0D * MODEL_UNIT, -7.0D * MODEL_UNIT);
    private static final Vec3 HEAD_PIVOT = new Vec3(0.0D,
            3.0D * MODEL_UNIT, 7.0D * MODEL_UNIT);

    private static final String ACTIVATED_TAG = "Activated";
    public static final String OVERLOADED_TAG = "Overloaded";
    public static final String TEMPERATURE_TAG = "Temperature";
    private static final String OVERLOADED_AT_TAG = "OverloadedAt";
    private static final String ACTIVATION_TICKS_TAG = "ActivationTicks";
    private static final String ATTACK_COOLDOWN_TAG = "AttackCooldown";
    private static final String AIM_YAW_TAG = "AimYaw";
    private static final String AIM_PITCH_TAG = "AimPitch";
    private static final String AIM_INITIALIZED_TAG = "AimInitialized";
    private static final String COOLANT_FLUID_TAG = "CoolantFluid";
    private static final String OVERLOAD_FLUID_TAG = "OverloadFluid";
    private static final String COOLANT_TICKS_TAG = "CoolantTicks";
    private static final String OVERLOAD_TICKS_TAG = "OverloadTicks";

    private static final RawAnimation ACTIVATE_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.turret.activate");
    private static final RawAnimation STOP_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.turret.stop");
    private static final RawAnimation OVERLOAD_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.turret.overload");
    private static final ResourceLocation RPK_GUN_ID =
            ResourceLocation.fromNamespaceAndPath("tacz", "rpk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean activated;
    private boolean overloaded;
    private double temperature = TurretTemperature.INITIAL_TEMPERATURE;
    private long overloadedAt = Long.MIN_VALUE;
    private int attackCooldown;
    private int activationTicks;
    private float aimYaw;
    private float aimPitch;
    private boolean aimInitialized;
    private int turretEntityId = -1;
    private FluidStack coolantFluid = FluidStack.EMPTY;
    private FluidStack overloadFluid = FluidStack.EMPTY;
    private int coolantTicks;
    private int overloadTicks;
    private final LazyOptional<IFluidHandler> fluidCapability =
            LazyOptional.of(() -> new TurretFluidHandler(this));

    public TurretBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryBlockEntities.TURRET.get(), pos, state);
        this.aimYaw = baseYaw(state);
        this.aimInitialized = true;
    }

    public static double getAttackRange() {
        return Config.TURRET_ATTACK_RANGE.get();
    }

    public static float getAttackDamage() {
        return Config.TURRET_ATTACK_DAMAGE.get().floatValue();
    }

    /** 左右对称的水平偏航角，单位为度。 */
    public static float getYawAngle() {
        return Config.TURRET_YAW_ANGLE.get().floatValue();
    }

    /** 游戏语义：负值向下，正值向上。 */
    public static float getMinAimPitch() {
        return -Config.TURRET_PITCH_DOWN_ANGLE.get().floatValue();
    }

    public static float getMaxAimPitch() {
        return Config.TURRET_PITCH_UP_ANGLE.get().floatValue();
    }

    /** 服务端每 tick 执行一次瞄准和攻击逻辑。 */
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!activated) {
            activate();
            return;
        }

        ensureTurretEntity();

        if (activationTicks > 0) {
            activationTicks--;
            if (activationTicks == 0) {
                setAnimationState(TurretBlock.ACTIVATING, false);
            }
            setChanged();
            return;
        }

        processFluids();

        if (overloaded) {
            if (overloadedAt == Long.MIN_VALUE) {
                overloadedAt = level.getGameTime();
                setChanged();
            } else if (level.getGameTime() - overloadedAt
                    >= TurretTemperature.getOverloadLifetimeTicks()) {
                level.removeBlock(worldPosition, false);
                return;
            }
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        LivingEntity target = findTarget();
        if (target != null) {
            updateAim(target);
        }

        if (attackCooldown > 0 || target == null
                || !isAimedAt(target)) {
            return;
        }

        if (!target.hurt(level.damageSources().generic(), getAttackDamage())) {
            return;
        }

        attackCooldown = Config.TURRET_ATTACK_INTERVAL_TICKS.get();
        Vec3 muzzle = getMuzzlePosition();
        playShootSound();
        spawnMuzzleFlash(muzzle);
        TurretTemperature.tryAddAttackHeat(this);
        setChanged();
    }

    /** 客户端只更新炮台的瞄准角度，不执行攻击逻辑。 */
    public void clientTick() {
        if (level == null || !level.isClientSide) {
            return;
        }

        if (isOverloaded()) {
            spawnOverloadSparks();
            return;
        }
        if (!isActivated() || isActivating()) {
            return;
        }

        LivingEntity target = findTarget();
        if (target != null) {
            updateAim(target);
        }
    }

    /** 在过载炮台周围生成电火花、火焰和烟雾粒子。 */
    private void spawnOverloadSparks() {
        double x = worldPosition.getX() + 0.2D + level.random.nextDouble() * 0.6D;
        double y = worldPosition.getY() + 1.0D + level.random.nextDouble();
        double z = worldPosition.getZ() + 0.2D + level.random.nextDouble() * 0.6D;
        double vx = (level.random.nextDouble() - 0.5D) * 0.08D;
        double vy = level.random.nextDouble() * 0.08D;
        double vz = (level.random.nextDouble() - 0.5D) * 0.08D;

        if (level.random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, vx, vy, vz);
        }
        if (level.random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, vx * 0.5D, vy, vz * 0.5D);
        }
        if (level.random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.15D, z,
                    vx * 0.35D, vy * 0.6D + 0.02D, vz * 0.35D);
        }
    }

    /** 激活炮台并播放一次展开动画。 */
    public void activate() {
        if (level == null || level.isClientSide || activated || overloaded) {
            return;
        }

        activated = true;
        activationTicks = ACTIVATION_ANIMATION_TICKS;
        level.setBlock(worldPosition, getBlockState()
                        .setValue(TurretBlock.ACTIVE, true)
                        .setValue(TurretBlock.ACTIVATING, true),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        setChanged();
    }

    /** 触发炮台过载，由温度系统调用。 */
    public void triggerOverload() {
        if (overloaded || level == null || level.isClientSide) {
            return;
        }

        overloaded = true;
        overloadedAt = level.getGameTime();
        TurretEntity turretEntity = getTurretEntity();
        if (turretEntity != null) {
            turretEntity.clearHostileTargets();
        }
        level.setBlock(worldPosition, getBlockState()
                        .setValue(TurretBlock.OVERLOADED, true)
                        .setValue(TurretBlock.FIRING, false),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        setChanged();
    }

    /** 在当前枪口位置生成一次开火火焰粒子。 */
    private void spawnMuzzleFlash(Vec3 muzzle) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        Vec3 direction = getMuzzleDirection();
        serverLevel.sendParticles(ParticleTypes.FLAME,
                muzzle.x, muzzle.y, muzzle.z, 6,
                direction.x * 0.08D, direction.y * 0.08D, direction.z * 0.08D,
                0.02D);
    }

    /** 通过 TACZ 动态音效系统播放默认枪包中的 RPK 射击音效。 */
    private void playShootSound() {
        TurretEntity turretEntity = getTurretEntity();
        if (turretEntity == null) {
            return;
        }

        SoundManager.sendSoundToNearby(
                turretEntity,
                64,
                RPK_GUN_ID,
                RPK_GUN_ID,
                "shoot",
                0.8F,
                0.9F + level.random.nextFloat() * 0.125F);
    }

    /** 返回从炮口向外的世界方向，用于让火焰粒子沿枪口方向散开。 */
    private Vec3 getMuzzleDirection() {
        double yaw = Math.toRadians(getAimYaw());
        double pitch = Math.toRadians(getAimPitch());
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal,
                Math.sin(pitch),
                Math.cos(yaw) * horizontal).normalize();
    }

    private void setAnimationState(
            net.minecraft.world.level.block.state.properties.BooleanProperty property, boolean value) {
        if (level == null || level.isClientSide || getBlockState().getValue(property) == value) {
            return;
        }

        level.setBlock(worldPosition, getBlockState().setValue(property, value),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    private LivingEntity findTarget() {
        if (level == null) {
            return null;
        }

        AABB searchArea = new AABB(worldPosition).inflate(getAttackRange());
        return level.getEntitiesOfClass(LivingEntity.class, searchArea,
                        entity -> entity.isAlive()
                                && entity instanceof Enemy
                                && !entity.isInvulnerable()
                                && canTrack(entity))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D)))
                .orElse(null);
    }

    private void updateAim(LivingEntity target) {
        ensureAimInitialized();

        float targetYaw = targetYaw(target);
        float targetPitch = targetPitch(target);

        aimYaw = approachAngle(aimYaw, targetYaw, MAX_YAW_STEP);
        aimPitch = Mth.clamp(approach(aimPitch, targetPitch, MAX_PITCH_STEP),
                getMinAimPitch(), getMaxAimPitch());
    }

    private boolean isAimedAt(LivingEntity target) {
        float targetYaw = targetYaw(target);
        float targetPitch = targetPitch(target);
        return Math.abs(Mth.wrapDegrees(targetYaw - aimYaw)) <= AIM_TOLERANCE
                && Math.abs(targetPitch - aimPitch) <= AIM_TOLERANCE;
    }

    private boolean canTrack(LivingEntity target) {
        float yaw = targetYaw(target);
        float pitch = targetPitch(target);
        float yawDifference = Mth.wrapDegrees(yaw - getBaseYaw());
        return Math.abs(yawDifference) <= getYawAngle()
                && pitch >= getMinAimPitch() && pitch <= getMaxAimPitch();
    }

    private float targetYaw(LivingEntity target) {
        Vec3 muzzle = getMuzzlePosition();
        double dx = target.getX() - muzzle.x;
        double dz = target.getZ() - muzzle.z;
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private float targetPitch(LivingEntity target) {
        float yaw = targetYaw(target);
        float pitch = aimPitch;
        for (int i = 0; i < 4; i++) {
            Vec3 muzzle = getMuzzlePosition(yaw, pitch);
            double dx = target.getX() - muzzle.x;
            double dz = target.getZ() - muzzle.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double targetCenterY = target.getY() + target.getBbHeight() * 0.5D;
            pitch = (float) (Mth.atan2(targetCenterY - muzzle.y, horizontalDistance)
                    * Mth.RAD_TO_DEG);
        }
        return pitch;
    }

    /**
     * 返回 laser 起始面中心在世界中的位置。
     *
     * laser 并不是直接挂在方块中心，而是经过三段展开骨骼。这里复现 stop 动画的
     * 固定变换，再叠加 head 的实际瞄准角度，避免用方块中心近似枪口。
     */
    public Vec3 getMuzzlePosition() {
        return getMuzzlePosition(aimYaw, getAimPitch());
    }

    private Vec3 getMuzzlePosition(float yawDegrees, float pitchDegrees) {
        Vec3 local = LASER_MUZZLE_LOCAL;
        // GeckoLib 的 X 轴正旋转会让炮管下压，因此游戏中的正俯仰角要取反。
        local = rotateX(local, HEAD_PIVOT, 60.0D - pitchDegrees)
                .add(0.0D, -5.0D * MODEL_UNIT, 0.0D);
        local = rotateX(local, OUT_SUPPORT_PIVOT, -120.0D);
        local = rotateX(local, IN_SUPPORT_PIVOT, 60.0D)
                .add(0.0D, 0.0D, -6.0D * MODEL_UNIT);

        double yaw = Math.toRadians(yawDegrees);
        double worldX = Math.cos(yaw) * local.x - Math.sin(yaw) * local.z;
        double worldZ = Math.sin(yaw) * local.x + Math.cos(yaw) * local.z;
        return new Vec3(worldPosition.getX() + 0.5D + worldX,
                worldPosition.getY() + local.y,
                worldPosition.getZ() + 0.5D + worldZ);
    }

    private static Vec3 rotateX(Vec3 point, Vec3 pivot, double degrees) {
        double angle = Math.toRadians(degrees);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double y = point.y - pivot.y;
        double z = point.z - pivot.z;
        return new Vec3(point.x,
                pivot.y + y * cos - z * sin,
                pivot.z + y * sin + z * cos);
    }

    private void ensureAimInitialized() {
        if (!aimInitialized) {
            aimYaw = baseYaw(getBlockState());
            aimPitch = 0.0F;
            aimInitialized = true;
        }
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float difference = Mth.wrapDegrees(target - current);
        return current + Mth.clamp(difference, -maxStep, maxStep);
    }

    private static float approach(float current, float target, float maxStep) {
        return current + Mth.clamp(target - current, -maxStep, maxStep);
    }

    private static float baseYaw(BlockState state) {
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        // GeoBlockRenderer 的模型正面是 local +Z，而 HORIZONTAL_FACING 表示其反方向。
        return Mth.wrapDegrees(direction.toYRot() + 180.0F);
    }

    public boolean isActivated() {
        return activated || getBlockState().getValue(TurretBlock.ACTIVE);
    }

    public boolean isOverloaded() {
        return overloaded || getBlockState().getValue(TurretBlock.OVERLOADED);
    }

    public double getTemperature() {
        return temperature;
    }

    /** 获取冷却槽中的流体副本。 */
    public FluidStack getCoolantFluid() {
        return coolantFluid.copy();
    }

    /** 获取过载槽中的流体副本。 */
    public FluidStack getOverloadFluid() {
        return overloadFluid.copy();
    }

    /** 获取包含过载液加成后的温度上限。 */
    public double getMaximumTemperature() {
        double maximum = TurretTemperature.getMaximumTemperature();
        TurretFluid definition = getFluidDefinition(overloadFluid);
        if (definition != null && definition.type() == TurretFluid.Type.OVERLOAD
                && !overloadFluid.isEmpty()) {
            maximum += definition.maximumTemperatureBonus();
        }
        return maximum;
    }

    /** 获取炮台流体能力，供桶、流体容器和 GUI 使用。 */
    public IFluidHandler getFluidHandler() {
        return fluidCapability.orElse(null);
    }

    /** 获取只对应指定槽位的流体能力，供 GUI 的两个桶位分别操作。 */
    public IFluidHandler getFluidHandler(int tank) {
        return tank < 0 || tank >= 2 ? null : new TurretFluidHandler(this, tank);
    }

    /** 清空两个流体槽，拆除炮台时调用。 */
    public void clearFluids() {
        coolantFluid = FluidStack.EMPTY;
        overloadFluid = FluidStack.EMPTY;
        coolantTicks = 0;
        overloadTicks = 0;
        markStateChanged();
    }

    /** 供 TurretTemperature 使用，调用方负责校验范围和过载。 */
    public void setTemperatureInternal(double value) {
        temperature = value;
    }

    /** 每 tick 处理冷却液和过载液。 */
    private void processFluids() {
        boolean changed = false;
        TurretFluid coolant = getFluidDefinition(coolantFluid);
        if (!coolantFluid.isEmpty() && coolant != null && !isOverloaded()) {
            if (coolant.type() == TurretFluid.Type.CONSUMABLE_COOLANT
                    && coolant.temperaturePerMb() > 0.0D && temperature > 0.0D) {
                int required = (int) Math.ceil(temperature / coolant.temperaturePerMb());
                int consumed = Math.min(required, coolantFluid.getAmount());
                if (consumed > 0) {
                    coolantFluid.shrink(consumed);
                    temperature = Math.max(TurretTemperature.INITIAL_TEMPERATURE,
                            temperature - consumed * coolant.temperaturePerMb());
                    changed = true;
                }
            } else if (coolant.type() == TurretFluid.Type.CIRCULATING_COOLANT) {
                coolantTicks++;
                if (coolantTicks >= coolant.coolingIntervalTicks()) {
                    coolantTicks = 0;
                    double amount = coolantFluid.getAmount() * coolant.coolingCoefficient();
                    if (amount > 0.0D) {
                        double previous = temperature;
                        temperature = Math.max(TurretTemperature.INITIAL_TEMPERATURE,
                                temperature - amount);
                        changed |= previous != temperature;
                    }
                }
            }
        } else if (coolantFluid.isEmpty() || coolant == null
                || coolant.type() != TurretFluid.Type.CIRCULATING_COOLANT) {
            coolantTicks = 0;
        }

        TurretFluid overload = getFluidDefinition(overloadFluid);
        if (!overloadFluid.isEmpty() && overload != null
                && overload.type() == TurretFluid.Type.OVERLOAD) {
            overloadTicks++;
            if (overloadTicks >= overload.consumptionIntervalTicks()) {
                overloadTicks = 0;
                int consumed = Math.min(overload.consumptionAmountMb(), overloadFluid.getAmount());
                if (consumed > 0) {
                    overloadFluid.shrink(consumed);
                    changed = true;
                }
            }
        } else {
            overloadTicks = 0;
        }

        if (!isOverloaded() && temperature >= getMaximumTemperature()) {
            TurretTemperature.set(this, temperature);
            changed = true;
        }
        if (changed) {
            markStateChanged();
        }
    }

    private static TurretFluid getFluidDefinition(FluidStack stack) {
        return stack == null || stack.isEmpty() ? null : TurretFluidManager.get(stack.getFluid());
    }

    private static FluidStack clampFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(stack, Math.min(FLUID_CAPACITY, stack.getAmount()));
    }

    private boolean acceptsFluid(int tank, FluidStack stack) {
        if (stack == null || stack.isEmpty() || (tank != 0 && tank != 1)) {
            return false;
        }
        TurretFluid definition = getFluidDefinition(stack);
        if (definition == null) {
            return false;
        }
        boolean correctType = tank == 0
                ? definition.type() == TurretFluid.Type.CONSUMABLE_COOLANT
                || definition.type() == TurretFluid.Type.CIRCULATING_COOLANT
                : definition.type() == TurretFluid.Type.OVERLOAD;
        if (!correctType) {
            return false;
        }
        FluidStack current = tank == 0 ? coolantFluid : overloadFluid;
        return current.isEmpty() || current.isFluidEqual(stack);
    }

    private FluidStack fluidInTank(int tank) {
        return tank == 0 ? coolantFluid : overloadFluid;
    }

    private void setFluidInTank(int tank, FluidStack stack) {
        if (tank == 0) {
            coolantFluid = stack;
            coolantTicks = 0;
        } else {
            overloadFluid = stack;
            overloadTicks = 0;
        }
        markStateChanged();
    }

    /** 标记炮台状态变化，并立即同步客户端 GUI、Jade 和渲染器所需的数据。 */
    private void markStateChanged() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    3);
            ClientboundBlockEntityDataPacket packet = getUpdatePacket();
            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(new ChunkPos(worldPosition), false)) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    /** 获取或创建与炮台绑定的生物实体。 */
    public TurretEntity getTurretEntity() {
        if (level == null) {
            return null;
        }
        if (turretEntityId >= 0
                && level.getEntity(turretEntityId) instanceof TurretEntity turret) {
            return turret;
        }

        TurretEntity turret = level.getEntitiesOfClass(TurretEntity.class,
                        new AABB(worldPosition))
                .stream()
                .filter(entity -> worldPosition.equals(entity.getTurretPos()))
                .findFirst()
                .orElse(null);
        if (turret != null) {
            turretEntityId = turret.getId();
        }
        return turret;
    }

    private void ensureTurretEntity() {
        if (getTurretEntity() != null) {
            return;
        }

        TurretEntity turret = RegistryEntities.TURRET.get().create(level);
        if (turret == null) {
            return;
        }
        turret.bindTo(worldPosition);
        level.addFreshEntity(turret);
        turretEntityId = turret.getId();
        setChanged();
    }

    /** 炮台方块移除时同步移除受击代理实体。 */
    public void removeTurretEntity() {
        TurretEntity turret = getTurretEntity();
        if (turret != null) {
            turret.discard();
        }
        turretEntityId = -1;
    }

    public boolean isActivating() {
        return getBlockState().getValue(TurretBlock.ACTIVATING);
    }

    public boolean isFiring() {
        return getBlockState().getValue(TurretBlock.FIRING);
    }

    public float getAimYaw() {
        ensureAimInitialized();
        return aimYaw;
    }

    public float getAimPitch() {
        ensureAimInitialized();
        return Mth.clamp(aimPitch, getMinAimPitch(), getMaxAimPitch());
    }

    public float getBaseYaw() {
        return baseYaw(getBlockState());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(ACTIVATED_TAG, activated);
        tag.putBoolean(OVERLOADED_TAG, overloaded);
        tag.putDouble(TEMPERATURE_TAG, temperature);
        tag.putLong(OVERLOADED_AT_TAG, overloadedAt);
        tag.putInt(ACTIVATION_TICKS_TAG, activationTicks);
        tag.putInt(ATTACK_COOLDOWN_TAG, attackCooldown);
        tag.putFloat(AIM_YAW_TAG, aimYaw);
        tag.putFloat(AIM_PITCH_TAG, aimPitch);
        tag.putBoolean(AIM_INITIALIZED_TAG, aimInitialized);
        tag.putInt("TurretEntityId", turretEntityId);
        if (!coolantFluid.isEmpty()) {
            tag.put(COOLANT_FLUID_TAG, coolantFluid.writeToNBT(new CompoundTag()));
        }
        if (!overloadFluid.isEmpty()) {
            tag.put(OVERLOAD_FLUID_TAG, overloadFluid.writeToNBT(new CompoundTag()));
        }
        tag.putInt(COOLANT_TICKS_TAG, coolantTicks);
        tag.putInt(OVERLOAD_TICKS_TAG, overloadTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activated = tag.getBoolean(ACTIVATED_TAG);
        overloaded = tag.getBoolean(OVERLOADED_TAG);
        temperature = Math.max(TurretTemperature.INITIAL_TEMPERATURE,
                tag.getDouble(TEMPERATURE_TAG));
        overloadedAt = tag.contains(OVERLOADED_AT_TAG)
                ? tag.getLong(OVERLOADED_AT_TAG) : Long.MIN_VALUE;
        activationTicks = tag.getInt(ACTIVATION_TICKS_TAG);
        attackCooldown = tag.getInt(ATTACK_COOLDOWN_TAG);
        aimYaw = tag.getFloat(AIM_YAW_TAG);
        aimPitch = Mth.clamp(tag.getFloat(AIM_PITCH_TAG), getMinAimPitch(), getMaxAimPitch());
        aimInitialized = tag.getBoolean(AIM_INITIALIZED_TAG);
        turretEntityId = tag.contains("TurretEntityId")
                ? tag.getInt("TurretEntityId") : -1;
        coolantFluid = tag.contains(COOLANT_FLUID_TAG, Tag.TAG_COMPOUND)
                ? FluidStack.loadFluidStackFromNBT(tag.getCompound(COOLANT_FLUID_TAG))
                : FluidStack.EMPTY;
        overloadFluid = tag.contains(OVERLOAD_FLUID_TAG, Tag.TAG_COMPOUND)
                ? FluidStack.loadFluidStackFromNBT(tag.getCompound(OVERLOAD_FLUID_TAG))
                : FluidStack.EMPTY;
        coolantFluid = clampFluid(coolantFluid);
        overloadFluid = clampFluid(overloadFluid);
        coolantTicks = tag.getInt(COOLANT_TICKS_TAG);
        overloadTicks = tag.getInt(OVERLOAD_TICKS_TAG);
        temperature = Mth.clamp(temperature, TurretTemperature.INITIAL_TEMPERATURE,
                getMaximumTemperature());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<TurretBlockEntity> controller = new AnimationController<>(this,
                "turret_controller", 0, state -> {
                    if (isOverloaded()) {
                        return state.setAndContinue(OVERLOAD_ANIMATION);
                    }
                    if (!isActivated()) {
                        return PlayState.STOP;
                    }
                    if (isActivating()) {
                        return state.setAndContinue(ACTIVATE_ANIMATION);
                    }
                    return state.setAndContinue(STOP_ANIMATION);
                });
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 炮台的双槽流体能力实现。 */
    private static final class TurretFluidHandler implements IFluidHandler {
        private final TurretBlockEntity turret;
        private final int fixedTank;

        private TurretFluidHandler(TurretBlockEntity turret) {
            this(turret, -1);
        }

        private TurretFluidHandler(TurretBlockEntity turret, int fixedTank) {
            this.turret = turret;
            this.fixedTank = fixedTank;
        }

        @Override
        public int getTanks() {
            return fixedTank < 0 ? 2 : 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            int actualTank = actualTank(tank);
            return actualTank >= 0 && actualTank < 2
                    ? turret.fluidInTank(actualTank).copy() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return actualTank(tank) >= 0 ? FLUID_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return turret.acceptsFluid(actualTank(tank), stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) {
                return 0;
            }
            for (int tank = 0; tank < (fixedTank < 0 ? 2 : 1); tank++) {
                int actualTank = actualTank(tank);
                if (!turret.acceptsFluid(actualTank, resource)) {
                    continue;
                }
                FluidStack current = turret.fluidInTank(actualTank);
                int amount = Math.min(resource.getAmount(), FLUID_CAPACITY - current.getAmount());
                if (amount <= 0) {
                    continue;
                }
                if (action.execute()) {
                    FluidStack result = current.isEmpty()
                            ? new FluidStack(resource, amount) : current.copy();
                    if (!current.isEmpty()) {
                        result.grow(amount);
                    }
                    turret.setFluidInTank(actualTank, result);
                }
                return amount;
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            for (int tank = 0; tank < (fixedTank < 0 ? 2 : 1); tank++) {
                int actualTank = actualTank(tank);
                FluidStack current = turret.fluidInTank(actualTank);
                if (!current.isEmpty() && current.isFluidEqual(resource)) {
                    int amount = Math.min(resource.getAmount(), current.getAmount());
                    return drainTank(actualTank, amount, action);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            for (int tank = 0; tank < (fixedTank < 0 ? 2 : 1); tank++) {
                int actualTank = actualTank(tank);
                if (!turret.fluidInTank(actualTank).isEmpty()) {
                    return drainTank(actualTank, maxDrain, action);
                }
            }
            return FluidStack.EMPTY;
        }

        private FluidStack drainTank(int tank, int amount, FluidAction action) {
            FluidStack current = turret.fluidInTank(tank);
            int drained = Math.min(amount, current.getAmount());
            FluidStack result = new FluidStack(current, drained);
            if (action.execute()) {
                FluidStack remaining = current.copy();
                remaining.shrink(drained);
                turret.setFluidInTank(tank, remaining.isEmpty() ? FluidStack.EMPTY : remaining);
            }
            return result;
        }

        private int actualTank(int tank) {
            return fixedTank < 0 ? tank : fixedTank;
        }
    }
}
