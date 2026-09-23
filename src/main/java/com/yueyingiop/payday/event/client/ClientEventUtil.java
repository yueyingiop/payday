package com.yueyingiop.payday.event.client;

import com.yueyingiop.payday.item.ArmorCurioItem;
import com.yueyingiop.payday.api.helper.CurioHelper;
import net.minecraft.world.entity.player.Player;

/** 客户端事件共用的护甲饰品查询工具。 */
final class ClientEventUtil {
    private ClientEventUtil() {
    }

    /** 判断玩家是否佩戴了护甲饰品。 */
    /**
     * 执行 <code>hasArmorCurio</code> 对应的操作。
     * @param player 参数值。
     * @return 方法执行结果。
     */
    static boolean hasArmorCurio(Player player) {
        return CurioHelper.hasStack(player, CurioHelper.ARMOR_SLOT,
                stack -> stack.getItem() instanceof ArmorCurioItem);
    }
}
