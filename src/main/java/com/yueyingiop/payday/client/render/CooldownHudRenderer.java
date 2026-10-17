package com.yueyingiop.payday.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yueyingiop.payday.api.CooldownHud;
import com.yueyingiop.payday.api.CooldownHudClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

/** 绘制屏幕左上角的三种冷却 HUD。 */
public final class CooldownHudRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/cd.png");
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
        int width = width(type);
        int sourceX = switch (type) {
            case TIMER -> 0;
            case PROBABILITY -> 43;
            case TRIGGER -> 84;
        };
        graphics.blit(TEXTURE, x, y, sourceX, 0, width, HUD_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (entry.hud().texture() != null) {
            graphics.blit(entry.hud().texture(), x + 3, y + 3,
                    0, 0, 16, 16, 16, 16);
        }
        switch (type) {
            case TIMER -> renderTimer(graphics, entry, x, y);
            case PROBABILITY -> renderProbability(graphics, entry, x, y);
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
        drawCompactDigits(graphics, value, x + 22, y + 3, 17);
    }

    private static void renderProbability(GuiGraphics graphics,
                                          CooldownHudClientManager.VisibleHud entry,
                                          int x, int y) {
        String value = Integer.toString((int) Math.round(entry.hud().value()));
        drawCompactDigits(graphics, value, x + 20, y + 3, 11);
        if (Minecraft.getInstance().options.keyPlayerList.isDown()) {
            renderProbabilityName(graphics, entry.hud(), x, y);
        }
    }

    /** 按住 Tab 时在概率型 HUD 右下角显示属性名称。 */
    private static void renderProbabilityName(GuiGraphics graphics, CooldownHud hud,
                                              int x, int y) {
        Font font = Minecraft.getInstance().font;
        int maxWidth = PROBABILITY_WIDTH - 22;
        int maxHeight = HUD_HEIGHT - 11;
        int textWidth = font.width(hud.name());
        float scale = textWidth <= 0 ? 1.0F : Math.min(1.0F, maxWidth / (float) textWidth);
        int scaledWidth = Math.round(textWidth * scale);
        int scaledHeight = Math.round(font.lineHeight * scale);
        float drawX = x + 22.0F + (maxWidth - scaledWidth) / 2.0F;
        float drawY = y + 11.0F + Math.max(0.0F, (maxHeight - scaledHeight) / 2.0F);

        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, hud.name(), 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popPose();
    }

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
                cursor += DIGIT_WIDTH;
            } else if (character == '-') {
                if (cursor + DIGIT_WIDTH <= x + maxWidth) {
                    graphics.fill(cursor, y + 2, cursor + DIGIT_WIDTH, y + 3, 0xFFFFFFFF);
                }
                cursor += DIGIT_WIDTH;
            } else if (character == '.') {
                if (cursor < x + maxWidth) {
                    graphics.fill(cursor, y + DIGIT_HEIGHT - 1,
                            Math.min(cursor + 1, x + maxWidth), y + DIGIT_HEIGHT,
                            0xFFFFFFFF);
                }
                cursor++;
            }
        }
    }

    private static int width(CooldownHud.Type type) {
        return switch (type) {
            case TIMER -> TIMER_WIDTH;
            case PROBABILITY -> PROBABILITY_WIDTH;
            case TRIGGER -> TRIGGER_WIDTH;
        };
    }
}
