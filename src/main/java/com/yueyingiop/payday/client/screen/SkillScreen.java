package com.yueyingiop.payday.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.api.helper.TextureFontHelper;
import com.yueyingiop.payday.client.ClientSkillState;
import com.yueyingiop.payday.network.PayDayNetwork;
import com.yueyingiop.payday.network.RequestSkillStatePacket;
import com.yueyingiop.payday.network.SkillActionPacket;
import com.yueyingiop.payday.skill.NodeHit;
import com.yueyingiop.payday.skill.NodePosition;
import com.yueyingiop.payday.skill.SkillCategory;
import com.yueyingiop.payday.skill.SkillNode;
import com.yueyingiop.payday.skill.SkillSeries;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** 技能树界面。技能文本由本地数据定义，技能等级与技能点由服务端同步。 */
public final class SkillScreen extends Screen {
    public static final int GUI_WIDTH = 372;
    public static final int GUI_HEIGHT = 141;
    private static final int SHOW_WIDTH = 244;
    private static final int MESSAGE_WIDTH = 128;
    private static final int NAV_HEIGHT = 8;
    private static final int NODE_SIZE = 16;
    private static final int STATE_SIZE = 20;
    private static final int SERIES_WIDTH = 82;
    private static final int INFO_X = 3;
    private static final int INFO_WIDTH = 122;
    private static final int INFO_BOTTOM = 140;
    private static final float SKILL_NAME_SCALE = 0.68F;
    private static final int SKILL_TEXT_WIDTH = 59;
    private static final int SKILL_TEXT_HEIGHT = 15;
    private static final int DIGIT_WIDTH = 5;
    private static final int DIGIT_HEIGHT = 7;
    private static final int DIGIT_STEP = 6;
    private static final int COVER_WIDTH = 80;
    private static final int COVER_HEIGHT = 133;
    private static final int COVER_STEP = 82;
    private static final int COLOR_DEFAULT = 0xFFFFFFFF;
    private static final int COLOR_ACTIVE = 0xFF5555FF;

    private static final ResourceLocation SHOW = texture("skill/skill_show.png");
    private static final ResourceLocation MESSAGE = texture("skill/skill_message.png");
    private static final ResourceLocation NAV = texture("skill/skill_nav.png");
    private static final ResourceLocation STATE = texture("skill/skill_state.png");
    private static final ResourceLocation TEXT = texture("skill/skill_text.png");
    private static final ResourceLocation MOUSE = texture("skill/skill_mouse.png");
    private static final ResourceLocation COVER = texture("skill/skill_cover.png");

    /** 技能树中单个系列的六个槽位，从一阶到四阶。 */
    private static final List<NodePosition> NODE_POSITIONS = List.of(
            new NodePosition(32, 116),
            new NodePosition(18, 83),
            new NodePosition(46, 83),
            new NodePosition(18, 50),
            new NodePosition(46, 50),
            new NodePosition(32, 17)
    );

    private static final List<SkillCategory> CATEGORIES = List.of(
            SkillCategory.create("mastermind", "medical", "control", "sharpshooter"),
            SkillCategory.create("mobster", "shotgun", "heavy_armor", "ammo"),
            SkillCategory.create("technician", "engineering", "repair", "suppression"),
            SkillCategory.create("ghost", "ninjutsu", "evasion", "suppressor"),
            SkillCategory.create("fugitive", "quickdraw", "rebirth", "melee")
    );

    private int selectedCategory;
    private int selectedSeries;
    private int selectedNode;
    private long redZeroFlashUntil;

    public SkillScreen() {
        super(Component.translatable("screen.payday.skill"));
    }

    @Override
    protected void init() {
        super.init();
        PayDayNetwork.CHANNEL.sendToServer(new RequestSkillStatePacket());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float scale = getGuiScale();
        float panelLeft = getGuiLeft(scale);
        float panelTop = getGuiTop(scale);
        int logicalMouseX = Mth.floor((mouseX - panelLeft) / scale);
        int logicalMouseY = Mth.floor((mouseY - panelTop) / scale);

        graphics.pose().pushPose();
        graphics.pose().translate(panelLeft, panelTop, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        renderPanel(graphics, logicalMouseX, logicalMouseY);
        graphics.pose().popPose();
        renderHints(graphics);
        RenderSystem.disableBlend();
    }

    private void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(SHOW, 0, 0, 0, 0, SHOW_WIDTH, GUI_HEIGHT, SHOW_WIDTH, GUI_HEIGHT);
        graphics.blit(MESSAGE, SHOW_WIDTH, 0, 0, 0, MESSAGE_WIDTH, GUI_HEIGHT, MESSAGE_WIDTH, GUI_HEIGHT);

        renderNavigation(graphics);
        SkillCategory category = CATEGORIES.get(selectedCategory);
        renderNodes(graphics, category);
        renderMessage(graphics, category, mouseX, mouseY);
    }

