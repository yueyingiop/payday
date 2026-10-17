package com.yueyingiop.payday.api.turret;

import com.yueyingiop.payday.api.TurretFluid;
import com.yueyingiop.payday.api.TurretFluidManager;
import com.yueyingiop.payday.api.TurretTemperature;
import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraftforge.fluids.FluidStack;

/** 保存并处理炮台的冷却液、过载液及其消耗计时。 */
public final class TurretFluidState {
    private static final String COOLANT_FLUID_TAG = "CoolantFluid";
    private static final String OVERLOAD_FLUID_TAG = "OverloadFluid";
    private static final String COOLANT_TICKS_TAG = "CoolantTicks";
    private static final String OVERLOAD_TICKS_TAG = "OverloadTicks";

    private final TurretBlockEntity turret;
    private FluidStack coolant = FluidStack.EMPTY;
    private FluidStack overload = FluidStack.EMPTY;
    private int coolantTicks;
    private int overloadTicks;

    public TurretFluidState(TurretBlockEntity turret) {
        this.turret = turret;
    }

    /** 获取冷却槽中的流体副本。 */
    public FluidStack coolant() {
        return coolant.copy();
    }

    /** 获取过载槽中的流体副本。 */
    public FluidStack overload() {
        return overload.copy();
    }

    /** 获取包含过载液加成后的温度上限。 */
    public double maximumTemperature() {
        double maximum = TurretTemperature.getMaximumTemperature();
        TurretFluid definition = definition(overload);
        if (definition != null && definition.type() == TurretFluid.Type.OVERLOAD
                && !overload.isEmpty()) {
            maximum += definition.maximumTemperatureBonus();
        }
        return maximum;
    }

    /** 每 tick 消耗流体并处理冷却效果。 */
    public void tick() {
        boolean changed = tickCoolant();
        changed |= tickOverload();
        if (!turret.isOverloaded() && turret.getTemperature() >= maximumTemperature()) {
            TurretTemperature.set(turret, turret.getTemperature());
            changed = true;
        }
        if (changed) {
            turret.markStateChanged();
        }
    }

    /** 清空两个流体槽和对应计时器。 */
    public void clear() {
        coolant = FluidStack.EMPTY;
        overload = FluidStack.EMPTY;
        coolantTicks = 0;
        overloadTicks = 0;
        turret.markStateChanged();
    }

    /** 判断流体是否可以放入指定槽位。 */
    public boolean accepts(int tank, FluidStack stack) {
        if (stack == null || stack.isEmpty() || (tank != 0 && tank != 1)) {
            return false;
        }
        TurretFluid definition = definition(stack);
        if (definition == null) {
            return false;
        }
        boolean correctType = tank == 0
                ? definition.type() == TurretFluid.Type.CONSUMABLE_COOLANT
                || definition.type() == TurretFluid.Type.CIRCULATING_COOLANT
                : definition.type() == TurretFluid.Type.OVERLOAD;
        FluidStack current = tank == 0 ? coolant : overload;
        return correctType && (current.isEmpty() || current.isFluidEqual(stack));
    }

    /** 获取内部槽位中的流体，不创建副本。 */
    public FluidStack fluidInTank(int tank) {
        return tank == 0 ? coolant : overload;
    }

    /** 设置内部槽位中的流体并重置该槽位的消耗计时。 */
    public void setFluidInTank(int tank, FluidStack stack) {
        if (tank == 0) {
            coolant = stack == null ? FluidStack.EMPTY : stack;
            coolantTicks = 0;
        } else if (tank == 1) {
            overload = stack == null ? FluidStack.EMPTY : stack;
            overloadTicks = 0;
        }
        turret.markStateChanged();
    }

