package com.yueyingiop.payday.skill;

import com.yueyingiop.payday.network.PayDayNetwork;
import com.yueyingiop.payday.network.SyncSkillStatePacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** 保存并处理玩家技能点与技能等级。技能具体效果暂时不在这里执行。 */
public final class SkillProgress {
    public static final int NODE_COUNT = 6;
    public static final List<String> SERIES_KEYS = List.of(
            "mastermind/medical", "mastermind/control", "mastermind/sharpshooter",
            "mobster/shotgun", "mobster/heavy_armor", "mobster/ammo",
            "technician/engineering", "technician/repair", "technician/suppression",
            "ghost/ninjutsu", "ghost/evasion", "ghost/suppressor",
            "fugitive/quickdraw", "fugitive/rebirth", "fugitive/melee"
    );
    public static final int SLOT_COUNT = SERIES_KEYS.size() * NODE_COUNT;

    private static final String ROOT = "payday_skill_progress";
    private static final String POINTS = "points";
    private static final String RANKS = "ranks";

    private SkillProgress() {
    }

    public static int getSkillPoints(Player player) {
        return root(player).getInt(POINTS);
    }

    public static void addSkillPoints(Player player, int amount) {
        setSkillPoints(player, Math.max(0, getSkillPoints(player) + amount));
    }

    public static void setSkillPoints(Player player, int points) {
        root(player).putInt(POINTS, Math.max(0, points));
    }

    public static int getRank(Player player, String categoryId, String seriesId, int nodeIndex) {
        return getRank(player, slotIndex(categoryId, seriesId, nodeIndex));
    }

