package com.yueyingiop.payday.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/**
 * 描述一个可以显示在屏幕左上角的冷却 HUD。
 *
 * <p>对象本身只描述 HUD 的类型、图标和数值；实际显示、刷新和移除由
 * {@link CooldownHudManager} 负责。</p>
 */
public final class CooldownHud {
    /** 冷却 HUD 的显示类型。 */
    public enum Type {
        /** 显示剩余时间和冷却进度。 */
        TIMER,
        /** 显示永久存在的百分比数值。 */
        PROBABILITY,
        /** 触发后显示固定五秒。 */
        TRIGGER;

        /**
         * 从脚本传入的字符串解析类型。
         *
         * @param name 类型名称
         * @return 对应的类型
         * @throws IllegalArgumentException 类型名称未知时抛出
         */
        public static Type fromName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Cooldown HUD type must not be null");
            }
            return switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "timer", "timing", "计时", "计时型" -> TIMER;
                case "probability", "chance", "概率", "概率型" -> PROBABILITY;
                case "trigger", "触发", "触发型" -> TRIGGER;
                default -> throw new IllegalArgumentException(
                        "Unknown cooldown HUD type: " + name);
            };
        }
    }

    private final Type type;
    private final ResourceLocation texture;
    private final double value;
    private final Component name;

    private CooldownHud(Type type, ResourceLocation texture, double value, Component name) {
        this.type = type;
        this.texture = texture;
        this.value = value;
        this.name = name;
    }

    /**
     * 创建一个冷却 HUD 构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建一个已经解析好的冷却 HUD。
     *
     * @param type HUD 类型
     * @param texture HUD 图标纹理，可以为 {@code null}
     * @param value 类型对应的数值
     * @return 冷却 HUD
     */
    public static CooldownHud of(Type type, ResourceLocation texture, double value) {
        return new Builder().type(type).texture(texture).value(value).build();
    }

    /** 创建一个已经解析好的冷却 HUD。 */
    public static CooldownHud of(Type type, ResourceLocation texture, double value, Component name) {
        return new Builder().type(type).texture(texture).value(value).showName(name).build();
    }

    /**
     * 获取 HUD 类型。
     *
     * @return HUD 类型
     */
    public Type type() {
        return type;
    }

    /**
     * 获取 HUD 图标纹理。
     *
     * @return 图标纹理；未设置时为 {@code null}
     */
    public ResourceLocation texture() {
        return texture;
    }

    /**
     * 获取 HUD 数值。
     *
     * <p>计时型单位为秒，概率型单位为百分比，触发型忽略该数值。概率型也可以显示负数，
     * 用于属性减少类 HUD。</p>
     *
     * @return HUD 数值
     */
    public double value() {
        return value;
    }

    /** 获取概率型 HUD 按住 Tab 时显示的属性名称。 */
    public Component name() {
        return name;
    }

    /** 冷却 HUD 构造器。 */
    public static final class Builder {
        private Type type;
        private ResourceLocation texture;
        private double value;
        private Component name;

        private Builder() {
        }

        /**
         * 设置冷却 HUD 类型。
         *
         * @param type HUD 类型
         * @return 当前构造器
         */
        public Builder type(Type type) {
            if (type == null) {
                throw new IllegalArgumentException("Cooldown HUD type must not be null");
            }
            this.type = type;
            return this;
        }

        /**
         * 使用字符串设置冷却 HUD 类型，方便 KubeJS 调用。
         *
         * @param type HUD 类型名称
         * @return 当前构造器
         */
        public Builder type(String type) {
            return type(Type.fromName(type));
        }

        /**
         * 设置 HUD 图标纹理。
         *
         * <p>可以使用完整资源路径，例如
         * {@code payday:textures/item/phone.png}；也可以使用 KubeJS 常见的
         * {@code payday:item/phone} 简写。不设置时不绘制图标，等同于空白纹理。</p>
         *
         * @param texture 图标纹理
         * @return 当前构造器
         */
        public Builder texture(ResourceLocation texture) {
            this.texture = normalizeTexture(texture);
            return this;
        }

        /**
         * 使用字符串设置 HUD 图标纹理，方便 KubeJS 调用。
         *
         * @param texture 图标纹理资源路径；为空时使用空白纹理
         * @return 当前构造器
         */
        public Builder texture(String texture) {
            this.texture = parseTexture(texture);
            return this;
        }

        /** 设置概率型 HUD 的属性名称。未设置时使用“未知”。 */
        public Builder showName(Component name) {
            this.name = name;
            return this;
        }

        /** 使用普通文本设置概率型 HUD 的属性名称。 */
        public Builder showName(String name) {
            this.name = name == null || name.isBlank() ? null : Component.literal(name);
            return this;
        }

        /**
         * 设置 HUD 数值。
         *
         * @param value 计时型为秒，概率型为百分比，触发型忽略该值
         * @return 当前构造器
         */
        public Builder value(double value) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Cooldown HUD value must be finite");
            }
            this.value = value;
            return this;
        }

        /**
         * 构建冷却 HUD。
         *
         * @return 不可变的冷却 HUD
         */
        public CooldownHud build() {
            if (type == null) {
                throw new IllegalStateException("Cooldown HUD type has not been set");
            }
            double normalizedValue = switch (type) {
                case TIMER -> Math.max(0.0D, Math.min(999.9D, value));
                case PROBABILITY -> Math.max(-999.0D, Math.min(999.0D, value));
                case TRIGGER -> 0.0D;
            };
            Component normalizedName = name == null
                    ? Component.translatable("cooldown_hud.payday.unknown") : name;
            return new CooldownHud(type, texture, normalizedValue, normalizedName);
        }

        private static ResourceLocation parseTexture(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(value.trim());
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid cooldown HUD texture: " + value);
            }
            return normalizeTexture(parsed);
        }

        private static ResourceLocation normalizeTexture(ResourceLocation texture) {
            if (texture == null) {
                return null;
            }
            String path = texture.getPath();
            if (!path.startsWith("textures/")) {
                path = "textures/" + path;
            }
            if (!path.endsWith(".png")) {
                path += ".png";
            }
            return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);
        }
    }
}