    private void renderNavigation(GuiGraphics graphics) {
        int x = 0;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int width = categoryWidth(i);
            boolean selected = i == selectedCategory;
            tileNavigation(graphics, x, width, selected ? 0 : 8);
            drawSmallCentered(graphics, CATEGORIES.get(i).displayName(), x + width / 2.0F,
                    1.0F, selected ? 0xFF000000 : 0xFFFFFFFF);
            x += width;
        }
    }

    private void tileNavigation(GuiGraphics graphics, int x, int width, int sourceY) {
        int remaining = width;
        int tileX = x;
        while (remaining > 0) {
            int tileWidth = Math.min(16, remaining);
            graphics.blit(NAV, tileX, 0, 0, sourceY, tileWidth, NAV_HEIGHT, 16, 16);
            tileX += tileWidth;
            remaining -= tileWidth;
        }
    }

    private void renderNodes(GuiGraphics graphics, SkillCategory category) {
        for (int seriesIndex = 0; seriesIndex < category.series().size(); seriesIndex++) {
            SkillSeries series = category.series().get(seriesIndex);
            int seriesOffset = seriesIndex * SERIES_WIDTH;
            renderSeriesCover(graphics, series, seriesOffset);
            for (int nodeIndex = 0; nodeIndex < NODE_POSITIONS.size(); nodeIndex++) {
                NodePosition position = NODE_POSITIONS.get(nodeIndex);
                SkillNode node = series.node(nodeIndex);
                int x = position.x() + seriesOffset;
                int y = position.y();
                float brightness = node.isActivated() ? 1.0F : 0.45F;
                RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
                graphics.blit(node.icon(), x, y, 0, 0,
                        NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                if (!node.isActivated()) {
                    graphics.blit(STATE, x - 2, y - 2, 0, 0, STATE_SIZE, STATE_SIZE, 40, 20);
                } else if (node.isMastered()) {
                    graphics.blit(STATE, x - 2, y - 2, 20, 0, STATE_SIZE, STATE_SIZE, 40, 20);
                }
            }
        }
    }

    private void renderSeriesCover(GuiGraphics graphics, SkillSeries series, int seriesOffset) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int activationTier = series.activationTier();
        int sourceX = (4 - activationTier) * COVER_STEP;
        graphics.blit(COVER, seriesOffset, NAV_HEIGHT, sourceX, 0,
                COVER_WIDTH, COVER_HEIGHT, 326, COVER_HEIGHT);
        RenderSystem.disableBlend();
    }

    private void renderMessage(GuiGraphics graphics, SkillCategory category, int mouseX, int mouseY) {
        NodeHit hovered = findNode(mouseX, mouseY);
        int seriesIndex = hovered == null ? selectedSeries : hovered.seriesIndex();
        int nodeIndex = hovered == null ? selectedNode : hovered.nodeIndex();
        SkillSeries series = category.series().get(seriesIndex);
        SkillNode node = series.node(nodeIndex);
        int messageX = SHOW_WIDTH;
        int contentX = messageX + INFO_X;
        int contentRight = messageX + MESSAGE_WIDTH - INFO_X;

        Component pointsLabel = Component.translatable("skill.payday.remaining_points");
        drawClipped(graphics, pointsLabel, contentX, 15, INFO_WIDTH - 24, COLOR_DEFAULT);
        int points = remainingSkillPoints();
        drawTextureNumber(graphics, points, contentRight - numberWidth(points), 15);

        int nodeTextColor = node.textColor();
        drawScaledClipped(graphics, series.displayName(), contentX, 29,
                INFO_WIDTH, SKILL_NAME_SCALE, nodeTextColor);
        drawScaledClipped(graphics, node.displayName(series), contentX, 36,
                INFO_WIDTH, SKILL_NAME_SCALE, nodeTextColor);

        Component baseLabel = Component.translatable("skill.payday.base_label");
        drawCostLine(graphics, baseLabel, contentX, 45, node.baseRank(), node.baseCost());

        int effectY = drawWrapped(graphics, node.baseEffect(), contentX, 55,
                INFO_WIDTH, INFO_BOTTOM, node.baseRank() > 0 ? COLOR_ACTIVE : COLOR_DEFAULT);
        Component masteryLabel = Component.translatable("skill.payday.mastery_label");
        int masteryY = Math.min(effectY + 2, INFO_BOTTOM - 16);
        drawCostLine(graphics, masteryLabel, contentX, masteryY,
                node.masteryRank(), node.masteryCost());
        drawWrapped(graphics, node.masteryEffect(), contentX, masteryY + 10,
                INFO_WIDTH, INFO_BOTTOM, node.masteryRank() > 0 ? COLOR_ACTIVE : COLOR_DEFAULT);
    }

    private void drawCostLine(GuiGraphics graphics, Component label, int x, int y,
                              int rank, int cost) {
        Component costText = Component.literal(" " + cost).withStyle(ChatFormatting.BLUE);
        if (rank > 0) {
            costText = Component.literal(" ")
                    .append(Component.translatable("skill.payday.owned").withStyle(ChatFormatting.BLUE))
                    .withStyle(ChatFormatting.BLUE);
        }
        graphics.drawString(font, label.copy().append(costText), x, y, COLOR_DEFAULT, false);
    }

    private int drawTextureNumber(GuiGraphics graphics, int value, int x, int y) {
        String text = Integer.toString(Math.max(0, value));
        TextureFontHelper.drawDigits(graphics, TEXT, SKILL_TEXT_WIDTH, SKILL_TEXT_HEIGHT,
                text, x, y, 0, isRedZeroFlashing(value) ? 8 : 0,
                DIGIT_WIDTH, DIGIT_HEIGHT, DIGIT_STEP, DIGIT_STEP);
        return numberWidth(value);
    }

    private int numberWidth(int value) {
        return Math.max(1, Integer.toString(Math.max(0, value)).length() * DIGIT_STEP - 1);
    }

    private boolean isRedZeroFlashing(int value) {
        long now = Util.getMillis();
        return value == 0 && now < redZeroFlashUntil && (now / 100L) % 2L == 0L;
    }

    private NodeHit findNode(int mouseX, int mouseY) {
        if (mouseX < 0 || mouseX >= SHOW_WIDTH || mouseY < NAV_HEIGHT || mouseY >= GUI_HEIGHT) {
            return null;
        }
        for (int seriesIndex = 0; seriesIndex < CATEGORIES.get(selectedCategory).series().size(); seriesIndex++) {
            int seriesOffset = seriesIndex * SERIES_WIDTH;
            for (int nodeIndex = 0; nodeIndex < NODE_POSITIONS.size(); nodeIndex++) {
                NodePosition position = NODE_POSITIONS.get(nodeIndex);
                if (mouseX >= position.x() + seriesOffset
                        && mouseX < position.x() + seriesOffset + NODE_SIZE
                        && mouseY >= position.y() && mouseY < position.y() + NODE_SIZE) {
                    return new NodeHit(seriesIndex, nodeIndex);
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        float scale = getGuiScale();
        int logicalX = Mth.floor((mouseX - getGuiLeft(scale)) / scale);
        int logicalY = Mth.floor((mouseY - getGuiTop(scale)) / scale);
        if (button == 0 && logicalY < NAV_HEIGHT && logicalX >= 0 && logicalX < SHOW_WIDTH) {
            int category = categoryAt(logicalX);
            if (category >= 0) {
                selectedCategory = category;
                selectedSeries = 0;
                selectedNode = 0;
                return true;
            }
        }

        NodeHit node = findNode(logicalX, logicalY);
        if (node != null) {
            selectedSeries = node.seriesIndex();
            selectedNode = node.nodeIndex();
            SkillNode clickedNode = CATEGORIES.get(selectedCategory).series()
                    .get(node.seriesIndex()).node(node.nodeIndex());
            if (button == 0 && clickedNode.masteryRank() == 0 && ClientSkillState.skillPoints() == 0) {
                redZeroFlashUntil = Util.getMillis() + 700L;
            }
            PayDayNetwork.CHANNEL.sendToServer(new SkillActionPacket(
                    button == 0 ? SkillActionPacket.LEARN : SkillActionPacket.FORGET,
                    selectedCategory, selectedSeries, selectedNode));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int action = switch (keyCode) {
            case GLFW.GLFW_KEY_U -> SkillActionPacket.CONVERT_LEVEL;
            case GLFW.GLFW_KEY_P -> SkillActionPacket.CONVERT_ALL;
            case GLFW.GLFW_KEY_F -> SkillActionPacket.RESET_CATEGORY;
            case GLFW.GLFW_KEY_R -> SkillActionPacket.RESET_ALL;
            default -> -1;
        };
        if (action >= 0) {
            PayDayNetwork.CHANNEL.sendToServer(new SkillActionPacket(
                    action, selectedCategory, selectedSeries, selectedNode));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int categoryAt(int x) {
        int left = 0;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int width = categoryWidth(i);
            if (x >= left && x < left + width) {
                return i;
            }
            left += width;
        }
        return -1;
    }

    private int categoryWidth(int categoryIndex) {
        int baseWidth = SHOW_WIDTH / CATEGORIES.size();
        int remainder = SHOW_WIDTH % CATEGORIES.size();
        return baseWidth + (categoryIndex < remainder ? 1 : 0);
    }

    private void drawSmallCentered(GuiGraphics graphics, Component text, float centerX, float y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(0.68F, 0.68F, 1.0F);
        graphics.drawCenteredString(font, text.copy().withStyle(ChatFormatting.BOLD), 0, 0, color);
        graphics.pose().popPose();
    }

    private void drawClipped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        if (font.width(text) <= width) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private void drawScaledClipped(GuiGraphics graphics, Component text, int x, int y,
                                   int width, float scale, int color) {
        int maxUnscaledWidth = Math.max(1, Mth.floor(width / scale));
        Component clipped = font.width(text) <= maxUnscaledWidth
                ? text
                : Component.literal(font.plainSubstrByWidth(text.getString(), maxUnscaledWidth));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, clipped, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y,
                            int width, int bottom, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, width);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (y + font.lineHeight > bottom) {
                break;
            }
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight;
        }
        return y;
    }

    private int remainingSkillPoints() {
        return ClientSkillState.skillPoints();
    }

    private void renderHints(GuiGraphics graphics) {
        Component upgrade = Component.translatable("skill.payday.hint.upgrade");
        Component forget = Component.translatable("skill.payday.hint.forget");
        Component resetTree = Component.translatable("skill.payday.hint.reset_tree");
        Component resetAll = Component.translatable("skill.payday.hint.reset_all");
        Component convertLevel = Component.translatable("skill.payday.hint.convert_level");
        Component convertAll = Component.translatable("skill.payday.hint.convert_all");

        int firstLineWidth = 5 + 2 + font.width(upgrade) + 8 + 5 + 2 + font.width(forget);
        drawMouseHint(graphics, width - firstLineWidth - 4, 4, 8, upgrade,
                width - firstLineWidth - 4 + 5 + 2 + font.width(upgrade) + 8, 0, forget);
        drawRightAligned(graphics, resetTree.copy().append(Component.literal("  ")).append(resetAll), 16,
                COLOR_ACTIVE);
        drawRightAligned(graphics, convertLevel, 28, COLOR_ACTIVE);
        drawRightAligned(graphics, convertAll, 40, COLOR_ACTIVE);
    }

    private void drawMouseHint(GuiGraphics graphics, int x, int y, int firstIconU,
                               Component firstText, int secondIconX, int secondIconU,
                               Component secondText) {
        graphics.blit(MOUSE, x, y + 1, firstIconU, 0, 5, 7, 13, 7);
        graphics.drawString(font, firstText, x + 7, y, COLOR_DEFAULT, false);
        graphics.blit(MOUSE, secondIconX, y + 1, secondIconU, 0, 5, 7, 13, 7);
        graphics.drawString(font, secondText, secondIconX + 7, y, COLOR_DEFAULT, false);
    }

    private void drawRightAligned(GuiGraphics graphics, Component text, int y, int color) {
        graphics.drawString(font, text, width - font.width(text) - 4, y, color, false);
    }

    public float getGuiScale() {
        return Math.min(1.0F, Math.min((float) width / GUI_WIDTH, (float) height / GUI_HEIGHT));
    }

    public float getGuiLeft(float scale) {
        return (width - GUI_WIDTH * scale) * 0.5F;
    }

    public float getGuiTop(float scale) {
        return (height - GUI_HEIGHT * scale) * 0.5F;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(PayDay.MODID, "textures/gui/" + path);
    }

}
