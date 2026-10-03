package com.yueyingiop.payday.event.server.phone;

import com.yueyingiop.payday.api.helper.PhoneUpgradeHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** 处理反社会者系列的近战命中条件。 */
public final class AntisocialEvents {
    private AntisocialEvents() {
    }

    /**
     * 判断反社会者1是否满足近战命中条件。
     *
     * @param event 受伤事件
     * @param player 反社会者升级持有者
     * @param radius 非友善生物统计半径
     * @param minimumUnfriendly 最少非友善生物数量
     * @return 是否为指定玩家的近战命中，且目标为非友善生物并满足数量条件
     */
    public static boolean isQualifiedMeleeHit(LivingHurtEvent event, Player player,
                                               double radius, int minimumUnfriendly) {
        return PhoneUpgradeHelper.isMeleeHit(event, player)
                && PhoneUpgradeHelper.isUnfriendly(player, event.getEntity())
                && PhoneUpgradeHelper.countUnfriendly(player, radius) >= minimumUnfriendly;
    }
}
