package com.yueyingiop.payday.api.helper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws bitmap decimal digits from texture atlases. */
public final class TextureFontHelper {
    private TextureFontHelper() {
    }

    /** Draws a string of decimal digits and returns its rendered advance. */
    public static int drawDigits(GuiGraphics graphics, ResourceLocation texture,
                                 int textureWidth, int textureHeight, String text,
                                 int x, int y, int sourceX, int sourceY,
                                 int digitWidth, int digitHeight,
                                 int sourceStep, int renderStep) {
        for (int i = 0; i < text.length(); i++) {
            int digit = text.charAt(i) - '0';
            if (digit < 0 || digit > 9) {
                continue;
            }
            graphics.blit(texture, x + i * renderStep, y,
                    sourceX + digit * sourceStep, sourceY, digitWidth, digitHeight,
                    textureWidth, textureHeight);
        }
        return text.length() * renderStep;
    }

    /**
     * 计算纹理数字文本的渲染宽度。
     * @param text 对应参数。
     * @param renderStep 对应参数。
     * @return 对应的执行结果。
     */
    public static int digitAdvance(String text, int renderStep) {
        return text.length() * renderStep;
    }
}
