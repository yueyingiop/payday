package com.yueyingiop.payday.compat.jei;

import com.yueyingiop.payday.api.TurretFluid;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

/** JEI 中展示一条炮台流体信息的独立配方数据。 */
public final class TurretFluidJeiRecipe {
    private final TurretFluid definition;
    private final FluidStack fluidStack;

    public TurretFluidJeiRecipe(TurretFluid definition, FluidStack fluidStack) {
        this.definition = definition;
        this.fluidStack = fluidStack.copy();
    }

    public TurretFluid definition() {
        return definition;
    }

    public FluidStack fluidStack() {
        return fluidStack.copy();
    }

    /** 获取配方中显示的流体类型。 */
    public Component typeText() {
        return Component.translatable("jei.payday.turret_fluid.type",
                Component.translatable(definition.type().translationKey()));
    }

    /** 获取配方中显示的流体效果。 */
    public Component effectText() {
        return switch (definition.type()) {
            case CONSUMABLE_COOLANT -> Component.translatable(
                    "jei.payday.turret_fluid.consumable_effect",
                    formatNumber(definition.temperaturePerMb() <= 0.0D
                            ? Double.POSITIVE_INFINITY : 1.0D / definition.temperaturePerMb()));
            case CIRCULATING_COOLANT -> Component.translatable(
                    "jei.payday.turret_fluid.circulating_effect",
                    definition.coolingIntervalTicks(),
                    formatNumber(definition.coolingCoefficient()));
            case OVERLOAD -> Component.translatable(
                    "jei.payday.turret_fluid.overload_effect",
                    definition.consumptionIntervalTicks(),
                    definition.consumptionAmountMb(),
                    formatNumber(definition.maximumTemperatureBonus()));
        };
    }

    private static String formatNumber(double value) {
        if (!Double.isFinite(value)) {
            return "infinite";
        }
        String result = String.format(java.util.Locale.ROOT, "%.3f", value);
        while (result.contains(".") && result.endsWith("0")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
