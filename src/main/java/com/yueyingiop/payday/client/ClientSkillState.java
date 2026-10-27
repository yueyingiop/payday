package com.yueyingiop.payday.client;

import com.yueyingiop.payday.network.SyncSkillStatePacket;
import com.yueyingiop.payday.skill.SkillProgress;

/** 客户端保存服务端同步的技能界面状态。 */
public final class ClientSkillState {
    private static int skillPoints;
    private static final byte[] ranks = new byte[SkillProgress.SLOT_COUNT];

    private ClientSkillState() {
    }

    public static void apply(SyncSkillStatePacket packet) {
        skillPoints = Math.max(0, packet.skillPoints());
        java.util.Arrays.fill(ranks, (byte) 0);
        System.arraycopy(packet.ranks(), 0, ranks, 0, Math.min(ranks.length, packet.ranks().length));
    }

    public static int skillPoints() {
        return skillPoints;
    }

    public static int rank(String categoryId, String seriesId, int nodeIndex) {
        int slot = SkillProgress.slotIndex(categoryId, seriesId, nodeIndex);
        return slot < 0 ? 0 : ranks[slot];
    }
}
