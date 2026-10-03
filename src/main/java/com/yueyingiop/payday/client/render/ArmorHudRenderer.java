package com.yueyingiop.payday.client.render;

import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.item.ArmorSlotItem;
import com.yueyingiop.payday.api.ArmorModifier;
import com.yueyingiop.payday.api.helper.CurioHelper;
import com.yueyingiop.payday.api.helper.TextureFontHelper;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.yueyingiop.payday.Config;

import java.util.ArrayList;
import java.util.List;

/** Renders the three armor-slot rows beside the vanilla health HUD. */
public final class ArmorHudRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "payday", "textures/gui/armor_value.png");

    private static final int TEXTURE_SIZE = 128;
    private static final int ICON_SIZE = 9;
    private static final int ICON_SOURCE_STEP = 10;
    private static final int ICON_RENDER_STEP = 8;
    private static final int ICON_U = 1;
    private static final int ICON_V = 1;

    private static final int DIGIT_WIDTH = 5;
    private static final int DIGIT_HEIGHT = 7;
    private static final int DIGIT_SOURCE_STEP = 6;
    private static final int DIGIT_RENDER_STEP = 4;
    private static final int DIGIT_U = 2;
    private static final int DIGIT_V = 12;
    private static final int DIGIT_ROW_STEP = 8;

    private static final int ICON_PERMANENT = 0;
    private static final int ICON_IMPACT = 1;
    private static final int ICON_ADAPTIVE = 2;
    private static final int ICON_RESISTANCE = 3;
    private static final int ICON_EMPTY = 4;

    private ArmorHudRenderer() {
    }

    /**
     * 绘制对应内容。
     * @param graphics 图形渲染上下文。
     * @param player 玩家。
     */
    public static void render(GuiGraphics graphics, Player player) {
        // 客户端也重建手机提供的护甲上下文，保证 HUD 使用升级后的护甲上限。
        PhoneItem.refreshArmorSlotModifiers(player);
        List<SlotStats> slots = collectSlots(player);
        if (slots.isEmpty()) {
            return;
        }

        int healthY = healthY(graphics);
        int anchorY = player.getArmorValue() > 0 ? armorY(graphics, player, healthY) : healthY;
        if (Config.COMPACT_HUD.get()) {
            renderCompact(graphics, slots, anchorY);
        } else {
            renderDetailed(graphics, slots, anchorY);
        }
    }

    /**
     * 绘制紧凑模式下的护甲状态。
     * @param graphics 图形渲染上下文。
     * @param slots 对应参数。
     * @param anchorY 对应参数。
     */
    private static void renderCompact(GuiGraphics graphics, List<SlotStats> slots, int anchorY) {
        Stats[] stats = collectStats(slots);
        int row = 0;

        if (stats[ArmorSlotItem.Kind.IMPACT.ordinal()].present) {
            Stats value = stats[ArmorSlotItem.Kind.IMPACT.ordinal()];
            renderLine(graphics, value.kind, value.current, value.max, value.permanent,
                    graphics.guiWidth(), anchorY - 10 - row++ * 10);
        }
        if (stats[ArmorSlotItem.Kind.ADAPTIVE.ordinal()].present) {
            Stats value = stats[ArmorSlotItem.Kind.ADAPTIVE.ordinal()];
            renderLine(graphics, value.kind, value.current, value.max, value.permanent,
                    graphics.guiWidth(), anchorY - 10 - row++ * 10);
        }
        if (stats[ArmorSlotItem.Kind.RESISTANCE.ordinal()].present) {
            Stats value = stats[ArmorSlotItem.Kind.RESISTANCE.ordinal()];
            renderLine(graphics, value.kind, value.current, value.max, value.permanent,
                    graphics.guiWidth(), anchorY - 10 - row * 10);
        }
    }

    /**
     * 绘制详细模式下的护甲状态。
     * @param graphics 图形渲染上下文。
     * @param slots 对应参数。
     * @param anchorY 对应参数。
     */
    private static void renderDetailed(GuiGraphics graphics, List<SlotStats> slots, int anchorY) {
        for (int i = 0; i < slots.size(); i++) {
            SlotStats slot = slots.get(i);
            renderLine(graphics, slot.kind, slot.current, slot.max, slot.permanent,
                    graphics.guiWidth(), anchorY - 10 - i * 10);
        }
    }

    /**
     * 收集玩家已装备的护甲槽位。
     * @param player 玩家。
     * @return 对应的执行结果。
     */
    private static List<SlotStats> collectSlots(Player player) {
        List<SlotStats> result = new ArrayList<>();
        for (ItemStack armorStack : CurioHelper.stacks(player, CurioHelper.ARMOR_SLOT)) {
                        if (!(armorStack.getItem() instanceof ArmorCurioItem armorCurio)) {
                            continue;
                        }

                        for (ItemStack upgradeStack : armorCurio.getUpgradeStacks(armorStack)) {
                            if (!(upgradeStack.getItem() instanceof ArmorSlotItem armorSlot)) {
                                continue;
                            }
                            for (int definitionIndex = 0;
                                 definitionIndex < armorSlot.armorDefinitions().size(); definitionIndex++) {
                                double maximum = ArmorModifier.effectiveMax(player, armorStack,
                                        armorSlot, upgradeStack, definitionIndex);
                                result.add(new SlotStats(armorSlot.kind(definitionIndex),
                                        maximum,
                                        ArmorModifier.current(player, armorStack, armorSlot,
                                                upgradeStack, definitionIndex),
                                        armorSlot.permanent(upgradeStack, definitionIndex)));
                            }
                        }
        }
        return result;
    }

    /**
     * 按护甲类型汇总护甲数值。
     * @param slots 对应参数。
     * @return 对应的执行结果。
     */
    private static Stats[] collectStats(List<SlotStats> slots) {
        Stats[] result = {new Stats(), new Stats(), new Stats()};
        for (SlotStats slot : slots) {
            Stats stats = result[slot.kind.ordinal()];
            stats.kind = slot.kind;
            stats.present = true;
            stats.max += slot.max;
            stats.current += Mth.clamp(slot.current, 0.0D, slot.max);
            if (slot.kind == ArmorSlotItem.Kind.IMPACT || slot.kind == ArmorSlotItem.Kind.RESISTANCE) {
                stats.permanent += Mth.clamp(slot.permanent, 0.0D, slot.max);
            }
        }
        return result;
    }

    /**
     * 绘制一种护甲类型的数值行。
     * @param graphics 图形渲染上下文。
     * @param kind 护甲类型。
     * @param currentValue 对应参数。
     * @param maxValue 对应参数。
     * @param permanentValue 对应参数。
     * @param screenWidth 屏幕宽度。
     * @param y 纵坐标。
     */
    private static void renderLine(GuiGraphics graphics, ArmorSlotItem.Kind kind,
                                   double currentValue, double maxValue, double permanentValue,
                                   int screenWidth, int y) {
        int current = integerValue(currentValue);
        int max = integerValue(maxValue);
        int permanent = integerValue(permanentValue);
        int iconX = screenWidth / 2 - 91;

        int currentRow = switch (kind) {
            case IMPACT -> 2;
            case ADAPTIVE -> 3;
            case RESISTANCE -> 4;
        };
        int currentWidth = drawNumberWidth(current);
        int maxWidth = drawNumberWidth(max);
        int permanentWidth = kind == ArmorSlotItem.Kind.RESISTANCE ? drawNumberWidth(permanent) : 0;
        int labelWidth = kind == ArmorSlotItem.Kind.RESISTANCE
                ? permanentWidth + DIGIT_RENDER_STEP + currentWidth + DIGIT_RENDER_STEP
                + maxWidth + DIGIT_WIDTH
                : currentWidth + DIGIT_RENDER_STEP + maxWidth + DIGIT_WIDTH;
        int labelX = iconX - labelWidth;
        if (labelX < 2) {
            int shift = 2 - labelX;
            labelX += shift;
            iconX += shift;
        }

        int textX = labelX;
        if (kind == ArmorSlotItem.Kind.RESISTANCE) {
            textX += drawNumber(graphics, permanent, textX, y + 1, 1);
            textX += drawSeparator(graphics, textX, y + 1, 1);
        }
        textX += drawNumber(graphics, current, textX, y + 1, currentRow);
        textX += drawSeparator(graphics, textX, y + 1, currentRow);
        textX += drawNumber(graphics, max, textX, y + 1, 0);
        drawSeparator(graphics, textX, y + 1, 0);

        int permanentIcons = kind == ArmorSlotItem.Kind.RESISTANCE
                ? iconCount(permanentValue, maxValue)
                : 0;
        int currentIcons = Math.min(10 - permanentIcons, iconCount(currentValue, maxValue));
        int emptyIcons = 10 - permanentIcons - currentIcons;
        int filledIcon = switch (kind) {
            case IMPACT -> ICON_IMPACT;
            case ADAPTIVE -> ICON_ADAPTIVE;
            case RESISTANCE -> ICON_RESISTANCE;
        };

        for (int i = 0; i < permanentIcons; i++) {
            drawIcon(graphics, iconX + i * ICON_RENDER_STEP, y, ICON_PERMANENT);
        }
        for (int i = 0; i < currentIcons; i++) {
            drawIcon(graphics, iconX + (permanentIcons + i) * ICON_RENDER_STEP, y, filledIcon);
        }
        for (int i = 0; i < emptyIcons; i++) {
            drawIcon(graphics, iconX + (permanentIcons + currentIcons + i) * ICON_RENDER_STEP, y, ICON_EMPTY);
        }
    }

    /**
     * 绘制护甲状态图标。
     * @param graphics 图形渲染上下文。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param iconIndex 对应参数。
     */
    private static void drawIcon(GuiGraphics graphics, int x, int y, int iconIndex) {
        graphics.blit(TEXTURE, x, y, ICON_U + iconIndex * ICON_SOURCE_STEP, ICON_V,
                ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * 绘制纹理数字并返回占用宽度。
     * @param graphics 图形渲染上下文。
     * @param value 数值。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param row 纹理行索引。
     * @return 对应的执行结果。
     */
    private static int drawNumber(GuiGraphics graphics, int value, int x, int y, int row) {
        String text = Integer.toString(Math.max(0, value));
        return TextureFontHelper.drawDigits(graphics, TEXTURE, TEXTURE_SIZE, TEXTURE_SIZE, text,
                x, y, DIGIT_U, DIGIT_V + row * DIGIT_ROW_STEP, DIGIT_WIDTH, DIGIT_HEIGHT,
                DIGIT_SOURCE_STEP, DIGIT_RENDER_STEP);
    }

    /**
     * 绘制护甲数值分隔符。
     * @param graphics 图形渲染上下文。
     * @param x 横坐标。
     * @param y 纵坐标。
     * @param row 纹理行索引。
     * @return 对应的执行结果。
     */
    private static int drawSeparator(GuiGraphics graphics, int x, int y, int row) {
        graphics.blit(TEXTURE, x, y, DIGIT_U + 10 * DIGIT_SOURCE_STEP, DIGIT_V + row * DIGIT_ROW_STEP,
                DIGIT_WIDTH, DIGIT_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        return DIGIT_RENDER_STEP;
    }

    /**
     * 计算纹理数字的显示宽度。
     * @param value 数值。
     * @return 对应的执行结果。
     */
    private static int drawNumberWidth(int value) {
        return TextureFontHelper.digitAdvance(Integer.toString(Math.max(0, value)), DIGIT_RENDER_STEP);
    }

    /**
     * 根据护甲比例计算图标数量。
     * @param value 数值。
     * @param max 对应参数。
     * @return 对应的执行结果。
     */
    private static int iconCount(double value, double max) {
        if (max <= 0.0D) {
            return 0;
        }
        if (value <= 0.0D) {
            return 0;
        }
        return Mth.clamp((int) Math.floor(value / max * 10.0D + 1.0E-6D), 0, 10);
    }

    /**
     * 将护甲数值转换为整数显示。
     * @param value 数值。
     * @return 对应的执行结果。
     */
    private static int integerValue(double value) {
        return Math.max(0, (int) Math.floor(value + 1.0E-6D));
    }

    /**
     * 计算生命值 HUD 的纵坐标。
     * @param graphics 图形渲染上下文。
     * @return 对应的执行结果。
     */
    private static int healthY(GuiGraphics graphics) {
        return graphics.guiHeight() - 39;
    }

    /**
     * 根据生命值 HUD 计算护甲 HUD 纵坐标。
     * @param graphics 图形渲染上下文。
     * @param player 玩家。
     * @param healthY 对应参数。
     * @return 对应的执行结果。
     */
    private static int armorY(GuiGraphics graphics, Player player, int healthY) {
        float maxHealth = Math.max(player.getMaxHealth(), player.getHealth());
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int healthRows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(12 - healthRows, 3);
        return healthY - (healthRows - 1) * rowHeight - 10;
    }

    private static final class Stats {
        private ArmorSlotItem.Kind kind;
        private boolean present;
        private double max;
        private double current;
        private double permanent;
    }

    private static final class SlotStats {
        private final ArmorSlotItem.Kind kind;
        private final double max;
        private final double current;
        private final double permanent;

        private SlotStats(ArmorSlotItem.Kind kind, double max, double current, double permanent) {
            this.kind = kind;
            this.max = max;
            this.current = current;
            this.permanent = permanent;
        }
    }
}
