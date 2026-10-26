package com.yueyingiop.payday.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yueyingiop.payday.api.CooldownHud;
import com.yueyingiop.payday.api.CooldownHudClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/** 绘制屏幕左上角的三种冷却 HUD。 */
public final class CooldownHudRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/cd.png");
    private static final ResourceLocation TEXTURE_TEXT = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/cd_text.png");
    private static final ResourceLocation TEXTURE_ATTRIBUTE = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/cd_attribute.png");
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 64;
    private static final int TIMER_WIDTH = 42;
    private static final int PROBABILITY_WIDTH = 40;
    private static final int TRIGGER_WIDTH = 22;
    private static final int HUD_HEIGHT = 22;
    private static final int GAP = 1;
    private static final int TIMER_DIGIT_X = 1;
    private static final int TIMER_DIGIT_Y = 46;
    private static final int DIGIT_WIDTH = 3;
    private static final int DIGIT_HEIGHT = 5;
    private static final int DIGIT_SOURCE_STEP = 6;
    private static final int DIGIT_SPACING = 1;

    private CooldownHudRenderer() {
    }

    /**
     * 绘制所有当前有效的冷却 HUD。
     *
     * @param graphics GUI 绘制上下文
     */
    public static void render(GuiGraphics graphics) {
        List<CooldownHudClientManager.VisibleHud> entries =
                CooldownHudClientManager.visible();
        if (entries.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, 450.0D);
        try {
            int x = 0;
            int y = 0;
            for (CooldownHudClientManager.VisibleHud entry : entries) {
                renderEntry(graphics, entry, x, y);
                y += HUD_HEIGHT + GAP;
            }
        } finally {
            graphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderEntry(GuiGraphics graphics,
                                    CooldownHudClientManager.VisibleHud entry,
                                    int x, int y) {
        CooldownHud.Type type = entry.hud().type();
        boolean textMode = type == CooldownHud.Type.PROBABILITY
                && Minecraft.getInstance().options.keyPlayerList.isDown();
        int width = width(type);
        int sourceX = switch (type) {
            case TIMER -> 0;
            case PROBABILITY -> 43;
            case TRIGGER -> 84;
        };
        if (textMode) {
            graphics.blit(TEXTURE_TEXT, x, y, 0, 0, PROBABILITY_WIDTH, HUD_HEIGHT,
                    PROBABILITY_WIDTH, HUD_HEIGHT);
        } else {
            graphics.blit(TEXTURE, x, y, sourceX, 0, width, HUD_HEIGHT,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        if (!textMode && entry.hud().texture() != null) {
            int sourceWidth = TEXTURE_ATTRIBUTE.equals(entry.hud().texture()) ? 64 : 16;
            graphics.blit(entry.hud().texture(), x + 3, y + 3,
                    entry.hud().textureU(), entry.hud().textureV(),
                    16, 16, sourceWidth, sourceWidth);
        }
        switch (type) {
            case TIMER -> renderTimer(graphics, entry, x, y);
            case PROBABILITY -> renderProbability(graphics, entry, x, y, textMode);
            case TRIGGER -> {
            }
        }
    }

    private static void renderTimer(GuiGraphics graphics,
                                    CooldownHudClientManager.VisibleHud entry,
                                    int x, int y) {
        int barHeight = Math.max(0, Math.min(16,
                Math.round(entry.remainingProgress() * 16.0F)));
        if (barHeight > 0) {
            graphics.fill(x + 20, y + 3 + (16 - barHeight),
                    x + 21, y + 19, 0xFFFFFFFF);
        }

        String value = String.format(Locale.ROOT, "%.1f", entry.remainingSeconds());
        drawRightAlignedDigits(graphics, value, x + 22, y + 3, 17);
    }

    private static void renderProbability(GuiGraphics graphics,
                                          CooldownHudClientManager.VisibleHud entry,
                                          int x, int y, boolean textMode) {
        if (textMode) {
            renderProbabilityName(graphics, entry.hud(), x, y);
        } else if (entry.hud().showValue()) {
            if (entry.hud().showPercentage()) {
                drawRightAlignedPercentage(graphics,
                        formatNumber(entry.hud().value() / 100.0D), x + 20, y + 3, 18);
            } else {
                String value = entry.hud().percentage()
                        ? Integer.toString((int) Math.round(entry.hud().value()))
                        : formatNumber(entry.hud().value());
                drawRightAlignedDigits(graphics, value, x + 20, y + 3, 18);
            }
        }
    }

    /** 按住 Tab 时在 cd_text 纹理的中间区域显示概率型 HUD 名称。 */
    private static void renderProbabilityName(GuiGraphics graphics, CooldownHud hud,
                                              int x, int y) {
        Font font = Minecraft.getInstance().font;
        int maxWidth = 34;
        int maxHeight = 16;
        String text = hud.name().getString();
        int textWidth = spacedTextWidth(font, text, 1);
        float scale = textWidth <= 0 ? 1.0F : Math.min(1.0F, maxWidth / (float) textWidth);
        int scaledWidth = Math.round(textWidth * scale);
        int scaledHeight = Math.round(font.lineHeight * scale);
        float drawX = x + 3.0F + (maxWidth - scaledWidth) / 2.0F;
        float drawY = y + 3.0F + Math.max(0.0F, (maxHeight - scaledHeight) / 2.0F);

        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        drawSpacedText(graphics, font, text, 0, 0, 0xFFFFFFFF, 1);
        graphics.pose().popPose();
    }

    /** 计算带字间距的文本宽度。 */
    private static int spacedTextWidth(Font font, String text, int spacing) {
        int width = 0;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            String character = new String(Character.toChars(codePoint));
            width += font.width(character) + spacing;
            index += Character.charCount(codePoint);
        }
        return Math.max(0, width - spacing);
    }

    /** 绘制带字间距的单行文本。 */
    private static void drawSpacedText(GuiGraphics graphics, Font font, String text,
                                       int x, int y, int color, int spacing) {
        int cursor = x;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            String character = new String(Character.toChars(codePoint));
            graphics.drawString(font, character, cursor, y, color, false);
            cursor += font.width(character) + spacing;
            index += Character.charCount(codePoint);
        }
    }

    /** Draws compact numeric glyphs from the supplied left edge, clipping to maxWidth. */
    private static void drawCompactDigits(GuiGraphics graphics, String value,
                                          int x, int y, int maxWidth) {
        int cursor = x;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character >= '0' && character <= '9') {
                if (cursor + DIGIT_WIDTH > x + maxWidth) {
                    break;
                }
                int digit = character - '0';
                graphics.blit(TEXTURE, cursor, y,
                        TIMER_DIGIT_X + digit * DIGIT_SOURCE_STEP, TIMER_DIGIT_Y,
                        DIGIT_WIDTH, DIGIT_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                cursor += DIGIT_WIDTH + DIGIT_SPACING;
            } else if (character == '-') {
                if (cursor + DIGIT_WIDTH <= x + maxWidth) {
                    graphics.fill(cursor, y + 2, cursor + DIGIT_WIDTH, y + 3, 0xFFFFFFFF);
                }
                cursor += DIGIT_WIDTH + DIGIT_SPACING;
            } else if (character == '.') {
                if (cursor < x + maxWidth) {
                    graphics.fill(cursor, y + DIGIT_HEIGHT - 1,
                            Math.min(cursor + 1, x + maxWidth), y + DIGIT_HEIGHT,
                            0xFFFFFFFF);
                }
                cursor += 1 + DIGIT_SPACING;
            }
        }
    }

    /** Draws compact numeric glyphs with their right edge aligned to maxWidth. */
    private static void drawRightAlignedDigits(GuiGraphics graphics, String value,
                                                int x, int y, int maxWidth) {
        int drawX = x + Math.max(0, maxWidth - compactDigitsWidth(value));
        drawCompactDigits(graphics, value, drawX, y, maxWidth - (drawX - x));
    }

    /** Draws a basis-point value followed by the percent glyph from cd.png. */
    private static void drawRightAlignedPercentage(GuiGraphics graphics, String value,
                                                   int x, int y, int maxWidth) {
        int valueWidth = compactDigitsWidth(value);
        int totalWidth = valueWidth + DIGIT_SPACING + 5;
        int drawX = x + Math.max(0, maxWidth - totalWidth);
        drawCompactDigits(graphics, value, drawX, y, Math.min(valueWidth, maxWidth));
        int percentX = drawX + valueWidth + DIGIT_SPACING;
        if (percentX + 5 <= x + maxWidth) {
            graphics.blit(TEXTURE, percentX, y, 1, 52, 5, 5,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    /** Calculates the visible width of the compact numeric glyph sequence. */
    private static int compactDigitsWidth(String value) {
        int width = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            width += character == '.' ? 1 : DIGIT_WIDTH;
            if (i + 1 < value.length()) {
                width += DIGIT_SPACING;
            }
        }
        return width;
    }

    /** 将属性原始值格式化为不带百分号的紧凑数字。 */
    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private static int width(CooldownHud.Type type) {
        return switch (type) {
            case TIMER -> TIMER_WIDTH;
            case PROBABILITY -> PROBABILITY_WIDTH;
            case TRIGGER -> TRIGGER_WIDTH;
        };
    }
}
