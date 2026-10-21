package com.yueyingiop.payday.api;

import com.yueyingiop.payday.api.helper.PhoneUpgradeHelper;
import com.yueyingiop.payday.event.server.phone.LeaderEvents;
import com.yueyingiop.payday.item.PhoneItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 刷新手机升级条件持续成立时显示的触发型 CDHUD。 */
public final class TriggerHudManager {
    private static final String ID_PREFIX = "payday_trigger_";

    private TriggerHudManager() {
    }

    /** 刷新一个玩家当前所有条件型触发 HUD。 */
    public static void refresh(ServerPlayer player) {
        Player leader = LeaderEvents.bestLeaderProvider(player);
        int leaderLevel = leader == null ? 0 : PhoneItem.installedSeriesLevel(leader, "leader");
        int ownLeaderLevel = PhoneItem.installedSeriesLevel(player, "leader");
        int rogueLevel = PhoneItem.installedSeriesLevel(player, "rogue");
        int thiefLevel = PhoneItem.installedSeriesLevel(player, "thief");
        int infiltratorLevel = PhoneItem.installedSeriesLevel(player, "infiltrator");
        int antisocialLevel = PhoneItem.installedSeriesLevel(player, "antisocial");

        set(player, "leader1_range", "special_leader_1", "special_leader_1",
                leader != null && leaderLevel >= 1 && leader != player
                        || leader == player && ownLeaderLevel >= 1
                        && LeaderEvents.otherPlayersInRange(player, 16.0D) > 0);
        set(player, "leader1_low_health", "special_leader_1", "special_leader_1",
                ownLeaderLevel >= 1 && LeaderEvents.isActiveLeader(player, player)
                        && player.getHealth() <= player.getMaxHealth() * 0.5F);
        set(player, "leader3_range_health", "special_leader_3", "special_leader_3",
                leader != null && leader != player && leaderLevel >= 3);
        set(player, "leader5_players", "special_leader_5", "special_leader_5",
                leader != null && leaderLevel >= 5
                        && LeaderEvents.otherPlayersInRange(leader, 16.0D) > 0);
        set(player, "rogue2_nearby_player", "special_rogue_2", "special_rogue_2",
                rogueLevel >= 2 && hasOtherPlayer(player, 8.0D));
        set(player, "thief5_sneak_speed", "special_thief_5", "special_thief_5",
                thiefLevel >= 5 && player.isCrouching()
                        && player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D);
        set(player, "infiltrator1_reduction", "special_infiltrator_1", "special_infiltrator_1",
                infiltratorLevel >= 1 && PhoneUpgradeHelper.countUnfriendly(player, 8.0D) >= 3);
        set(player, "infiltrator2_reduction", "special_infiltrator_2", "special_infiltrator_2",
                infiltratorLevel >= 2 && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1);
        set(player, "infiltrator3_reduction", "special_infiltrator_3", "special_infiltrator_3",
                infiltratorLevel >= 3 && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1);
        set(player, "infiltrator4_reduction", "special_infiltrator_4", "special_infiltrator_4",
                infiltratorLevel >= 4 && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1);
        set(player, "antisocial1_infiltration", "special_antisocial_1", "special_antisocial_1",
                antisocialLevel >= 1 && PhoneUpgradeHelper.countUnfriendly(player, 8.0D) >= 3);
        set(player, "antisocial3_reduction", "special_antisocial_3", "special_antisocial_3",
                antisocialLevel >= 3 && PhoneUpgradeHelper.countUnfriendly(player, 16.0D) >= 1);
    }

    /** 清理一个玩家的全部条件型触发 HUD。 */
    public static void clear(Player player) {
        CooldownHudManager.removeByPrefix(player, ID_PREFIX);
    }

    private static boolean hasOtherPlayer(Player player, double radius) {
        return !player.level().getEntitiesOfClass(Player.class,
                player.getBoundingBox().inflate(radius), other -> other != player
                        && other.isAlive() && !other.isSpectator()
                        && other.distanceToSqr(player) <= radius * radius).isEmpty();
    }

    private static void set(Player player, String id, String effectKey, String nameKey,
                            boolean active) {
        CooldownHud hud = active ? CooldownHud.builder()
                .type(CooldownHud.Type.TRIGGER)
                .texture("payday:item/phone_upgrade/generated/" + effectKey)
                .showName(Component.translatable("item.payday.phone_upgrade." + nameKey))
                .build() : null;
        CooldownHudManager.setTriggerActive(player, ID_PREFIX + id, hud, active);
    }
}
