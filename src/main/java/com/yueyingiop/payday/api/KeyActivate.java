package com.yueyingiop.payday.api;

import net.minecraft.world.entity.player.Player;

/** 手机升级的按键激活回调。 */
@FunctionalInterface
public interface KeyActivate {
    /**
     * 处理一次服务端按键激活请求。
     *
     * @param player 按下对应按键的玩家
     * @return 是否成功激活升级效果
     */
    boolean activate(Player player);
}
