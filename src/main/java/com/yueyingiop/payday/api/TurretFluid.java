package com.yueyingiop.payday.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * 描述炮台可以使用的一种流体及其行为。
 *
 * <p>这个类只保存流体规则，不会注册新的 Forge 流体。流体必须已经存在于流体注册表中，
 * 例如 {@code minecraft:water} 或由其他模组、KubeJS 注册的流体。</p>
 */
public final class TurretFluid {
    /** 炮台流体的行为类型。 */
    public enum Type {
        /** 按温度消耗流体，消耗比例由 {@link TurretFluid#temperaturePerMb()} 决定。 */
        CONSUMABLE_COOLANT,
        /** 不消耗流体，按流体数量和系数定时降低温度。 */
        CIRCULATING_COOLANT,
        /** 提高温度上限，并按间隔消耗流体。 */
        OVERLOAD;

        /**
         * 从 JavaScript 或配置字符串解析流体类型。
         *
         * @param value 类型名称
         * @return 对应的类型
         */
        public static Type fromName(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Turret fluid type must not be null");
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "consumable", "consumable_coolant", "coolant", "消耗", "消耗型冷却液" ->
                        CONSUMABLE_COOLANT;
                case "circulating", "circulating_coolant", "循环", "循环型冷却液" ->
                        CIRCULATING_COOLANT;
                case "overload", "overload_fluid", "过载", "过载液" -> OVERLOAD;
                default -> throw new IllegalArgumentException("Unknown turret fluid type: " + value);
            };
        }

        /** 获取界面和 Jade 使用的翻译键。 */
        public String translationKey() {
            return switch (this) {
                case CONSUMABLE_COOLANT -> "turret_fluid.payday.consumable_coolant";
                case CIRCULATING_COOLANT -> "turret_fluid.payday.circulating_coolant";
                case OVERLOAD -> "turret_fluid.payday.overload";
            };
        }
    }

    private final ResourceLocation fluidId;
    private final Type type;
    private final double temperaturePerMb;
    private final double coolingCoefficient;
    private final int coolingIntervalTicks;
    private final double maximumTemperatureBonus;
    private final int consumptionIntervalTicks;
    private final int consumptionAmountMb;

    private TurretFluid(Builder builder) {
        this.fluidId = builder.fluidId;
        this.type = builder.type;
        this.temperaturePerMb = builder.temperaturePerMb;
        this.coolingCoefficient = builder.coolingCoefficient;
        this.coolingIntervalTicks = builder.coolingIntervalTicks;
        this.maximumTemperatureBonus = builder.maximumTemperatureBonus;
        this.consumptionIntervalTicks = builder.consumptionIntervalTicks;
        this.consumptionAmountMb = builder.consumptionAmountMb;
    }

    /** 创建炮台流体 Builder。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 获取 Forge 流体注册表标识。 */
    public ResourceLocation fluidId() {
        return fluidId;
    }

    /** 获取流体行为类型。 */
    public Type type() {
        return type;
    }

    /** 获取消耗型冷却液每 1 mB 抵消的温度。 */
    public double temperaturePerMb() {
        return temperaturePerMb;
    }

    /** 获取循环型冷却液每 1 mB 对应的降温系数。 */
    public double coolingCoefficient() {
        return coolingCoefficient;
    }

    /** 获取循环型冷却液的处理间隔，单位为游戏刻。 */
    public int coolingIntervalTicks() {
        return coolingIntervalTicks;
    }

    /** 获取过载液提供的温度上限加成。 */
    public double maximumTemperatureBonus() {
        return maximumTemperatureBonus;
    }

    /** 获取过载液的消耗间隔，单位为游戏刻。 */
    public int consumptionIntervalTicks() {
        return consumptionIntervalTicks;
    }

    /** 获取过载液每次消耗的数量，单位为 mB。 */
    public int consumptionAmountMb() {
        return consumptionAmountMb;
    }

    /**
     * 将当前定义注册到炮台流体管理器，方便 KubeJS 链式调用。
     *
     * @return 当前定义
     */
    public TurretFluid register() {
        TurretFluidManager.register(this);
        return this;
    }

    /** 炮台流体 Builder。 */
    public static final class Builder {
        private ResourceLocation fluidId;
        private Type type;
        private double temperaturePerMb;
        private double coolingCoefficient;
        private int coolingIntervalTicks = 20;
        private double maximumTemperatureBonus;
        private int consumptionIntervalTicks = 20;
        private int consumptionAmountMb = 1;

        private Builder() {
        }

        /** 设置流体注册表标识。 */
        public Builder fluid(ResourceLocation fluid) {
            if (fluid == null) {
                throw new IllegalArgumentException("Turret fluid id must not be null");
            }
            this.fluidId = fluid;
            return this;
        }

        /** 使用字符串设置流体注册表标识。 */
        public Builder fluid(String fluid) {
            ResourceLocation parsed = ResourceLocation.tryParse(fluid == null ? "" : fluid.trim());
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid turret fluid id: " + fluid);
            }
            return fluid(parsed);
        }

        /** 设置流体行为类型。 */
        public Builder type(Type type) {
            if (type == null) {
                throw new IllegalArgumentException("Turret fluid type must not be null");
            }
            this.type = type;
            return this;
        }

        /** 使用字符串设置流体行为类型，方便 KubeJS 调用。 */
        public Builder type(String type) {
            return type(Type.fromName(type));
        }

        /** 设置消耗型冷却液每 1 mB 抵消的温度。 */
        public Builder temperaturePerMb(double value) {
            this.temperaturePerMb = finiteNonNegative(value, "temperaturePerMb");
            return this;
        }

        /** 使用每多少 mB 抵消 1 点温度的形式设置消耗比例。 */
        public Builder mbPerTemperature(double value) {
            if (!Double.isFinite(value) || value <= 0.0D) {
                throw new IllegalArgumentException("mbPerTemperature must be positive");
            }
            this.temperaturePerMb = 1.0D / value;
            return this;
        }

        /** 设置循环型冷却液的降温系数。 */
        public Builder coolingCoefficient(double value) {
            this.coolingCoefficient = finiteNonNegative(value, "coolingCoefficient");
            return this;
        }

        /** 设置循环型冷却液的处理间隔。 */
        public Builder coolingInterval(int ticks) {
            this.coolingIntervalTicks = positive(ticks, "coolingInterval");
            return this;
        }

        /** 设置过载液提供的温度上限加成。 */
        public Builder maximumTemperatureBonus(double value) {
            this.maximumTemperatureBonus = finiteNonNegative(value, "maximumTemperatureBonus");
            return this;
        }

        /** 设置过载液的消耗间隔。 */
        public Builder consumptionInterval(int ticks) {
            this.consumptionIntervalTicks = positive(ticks, "consumptionInterval");
            return this;
        }

        /** 设置过载液每次消耗的数量。 */
        public Builder consumptionAmount(int amountMb) {
            this.consumptionAmountMb = positive(amountMb, "consumptionAmount");
            return this;
        }

        /** 构建流体定义。 */
        public TurretFluid build() {
            if (fluidId == null) {
                throw new IllegalStateException("Turret fluid id has not been set");
            }
            if (type == null) {
                throw new IllegalStateException("Turret fluid type has not been set");
            }
            return new TurretFluid(this);
        }

        private static double finiteNonNegative(double value, String name) {
            if (!Double.isFinite(value) || value < 0.0D) {
                throw new IllegalArgumentException(name + " must be finite and non-negative");
            }
            return value;
        }

        private static int positive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}
