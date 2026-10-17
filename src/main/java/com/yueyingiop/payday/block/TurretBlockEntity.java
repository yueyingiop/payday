package com.yueyingiop.payday.block;

import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.TurretTemperature;
import com.yueyingiop.payday.api.turret.TurretAimHelper;
import com.yueyingiop.payday.api.turret.TurretCombatHelper;
import com.yueyingiop.payday.api.turret.TurretEntityBindingHelper;
import com.yueyingiop.payday.api.turret.TurretFluidHandler;
import com.yueyingiop.payday.api.turret.TurretFluidState;
import com.yueyingiop.payday.entity.TurretEntity;
import com.yueyingiop.payday.init.RegistryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** 炮台的状态、NBT、流体能力和方块实体同步接口；模型由 {@link TurretEntity} 渲染。 */
public final class TurretBlockEntity extends BlockEntity {
    /** 单个流体槽的最大容量，单位为 mB。 */
    public static final int FLUID_CAPACITY = 4000;

    private static final String ACTIVATED_TAG = "Activated";
    public static final String OVERLOADED_TAG = "Overloaded";
    public static final String TEMPERATURE_TAG = "Temperature";
    private static final String OVERLOADED_AT_TAG = "OverloadedAt";
    private static final String ACTIVATION_TICKS_TAG = "ActivationTicks";
    private static final String ATTACK_COOLDOWN_TAG = "AttackCooldown";
    private static final String AIM_YAW_TAG = "AimYaw";
    private static final String AIM_PITCH_TAG = "AimPitch";
    private static final String AIM_INITIALIZED_TAG = "AimInitialized";
    private static final String BASE_YAW_TAG = "BaseYaw";
    private static final String TURRET_ENTITY_ID_TAG = "TurretEntityId";

    private boolean activated;
    private boolean overloaded;
    private double temperature = TurretTemperature.INITIAL_TEMPERATURE;
    private long overloadedAt = Long.MIN_VALUE;
    private int activationTicks;
    private int attackCooldown;
    private float aimYaw;
    private float aimPitch;
    private float baseYaw;
    private boolean aimInitialized;
    private int turretEntityId = -1;
    private int targetEntityId = -1;
    private final TurretFluidState fluidState = new TurretFluidState(this);
    private final LazyOptional<IFluidHandler> fluidCapability =
            LazyOptional.of(() -> new TurretFluidHandler(this));