    public static int getRank(Player player, int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return 0;
        }
        return Math.min(2, Math.max(0, root(player).getCompound(RANKS).getByte(Integer.toString(slot))));
    }

    public static void setRank(Player player, String categoryId, String seriesId, int nodeIndex, int rank) {
        setRank(player, slotIndex(categoryId, seriesId, nodeIndex), rank);
    }

    public static void setRank(Player player, int slot, int rank) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        root(player).getCompound(RANKS).putByte(Integer.toString(slot), (byte) Math.min(2, Math.max(0, rank)));
    }

    public static int slotIndex(String categoryId, String seriesId, int nodeIndex) {
        int seriesIndex = SERIES_KEYS.indexOf(categoryId + "/" + seriesId);
        if (seriesIndex < 0 || nodeIndex < 0 || nodeIndex >= NODE_COUNT) {
            return -1;
        }
        return seriesIndex * NODE_COUNT + nodeIndex;
    }

    public static byte[] snapshotRanks(Player player) {
        byte[] ranks = new byte[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ranks[slot] = (byte) getRank(player, slot);
        }
        return ranks;
    }

    public static void sync(ServerPlayer player) {
        PayDayNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SyncSkillStatePacket(getSkillPoints(player), snapshotRanks(player)));
    }

    public static boolean learn(Player player, int categoryIndex, int seriesIndex, int nodeIndex) {
        String seriesKey = seriesKey(categoryIndex, seriesIndex);
        if (seriesKey == null || nodeIndex < 0 || nodeIndex >= NODE_COUNT) {
            return false;
        }

        String[] parts = seriesKey.split("/", 2);
        int rank = getRank(player, parts[0], parts[1], nodeIndex);
        SkillData.Definition definition = definition(parts[0], parts[1], nodeIndex);
        if (definition == null || !hasPrerequisites(player, parts[0], parts[1], nodeIndex)) {
            return false;
        }

        int cost;
        if (rank == 0) {
            cost = definition.baseCost();
        } else if (rank == 1) {
            cost = definition.masteryCost();
        } else {
            return false;
        }
        if (getSkillPoints(player) < cost) {
            return false;
        }

        setSkillPoints(player, getSkillPoints(player) - cost);
        setRank(player, parts[0], parts[1], nodeIndex, rank + 1);
        return true;
    }

    public static boolean forget(Player player, int categoryIndex, int seriesIndex, int nodeIndex) {
        String seriesKey = seriesKey(categoryIndex, seriesIndex);
        if (seriesKey == null || nodeIndex < 0 || nodeIndex >= NODE_COUNT) {
            return false;
        }

        String[] parts = seriesKey.split("/", 2);
        int rank = getRank(player, parts[0], parts[1], nodeIndex);
        SkillData.Definition definition = definition(parts[0], parts[1], nodeIndex);
        if (definition == null || rank == 0) {
            return false;
        }

        if (rank == 2) {
            setSkillPoints(player, getSkillPoints(player) + definition.masteryCost());
            setRank(player, parts[0], parts[1], nodeIndex, 1);
        } else {
            setSkillPoints(player, getSkillPoints(player) + definition.baseCost());
            setRank(player, parts[0], parts[1], nodeIndex, 0);
        }
        removeInvalidSkills(player, parts[0], parts[1]);
        return true;
    }

    public static void resetCategory(Player player, int categoryIndex) {
        if (categoryIndex < 0 || categoryIndex >= 5) {
            return;
        }
        for (int seriesIndex = 0; seriesIndex < 3; seriesIndex++) {
            refundSeries(player, categoryIndex, seriesIndex);
        }
    }

    public static void resetAll(Player player) {
        for (int categoryIndex = 0; categoryIndex < 5; categoryIndex++) {
            resetCategory(player, categoryIndex);
        }
    }

    public static void convertTenLevels(ServerPlayer player) {
        if (player.experienceLevel < 10) {
            return;
        }
        player.giveExperienceLevels(-10);
        addSkillPoints(player, 1);
    }

    public static void convertAllExperience(ServerPlayer player) {
        int points = player.experienceLevel / 10;
        if (player.experienceLevel <= 0 && player.experienceProgress <= 0.0F) {
            return;
        }
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
        addSkillPoints(player, points);
    }

    public static void copy(Player original, Player target) {
        CompoundTag oldRoot = original.getPersistentData().getCompound(ROOT);
        if (!oldRoot.isEmpty()) {
            target.getPersistentData().put(ROOT, oldRoot.copy());
        }
    }

    private static void refundSeries(Player player, int categoryIndex, int seriesIndex) {
        String seriesKey = seriesKey(categoryIndex, seriesIndex);
        if (seriesKey == null) {
            return;
        }
        String[] parts = seriesKey.split("/", 2);
        for (int nodeIndex = 0; nodeIndex < NODE_COUNT; nodeIndex++) {
            int rank = getRank(player, parts[0], parts[1], nodeIndex);
            SkillData.Definition definition = definition(parts[0], parts[1], nodeIndex);
            if (definition == null) {
                continue;
            }
            if (rank >= 1) {
                addSkillPoints(player, definition.baseCost());
            }
            if (rank >= 2) {
                addSkillPoints(player, definition.masteryCost());
            }
            setRank(player, parts[0], parts[1], nodeIndex, 0);
        }
    }

    private static void removeInvalidSkills(Player player, String categoryId, String seriesId) {
        boolean removed;
        do {
            removed = false;
            for (int nodeIndex = NODE_COUNT - 1; nodeIndex >= 0; nodeIndex--) {
                int rank = getRank(player, categoryId, seriesId, nodeIndex);
                SkillData.Definition definition = definition(categoryId, seriesId, nodeIndex);
                if (rank == 0 || definition == null || hasPrerequisites(player, categoryId, seriesId, nodeIndex)) {
                    continue;
                }
                if (rank >= 1) {
                    addSkillPoints(player, definition.baseCost());
                }
                if (rank >= 2) {
                    addSkillPoints(player, definition.masteryCost());
                }
                setRank(player, categoryId, seriesId, nodeIndex, 0);
                removed = true;
            }
        } while (removed);
    }

    private static boolean hasPrerequisites(Player player, String categoryId, String seriesId, int nodeIndex) {
        if (nodeIndex == 0) {
            return true;
        }
        int previousTierStart = nodeIndex == 1 || nodeIndex == 2 ? 0 : nodeIndex == 3 || nodeIndex == 4 ? 1 : 3;
        int previousTierEnd = nodeIndex == 1 || nodeIndex == 2 ? 0 : nodeIndex == 3 || nodeIndex == 4 ? 2 : 4;
        boolean previousLearned = false;
        for (int index = previousTierStart; index <= previousTierEnd; index++) {
            if (getRank(player, categoryId, seriesId, index) > 0) {
                previousLearned = true;
                break;
            }
        }
        if (!previousLearned) {
            return false;
        }

        int requiredSpent = nodeIndex <= 2 ? 1 : nodeIndex <= 4 ? 3 : 16;
        return spentInSeries(player, categoryId, seriesId, nodeIndex) >= requiredSpent;
    }

    private static int spentInSeries(Player player, String categoryId, String seriesId, int excludedNode) {
        int spent = 0;
        List<SkillData.Definition> definitions = SkillData.forSeries(categoryId, seriesId);
        for (int nodeIndex = 0; nodeIndex < Math.min(NODE_COUNT, definitions.size()); nodeIndex++) {
            if (nodeIndex == excludedNode) {
                continue;
            }
            int rank = getRank(player, categoryId, seriesId, nodeIndex);
            if (rank >= 1) {
                spent += definitions.get(nodeIndex).baseCost();
            }
            if (rank >= 2) {
                spent += definitions.get(nodeIndex).masteryCost();
            }
        }
        return spent;
    }

    private static SkillData.Definition definition(String categoryId, String seriesId, int nodeIndex) {
        List<SkillData.Definition> definitions = SkillData.forSeries(categoryId, seriesId);
        return nodeIndex >= 0 && nodeIndex < definitions.size() ? definitions.get(nodeIndex) : null;
    }

    private static String seriesKey(int categoryIndex, int seriesIndex) {
        int index = categoryIndex * 3 + seriesIndex;
        return index >= 0 && index < SERIES_KEYS.size() ? SERIES_KEYS.get(index) : null;
    }

    private static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        CompoundTag root = persistent.getCompound(ROOT);
        if (!root.contains(RANKS)) {
            root.put(RANKS, new CompoundTag());
        }
        return root;
    }
}