    /** 将流体状态写入炮台方块实体的 NBT。 */
    public void save(CompoundTag tag) {
        if (!coolant.isEmpty()) {
            tag.put(COOLANT_FLUID_TAG, coolant.writeToNBT(new CompoundTag()));
        }
        if (!overload.isEmpty()) {
            tag.put(OVERLOAD_FLUID_TAG, overload.writeToNBT(new CompoundTag()));
        }
        tag.putInt(COOLANT_TICKS_TAG, coolantTicks);
        tag.putInt(OVERLOAD_TICKS_TAG, overloadTicks);
    }

    /** 从炮台方块实体的 NBT 恢复流体状态。 */
    public void load(CompoundTag tag) {
        coolant = tag.contains(COOLANT_FLUID_TAG, Tag.TAG_COMPOUND)
                ? FluidStack.loadFluidStackFromNBT(tag.getCompound(COOLANT_FLUID_TAG))
                : FluidStack.EMPTY;
        overload = tag.contains(OVERLOAD_FLUID_TAG, Tag.TAG_COMPOUND)
                ? FluidStack.loadFluidStackFromNBT(tag.getCompound(OVERLOAD_FLUID_TAG))
                : FluidStack.EMPTY;
        coolant = clamp(coolant);
        overload = clamp(overload);
        coolantTicks = tag.getInt(COOLANT_TICKS_TAG);
        overloadTicks = tag.getInt(OVERLOAD_TICKS_TAG);
    }

    /** 处理消耗型和循环型冷却液。 */
    private boolean tickCoolant() {
        TurretFluid definition = definition(coolant);
        if (coolant.isEmpty() || definition == null || turret.isOverloaded()) {
            if (definition == null || definition.type() != TurretFluid.Type.CIRCULATING_COOLANT) {
                coolantTicks = 0;
            }
            return false;
        }
        if (definition.type() == TurretFluid.Type.CONSUMABLE_COOLANT
                && definition.temperaturePerMb() > 0.0D
                && turret.getTemperature() > 0.0D) {
            int required = (int) Math.ceil(turret.getTemperature() / definition.temperaturePerMb());
            int consumed = Math.min(required, coolant.getAmount());
            if (consumed <= 0) {
                return false;
            }
            coolant.shrink(consumed);
            turret.setTemperatureInternal(Math.max(TurretTemperature.INITIAL_TEMPERATURE,
                    turret.getTemperature() - consumed * definition.temperaturePerMb()));
            return true;
        }
        if (definition.type() != TurretFluid.Type.CIRCULATING_COOLANT) {
            coolantTicks = 0;
            return false;
        }
        coolantTicks++;
        if (coolantTicks < definition.coolingIntervalTicks()) {
            return false;
        }
        coolantTicks = 0;
        double previous = turret.getTemperature();
        double amount = coolant.getAmount() * definition.coolingCoefficient();
        turret.setTemperatureInternal(Math.max(TurretTemperature.INITIAL_TEMPERATURE,
                previous - amount));
        return previous != turret.getTemperature();
    }

    /** 处理过载液的周期性消耗。 */
    private boolean tickOverload() {
        TurretFluid definition = definition(overload);
        if (overload.isEmpty() || definition == null
                || definition.type() != TurretFluid.Type.OVERLOAD) {
            overloadTicks = 0;
            return false;
        }
        overloadTicks++;
        if (overloadTicks < definition.consumptionIntervalTicks()) {
            return false;
        }
        overloadTicks = 0;
        int consumed = Math.min(definition.consumptionAmountMb(), overload.getAmount());
        if (consumed <= 0) {
            return false;
        }
        overload.shrink(consumed);
        return true;
    }

    /** 获取流体定义。 */
    private static TurretFluid definition(FluidStack stack) {
        return stack == null || stack.isEmpty() ? null : TurretFluidManager.get(stack.getFluid());
    }

    /** 限制从 NBT 读取的流体数量不超过槽位容量。 */
    private static FluidStack clamp(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(stack, Mth.clamp(stack.getAmount(), 0, TurretBlockEntity.FLUID_CAPACITY));
    }
}
