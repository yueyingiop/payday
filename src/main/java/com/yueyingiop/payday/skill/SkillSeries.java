package com.yueyingiop.payday.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.stream.IntStream;

/** Client-facing state and metadata for one skill series. */
public final class SkillSeries {
    private final String categoryId;
    private final String id;
    private final Component displayName;
    private final List<SkillNode> nodes;

    SkillSeries(String categoryId, String id) {
        this.categoryId = categoryId;
        this.id = id;
        this.displayName = Component.translatable("skill.payday.series." + categoryId + "." + id);
        List<SkillData.Definition> definitions = SkillData.forSeries(categoryId, id);
        this.nodes = IntStream.range(0, definitions.size())
                .mapToObj(index -> new SkillNode(index + 1, categoryId, id, definitions.get(index)))
                .toList();
    }

    public Component displayName() {
        return displayName;
    }

    public SkillNode node(int index) {
        return nodes.get(Mth.clamp(index, 0, nodes.size() - 1));
    }

    public int activationTier() {
        int spent = spentPoints();
        if (tierAvailable(4, spent)) {
            return 4;
        }
        if (tierAvailable(3, spent)) {
            return 3;
        }
        if (tierAvailable(2, spent)) {
            return 2;
        }
        return 1;
    }

    private boolean tierAvailable(int tier, int spent) {
        if (tier == 1) {
            return true;
        }
        int previousStart = tier == 2 ? 0 : tier == 3 ? 1 : 3;
        int previousEnd = tier == 2 ? 0 : tier == 3 ? 2 : 4;
        boolean previousLearned = false;
        for (int index = previousStart; index <= previousEnd; index++) {
            if (node(index).isActivated()) {
                previousLearned = true;
                break;
            }
        }
        int requiredSpent = tier == 2 ? 1 : tier == 3 ? 3 : 16;
        return previousLearned && spent >= requiredSpent;
    }

    private int spentPoints() {
        int spent = 0;
        for (SkillNode node : nodes) {
            if (node.baseRank() > 0) {
                spent += node.baseCost();
            }
            if (node.masteryRank() > 0) {
                spent += node.masteryCost();
            }
        }
        return spent;
    }
}
