package com.yueyingiop.payday.skill;

import com.yueyingiop.payday.PayDay;
import com.yueyingiop.payday.client.ClientSkillState;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Client-facing metadata and current rank for one skill node. */
public record SkillNode(int index, String categoryId, String seriesId,
                        Component name, int baseCost, Component baseEffect,
                        int masteryCost, Component masteryEffect) {
    private static final int COLOR_DEFAULT = 0xFFFFFFFF;
    private static final int COLOR_ACTIVE = 0xFF5555FF;
    private static final int COLOR_MASTERED = 0xFFAA00AA;

    SkillNode(int index, String categoryId, String seriesId, SkillData.Definition definition) {
        this(index, categoryId, seriesId, definition.name(), definition.baseCost(),
                definition.baseEffect(), definition.masteryCost(), definition.masteryEffect());
    }

    public int baseRank() {
        return rank() >= 1 ? 1 : 0;
    }

    public int masteryRank() {
        return rank() >= 2 ? 1 : 0;
    }

    private int rank() {
        return ClientSkillState.rank(categoryId, seriesId, index - 1);
    }

    public boolean isActivated() {
        return baseRank() > 0 || masteryRank() > 0;
    }

    public boolean isMastered() {
        return masteryRank() > 0;
    }

    public Component displayName(SkillSeries series) {
        return name;
    }

    public int textColor() {
        if (masteryRank() > 0) {
            return COLOR_MASTERED;
        }
        return baseRank() > 0 ? COLOR_ACTIVE : COLOR_DEFAULT;
    }

    public ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath(PayDay.MODID,
                "textures/gui/skill/generated/" + categoryId + "/" + seriesId + "_" + index + ".png");
    }
}
