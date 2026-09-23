package com.yueyingiop.payday.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yueyingiop.payday.Config;
import com.yueyingiop.payday.api.helper.RenderHelper;
import com.yueyingiop.payday.api.helper.TextureFontHelper;
import com.yueyingiop.payday.exposure.ExposureRisk;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** 绘制物品栏左侧的暴露风险 HUD，以及 HUD 配置界面的预览。 */
public final class ExposureRiskHudRenderer {
    public static final int HUD_WIDTH = 13;
    public static final int HUD_HEIGHT = 91;
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 128;
    private static final int TEXTURE_X = 10;
    private static final int TEXTURE_Y = 30;
    private static final int DIGIT_X = 2;
    private static final int DIGIT_Y = 2;
    private static final int DIGIT_WIDTH = 5;
    private static final int DIGIT_HEIGHT = 7;
    private static final int DIGIT_SOURCE_STEP = 6;
    private static final int DIGIT_RENDER_STEP = 4;
    private static final int SAFE_ROW_Y = 9;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("payday", "textures/gui/exposure_risk.png");

    private ExposureRiskHudRenderer() {
    }

    /**
     * 在物品栏界面中绘制 HUD
     * @param graphics 图形渲染上下文
     * @param player 玩家
     */
    public static void render(GuiGraphics graphics, Player player) {
        ensureDefaultPosition(graphics.guiWidth(), graphics.guiHeight());
        int x = RenderHelper.xFromRatio(Config.EXPOSURE_RISK_HUD_X_RATIO.get(),
                graphics.guiWidth(), HUD_WIDTH);
        int y = RenderHelper.yFromRatio(Config.EXPOSURE_RISK_HUD_Y_RATIO.get(),
                graphics.guiHeight(), HUD_HEIGHT);
        renderAt(graphics, x, y, ExposureRisk.get(player), false);
    }

    /**
     * 绘制配置界面预览
     * @param graphics 图形渲染上下文
     * @param player 玩家（可为 {@code null}，为 null 时使用默认暴露风险值 30）
     * @param x X 坐标
     * @param y Y 坐标
     */
    public static void renderPreview(GuiGraphics graphics, Player player, int x, int y) {
        renderAt(graphics, x, y, player == null ? 30 : ExposureRisk.get(player), true);
    }

    /**
     * 在指定位置绘制 HUD
     * @param graphics 图形渲染上下文
     * @param x X 坐标
     * @param y Y 坐标
     * @param risk 暴露风险值
     * @param configurationMode 配置模式标志
     */
    private static void renderAt(GuiGraphics graphics, int x, int y, int risk, boolean configurationMode) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(TEXTURE, x, y, TEXTURE_X, TEXTURE_Y, HUD_WIDTH, HUD_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int clampedRisk = Mth.clamp(risk, 0, 75);
        int filled = clampedRisk;
        // 从提供的 3x75 填充条底部进行裁剪，使进度条向上增长。
        int barX = x + 5;
        int barBottom = y + 79;
        if (filled > 0) {
            graphics.blit(
                TEXTURE, 
                barX, barBottom - filled,
                27, 109 - filled, 
                3, filled, 
                TEXTURE_WIDTH, TEXTURE_HEIGHT
            );
        }

        // 纹理坐标 (12, 110) 是 9x9 的风险数值框，相对于 HUD 纹理起点 (10, 30)。
        int barColor = clampedRisk <= 30 ? 0xFF0056FF : 0xFFDA0000;
        graphics.fill(x + 2, y + 80, x + 11, y + 89, barColor);
        // 5x7 的数字字形在纹理 (12, 110) 的 9x9 数值框内垂直居中。
        renderRiskDigits(graphics, x + 2, y + 81, clampedRisk, clampedRisk <= 30);
        if (configurationMode) {
            graphics.fill(x, y, x + HUD_WIDTH, y + HUD_HEIGHT, 0x6600FF00);
            graphics.renderOutline(x, y, HUD_WIDTH, HUD_HEIGHT, 0xFF55FF55);
        }
    }

    /**
     * 绘制暴露风险值
     * @param graphics 图形渲染上下文
     * @param x X 坐标
     * @param y Y 坐标
     * @param risk 暴露风险值
     * @param safe 是否处于安全状态（决定使用哪一行数字纹理）
     */
    private static void renderRiskDigits(GuiGraphics graphics, int x, int y, int risk, boolean safe) {
        String value = String.format(java.util.Locale.ROOT, "%02d", Mth.clamp(risk, 0, 99));
        int sourceY = safe ? DIGIT_Y + SAFE_ROW_Y : DIGIT_Y;
        TextureFontHelper.drawDigits(graphics, TEXTURE, TEXTURE_WIDTH, TEXTURE_HEIGHT, value,
                x, y, DIGIT_X, sourceY, DIGIT_WIDTH, DIGIT_HEIGHT,
                DIGIT_SOURCE_STEP, DIGIT_RENDER_STEP);
    }

    /**
     * 将 HUD 初始化配置到标准物品栏左侧，此操作仅执行一次。
     * <p>
     * 如果尚未迁移过位置配置，则根据屏幕尺寸计算默认位置并保存。
     * </p>
     *
     * @param screenWidth  屏幕宽度
     * @param screenHeight 屏幕高度
     */
    public static void ensureDefaultPosition(int screenWidth, int screenHeight) {
        if (Config.EXPOSURE_RISK_HUD_POSITION_MIGRATED.get()) {
            return;
        }
        int inventoryLeft = (screenWidth - 176) / 2;
        int inventoryTop = (screenHeight - 166) / 2;
        int x = Math.max(0, inventoryLeft - HUD_WIDTH - 4);
        int y = Mth.clamp(inventoryTop + 38, 0, Math.max(0, screenHeight - HUD_HEIGHT));
        Config.EXPOSURE_RISK_HUD_X_RATIO.set(
            RenderHelper.xRatioFromPosition(x, screenWidth, HUD_WIDTH)
        );
        Config.EXPOSURE_RISK_HUD_Y_RATIO.set(
            RenderHelper.yRatioFromPosition(y, screenHeight, HUD_HEIGHT)
        );
        Config.EXPOSURE_RISK_HUD_POSITION_MIGRATED.set(true);
        Config.CLIENT_SPEC.save();
    }

}
