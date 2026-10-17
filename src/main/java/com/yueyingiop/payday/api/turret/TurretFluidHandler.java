package com.yueyingiop.payday.api.turret;

import com.yueyingiop.payday.block.TurretBlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** 炮台双流体槽的 Forge 流体能力实现。 */
public final class TurretFluidHandler implements IFluidHandler {
    private final TurretBlockEntity turret;
    private final int fixedTank;

    public TurretFluidHandler(TurretBlockEntity turret) {
        this(turret, -1);
    }

    public TurretFluidHandler(TurretBlockEntity turret, int fixedTank) {
        this.turret = turret;
        this.fixedTank = fixedTank;
    }

    /** 返回可见流体槽数量。 */
    @Override
    public int getTanks() {
        return fixedTank < 0 ? 2 : 1;
    }

    /** 返回指定流体槽中的内容。 */
    @Override
    public FluidStack getFluidInTank(int tank) {
        int actualTank = actualTank(tank);
        return actualTank >= 0 && actualTank < 2
                ? turret.fluidInTank(actualTank).copy() : FluidStack.EMPTY;
    }

    /** 返回流体槽容量。 */
    @Override
    public int getTankCapacity(int tank) {
        int actualTank = actualTank(tank);
        return actualTank >= 0 && actualTank < 2 ? TurretBlockEntity.FLUID_CAPACITY : 0;
    }

    /** 判断流体是否适合放入指定槽位。 */
    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return turret.acceptsFluid(actualTank(tank), stack);
    }

    /** 向可用槽位填充流体。 */
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        for (int tank = 0; tank < getTanks(); tank++) {
            int actualTank = actualTank(tank);
            if (!turret.acceptsFluid(actualTank, resource)) {
                continue;
            }
            FluidStack current = turret.fluidInTank(actualTank);
            int amount = Math.min(resource.getAmount(),
                    TurretBlockEntity.FLUID_CAPACITY - current.getAmount());
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

    /** 按流体类型从槽位中抽取流体。 */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < getTanks(); tank++) {
            int actualTank = actualTank(tank);
            FluidStack current = turret.fluidInTank(actualTank);
            if (!current.isEmpty() && current.isFluidEqual(resource)) {
                return drainTank(actualTank, Math.min(resource.getAmount(), current.getAmount()), action);
            }
        }
        return FluidStack.EMPTY;
    }

    /** 从第一个非空槽位抽取指定数量的流体。 */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < getTanks(); tank++) {
            int actualTank = actualTank(tank);
            if (!turret.fluidInTank(actualTank).isEmpty()) {
                return drainTank(actualTank, maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }

    /** 执行单个槽位的流体抽取。 */
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

    /** 将外部槽位编号转换为炮台真实槽位编号。 */
    private int actualTank(int tank) {
        return fixedTank < 0 ? tank : tank == 0 ? fixedTank : -1;
    }
}
