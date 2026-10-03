package com.yueyingiop.payday.item;

import com.yueyingiop.payday.api.PhoneUpgradeAction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FastColor;

import java.util.List;

/** A phone upgrade. Its data is kept on the item so recipes and future effects can use stable IDs. */
public class PhoneUpgradeItem extends Item {
    public enum Category { BASIC, SPECIAL }

    private final Category category;
    private final String series;
    private final int level;
    private final boolean prerequisite;
    private final String effectKey;
    private final PhoneUpgradeAction action;
    private final int textureColor;

    public PhoneUpgradeItem(Category category, String series, int level, boolean prerequisite, String effectKey,
                            PhoneUpgradeAction action) {
        this(category, series, level, prerequisite, effectKey, action,
                new Item.Properties().stacksTo(category == Category.SPECIAL ? 1 : 16),
                0xFFFFFFFF);
    }

    public PhoneUpgradeItem(Category category, String series, int level, boolean prerequisite, String effectKey,
                            PhoneUpgradeAction action, Item.Properties properties) {
        this(category, series, level, prerequisite, effectKey, action, properties, 0xFFFFFFFF);
    }

    /**
     * 创建带有显式物品贴图颜色的手机升级。颜色只影响客户端渲染，默认是原色。
     *
     * @param category 升级分类
     * @param series 升级系列
     * @param level 升级等级
     * @param prerequisite 是否需要前置升级
     * @param effectKey tooltip 效果键
     * @param action 升级效果
     * @param properties 物品属性
     * @param textureColor ARGB 颜色值
     */
    public PhoneUpgradeItem(Category category, String series, int level, boolean prerequisite, String effectKey,
                            PhoneUpgradeAction action, Item.Properties properties, int textureColor) {
        super(properties);
        this.category = category;
        this.series = series;
        this.level = level;
        this.prerequisite = prerequisite;
        this.effectKey = effectKey;
        this.action = action;
        this.textureColor = textureColor;
    }

    public Category category() { return category; }
    public String series() { return series; }
    public int level() { return level; }
    public boolean requiresPrerequisite() { return prerequisite; }
    public String effectKey() { return effectKey; }
    public PhoneUpgradeAction action() { return action; }

    /** 返回该升级物品的 ARGB 贴图颜色。*/
    public int textureColor() { return textureColor; }

    /**
     * 将 RGBA 分量转换为 Minecraft 使用的 ARGB 颜色。
     *
     * @param r 红色分量，范围 0~255
     * @param g 绿色分量，范围 0~255
     * @param b 蓝色分量，范围 0~255
     * @param a 不透明度分量，范围 0~255
     * @return ARGB 颜色
     */
    public static int textureColor(int r, int g, int b, int a) {
        return FastColor.ARGB32.color(clampColor(a), clampColor(r), clampColor(g), clampColor(b));
    }

    /**
     * 将 HSL 分量转换为 Minecraft 使用的 ARGB 颜色。
     * h 使用 0~360 度，s 和 l 使用 0~1。
     *
     * @param h 色相角度
     * @param s 饱和度
     * @param l 亮度
     * @return 不透明的 ARGB 颜色
     */
    public static int textureColor(double h, double s, double l) {
        double hue = ((h % 360.0D) + 360.0D) % 360.0D / 360.0D;
        double saturation = clampUnit(s);
        double lightness = clampUnit(l);
        double chroma = (1.0D - Math.abs(2.0D * lightness - 1.0D)) * saturation;
        double x = chroma * (1.0D - Math.abs((hue * 6.0D) % 2.0D - 1.0D));
        double m = lightness - chroma / 2.0D;
        double red;
        double green;
        double blue;
        double sector = hue * 6.0D;
        if (sector < 1.0D) {
            red = chroma; green = x; blue = 0.0D;
        } else if (sector < 2.0D) {
            red = x; green = chroma; blue = 0.0D;
        } else if (sector < 3.0D) {
            red = 0.0D; green = chroma; blue = x;
        } else if (sector < 4.0D) {
            red = 0.0D; green = x; blue = chroma;
        } else if (sector < 5.0D) {
            red = x; green = 0.0D; blue = chroma;
        } else {
            red = chroma; green = 0.0D; blue = x;
        }
        return textureColor((int) Math.round((red + m) * 255.0D),
                (int) Math.round((green + m) * 255.0D),
                (int) Math.round((blue + m) * 255.0D), 255);
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static double clampUnit(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.payday.upgrade_category." + category.name().toLowerCase())
                .withStyle(ChatFormatting.GRAY));
        if (category == Category.SPECIAL) {
            tooltip.add(Component.translatable("tooltip.payday.upgrade_series",
                    Component.translatable("tooltip.payday.upgrade_series_name." + series))
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.payday.upgrade_level", this.level)
                .withStyle(ChatFormatting.AQUA));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.payday.upgrade_effect_label")
                    .withStyle(ChatFormatting.GREEN));
            Component effect = Component.translatable("tooltip.payday.upgrade_effect." + effectKey);
            // Translation files may contain a literal `n, an escaped newline, or a real newline.
            for (String line : effect.getString().split("\\\\n|`n|\\r?\\n")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GREEN));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.payday.upgrade_effect_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

}
