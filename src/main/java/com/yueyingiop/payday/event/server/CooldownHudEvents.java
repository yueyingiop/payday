package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.api.CooldownHudManager;
import com.yueyingiop.payday.api.ProbabilityHudManager;
import com.yueyingiop.payday.api.TriggerHudManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 维护服务端冷却 HUD 的生命周期。 */
public final class CooldownHudEvents {
    private CooldownHudEvents() {
    }

    /** 每 tick 清理到期的计时型和触发型 HUD。 */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            CooldownHudManager.tick(event.player);
            if (event.player instanceof ServerPlayer player) {
                TriggerHudManager.refresh(player);
            }
        }
    }

    /** 玩家登录后同步当前仍然有效的 HUD。 */
    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CooldownHudManager.sync(player);
        }
    }

    /** 玩家重生后重新同步 HUD。 */
    @SubscribeEvent
    public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CooldownHudManager.sync(player);
        }
    }

    /** 玩家跨维度后重新同步 HUD。 */
    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CooldownHudManager.sync(player);
        }
    }

    /** 玩家退出服务器后清理服务端缓存。 */
    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        CooldownHudManager.forget(event.getEntity());
        ProbabilityHudManager.forget(event.getEntity());
    }
}
