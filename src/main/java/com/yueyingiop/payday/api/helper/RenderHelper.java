package com.yueyingiop.payday.api.helper;

import net.minecraft.util.Mth;

/** Shared screen-bound position and ratio conversions for HUD elements. */
public final class RenderHelper {
    private RenderHelper() {
    }

    /**
     * 根据比例计算横坐标。
     * @param ratio 比例。
     * @param screenWidth 屏幕宽度。
     * @param elementWidth 元素宽度。
     * @return 对应的执行结果。
     */
    public static int xFromRatio(double ratio, int screenWidth, int elementWidth) {
        return (int) Math.round(Mth.clamp(ratio, 0.0D, 1.0D)
                * Math.max(0, screenWidth - elementWidth));
    }

    /**
     * 根据比例计算纵坐标。
     * @param ratio 比例。
     * @param screenHeight 屏幕高度。
     * @param elementHeight 元素高度。
     * @return 对应的执行结果。
     */
    public static int yFromRatio(double ratio, int screenHeight, int elementHeight) {
        return (int) Math.round(Mth.clamp(ratio, 0.0D, 1.0D)
                * Math.max(0, screenHeight - elementHeight));
    }

    /**
     * 根据坐标计算横向比例。
     * @param x 横坐标。
     * @param screenWidth 屏幕宽度。
     * @param elementWidth 元素宽度。
     * @return 对应的执行结果。
     */
    public static double xRatioFromPosition(int x, int screenWidth, int elementWidth) {
        int maxX = Math.max(0, screenWidth - elementWidth);
        return maxX == 0 ? 0.0D : Mth.clamp(x, 0, maxX) / (double) maxX;
    }

    /**
     * 根据坐标计算纵向比例。
     * @param y 纵坐标。
     * @param screenHeight 屏幕高度。
     * @param elementHeight 元素高度。
     * @return 对应的执行结果。
     */
    public static double yRatioFromPosition(int y, int screenHeight, int elementHeight) {
        int maxY = Math.max(0, screenHeight - elementHeight);
        return maxY == 0 ? 0.0D : Mth.clamp(y, 0, maxY) / (double) maxY;
    }

    /**
     * 限制 HUD 横坐标在屏幕范围内。
     * @param x 横坐标。
     * @param screenWidth 屏幕宽度。
     * @param elementWidth 元素宽度。
     * @return 对应的执行结果。
     */
    public static int clampX(int x, int screenWidth, int elementWidth) {
        return Mth.clamp(x, 0, Math.max(0, screenWidth - elementWidth));
    }

    /**
     * 限制 HUD 纵坐标在屏幕范围内。
     * @param y 纵坐标。
     * @param screenHeight 屏幕高度。
     * @param elementHeight 元素高度。
     * @return 对应的执行结果。
     */
    public static int clampY(int y, int screenHeight, int elementHeight) {
        return Mth.clamp(y, 0, Math.max(0, screenHeight - elementHeight));
    }

    /**
     * 根据底边坐标计算 HUD 顶部坐标。
     * @param bottom 对应参数。
     * @param screenHeight 屏幕高度。
     * @param elementHeight 元素高度。
     * @return 对应的执行结果。
     */
    public static int topFromBottom(int bottom, int screenHeight, int elementHeight) {
        return clampY(screenHeight - Math.max(0, bottom) - elementHeight, screenHeight, elementHeight);
    }

    /**
     * 根据顶部坐标计算 HUD 底边坐标。
     * @param top 对应参数。
     * @param screenHeight 屏幕高度。
     * @param elementHeight 元素高度。
     * @return 对应的执行结果。
     */
    public static int bottomFromTop(int top, int screenHeight, int elementHeight) {
        return Math.max(0, screenHeight - clampY(top, screenHeight, elementHeight) - elementHeight);
    }
}