    /** 创建炮台方块实体并从方块朝向初始化默认 yaw。 */
    public TurretBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryBlockEntities.TURRET.get(), pos, state);
        baseYaw = TurretAimHelper.baseYaw(state);
        aimYaw = baseYaw;
        aimInitialized = true;
    }

    /** 获取炮台攻击范围。 */
    public static double getAttackRange() {
        return Config.TURRET_ATTACK_RANGE.get();
    }

    /** 获取炮台基础攻击伤害。 */
    public static float getAttackDamage() {
        return Config.TURRET_ATTACK_DAMAGE.get().floatValue();
    }

    /** 获取炮台左右对称的水平偏航角。 */
    public static float getYawAngle() {
        return Config.TURRET_YAW_ANGLE.get().floatValue();
    }

    /** 获取炮台允许的最小俯仰角。 */
    public static float getMinAimPitch() {
        return -Config.TURRET_PITCH_DOWN_ANGLE.get().floatValue();
    }

    /** 获取炮台允许的最大仰角。 */
    public static float getMaxAimPitch() {
        return Config.TURRET_PITCH_UP_ANGLE.get().floatValue();
    }

    /** 执行服务端炮台 tick。 */
    public void serverTick() {
        TurretCombatHelper.serverTick(this);
    }

    /** 执行客户端瞄准和过载粒子 tick。 */
    public void clientTick() {
        TurretCombatHelper.clientTick(this);
    }

    /** 激活炮台并开始展开动画。 */
    public void activate() {
        TurretCombatHelper.activate(this);
    }

    /** 触发炮台过载并停止敌对目标锁定。 */
    public void triggerOverload() {
        if (overloaded || level == null || level.isClientSide) {
            return;
        }
        overloaded = true;
        overloadedAt = level.getGameTime();
        TurretEntity entity = getTurretEntity();
        if (entity != null) {
            entity.clearHostileTargets();
        }
        level.setBlock(worldPosition, getBlockState()
                        .setValue(TurretBlock.OVERLOADED, true)
                        .setValue(TurretBlock.FIRING, false),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        markStateChanged();
    }

    /** 判断炮台是否已经激活。 */
    public boolean isActivated() {
        return activated || getBlockState().getValue(TurretBlock.ACTIVE);
    }

    /** 判断炮台是否已经过载。 */
    public boolean isOverloaded() {
        return overloaded || getBlockState().getValue(TurretBlock.OVERLOADED);
    }

    /** 获取炮台当前温度。 */
    public double getTemperature() {
        return temperature;
    }

    /** 设置温度系统使用的内部温度值。 */
    public void setTemperatureInternal(double value) {
        temperature = value;
    }

    /** 获取冷却槽中的流体副本。 */
    public FluidStack getCoolantFluid() {
        return fluidState.coolant();
    }

    /** 获取过载槽中的流体副本。 */
    public FluidStack getOverloadFluid() {
        return fluidState.overload();
    }

    /** 获取包含过载液加成后的温度上限。 */
    public double getMaximumTemperature() {
        return fluidState.maximumTemperature();
    }

    /** 处理冷却液和过载液的每 tick 效果。 */
    public void tickFluids() {
        fluidState.tick();
    }

    /** 清空两个流体槽。 */
    public void clearFluids() {
        fluidState.clear();
    }

    /** 获取炮台的双槽流体能力。 */
    public IFluidHandler getFluidHandler() {
        return fluidCapability.orElse(null);
    }

    /** 获取只对应指定槽位的流体能力。 */
    public IFluidHandler getFluidHandler(int tank) {
        return tank < 0 || tank >= 2 ? null : new TurretFluidHandler(this, tank);
    }

    /** 判断流体是否可以放入指定槽位。 */
    public boolean acceptsFluid(int tank, FluidStack stack) {
        return fluidState.accepts(tank, stack);
    }

    /** 获取指定槽位中的内部流体。 */
    public FluidStack fluidInTank(int tank) {
        return fluidState.fluidInTank(tank);
    }

    /** 设置指定槽位中的流体。 */
    public void setFluidInTank(int tank, FluidStack stack) {
        fluidState.setFluidInTank(tank, stack);
    }

    /** 获取或查找与炮台绑定的实体。 */
    public TurretEntity getTurretEntity() {
        return TurretEntityBindingHelper.find(this);
    }

    /** 炮台方块移除时同步移除绑定实体。 */
    public void removeTurretEntity() {
        TurretEntityBindingHelper.remove(this);
    }

    /** 获取炮台是否处于展开动画阶段。 */
    public boolean isActivating() {
        return getBlockState().getValue(TurretBlock.ACTIVATING);
    }

    /** 获取炮台是否处于开火状态。 */
    public boolean isFiring() {
        return getBlockState().getValue(TurretBlock.FIRING);
    }

    /** 获取当前水平瞄准角度。 */
    public float getAimYaw() {
        ensureAimInitialized();
        return aimYaw;
    }

    /** 获取当前垂直瞄准角度。 */
    public float getAimPitch() {
        ensureAimInitialized();
        return Mth.clamp(aimPitch, getMinAimPitch(), getMaxAimPitch());
    }

    /** 获取炮台任意角度的初始水平朝向。 */
    public float getBaseYaw() {
        return baseYaw;
    }

    /** 设置炮台任意角度的初始水平朝向。 */
    public void setBaseYaw(float value) {
        baseYaw = Mth.wrapDegrees(value);
        if (aimInitialized) {
            aimYaw = baseYaw;
        }
        markStateChanged();
    }

    /** 更新当前瞄准角度，供瞄准辅助类使用。 */
    public void setAimInternal(float yaw, float pitch) {
        aimYaw = Mth.wrapDegrees(yaw);
        aimPitch = Mth.clamp(pitch, getMinAimPitch(), getMaxAimPitch());
        setChanged();
    }

    /** 获取激活动画剩余 tick。 */
    public int getActivationTicks() {
        return activationTicks;
    }

    /** 设置激活动画剩余 tick。 */
    public void setActivationTicks(int ticks) {
        activationTicks = Math.max(0, ticks);
    }

    /** 获取攻击冷却剩余 tick。 */
    public int getAttackCooldown() {
        return attackCooldown;
    }

    /** 设置攻击冷却剩余 tick。 */
    public void setAttackCooldown(int ticks) {
        attackCooldown = Math.max(0, ticks);
    }

    /** 设置激活状态，供战斗辅助类使用。 */
    public void setActivatedInternal(boolean value) {
        activated = value;
    }

    /** 获取过载开始时间。 */
    public long getOverloadedAt() {
        return overloadedAt;
    }

    /** 设置过载开始时间。 */
    public void setOverloadedAt(long value) {
        overloadedAt = value;
    }

    /** 获取绑定实体 ID。 */
    public int getTurretEntityId() {
        return turretEntityId;
    }

    /** 设置绑定实体 ID。 */
    public void setTurretEntityId(int id) {
        turretEntityId = id;
    }

    /** 获取炮台当前分配的攻击目标。 */
    public LivingEntity getTargetEntity() {
        if (level == null || targetEntityId < 0) {
            return null;
        }
        return level.getEntity(targetEntityId) instanceof LivingEntity target ? target : null;
    }

    /** 获取炮台当前攻击目标的实体 ID。 */
    public int getTargetEntityId() {
        return targetEntityId;
    }

    /** 设置炮台当前攻击目标，不触发方块更新。 */
    public void setTargetEntityId(int id) {
        targetEntityId = id;
    }

    /** 标记状态变化并向附近客户端发送方块实体同步包。 */
    public void markStateChanged() {
        setChanged();
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            ClientboundBlockEntityDataPacket packet = getUpdatePacket();
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(new net.minecraft.world.level.ChunkPos(worldPosition), false)) {
                player.connection.send(packet);
            }
        }
    }

    /** 提供炮台流体能力。 */
    /** 返回炮台的 Forge 流体能力。 */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    /** 失效炮台的 Forge capability。 */
    /** 失效炮台持有的 Forge 流体能力。 */
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    /** 确保旧存档中的瞄准状态拥有有效初始值。 */
    private void ensureAimInitialized() {
        if (!aimInitialized) {
            aimYaw = baseYaw;
            aimPitch = 0.0F;
            aimInitialized = true;
        }
    }

    /** 保存炮台状态、瞄准角度、流体和绑定实体 ID。 */
    /** 保存炮台状态、瞄准角度、流体和绑定实体数据。 */
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
        tag.putFloat(BASE_YAW_TAG, baseYaw);
        tag.putBoolean(AIM_INITIALIZED_TAG, aimInitialized);
        tag.putInt(TURRET_ENTITY_ID_TAG, turretEntityId);
        fluidState.save(tag);
    }

    /** 从 NBT 恢复炮台状态、瞄准角度、流体和绑定实体 ID。 */
    /** 从 NBT 恢复炮台状态、瞄准角度、流体和绑定实体数据。 */
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
        baseYaw = tag.contains(BASE_YAW_TAG)
                ? Mth.wrapDegrees(tag.getFloat(BASE_YAW_TAG))
                : TurretAimHelper.baseYaw(getBlockState());
        aimInitialized = tag.getBoolean(AIM_INITIALIZED_TAG);
        turretEntityId = tag.contains(TURRET_ENTITY_ID_TAG)
                ? tag.getInt(TURRET_ENTITY_ID_TAG) : -1;
        fluidState.load(tag);
        temperature = Mth.clamp(temperature, TurretTemperature.INITIAL_TEMPERATURE,
                getMaximumTemperature());
    }

    /** 获取客户端方块实体同步 NBT。 */
    /** 返回客户端初始化方块实体所需的同步数据。 */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    /** 获取客户端方块实体同步数据包。 */
    /** 创建客户端接收炮台状态变化的同步数据包。 */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
