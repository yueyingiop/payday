package com.yueyingiop.payday.api;

import com.yueyingiop.payday.network.CooldownHudSyncPacket;
import com.yueyingiop.payday.network.PayDayNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理玩家屏幕左上角的冷却 HUD。
 *
 * <p>所有方法都使用调用方提供的字符串 ID 区分 HUD。同一个玩家的同一个 ID
 * 再次设置时会覆盖旧内容，适合升级或技能在状态变化时刷新显示。</p>
 */
public final class CooldownHudManager {
    /** 触发型 HUD 的固定显示时间，单位为 tick。 */
    public static final long TRIGGER_DISPLAY_TICKS = 5L * 20L;
    private static final Map<UUID, LinkedHashMap<String, Entry>> HUDS = new ConcurrentHashMap<>();

    private CooldownHudManager() {
    }

    /**
     * 添加或覆盖一个 HUD。
     *
     * <p>计时型会从调用时开始倒计时，概率型会永久显示，触发型会显示五秒。</p>
     *
     * @param player HUD 所属玩家
     * @param id HUD 唯一 ID
     * @param hud HUD 描述
     */
    public static void show(Player player, String id, CooldownHud hud) {
        if (!(player instanceof ServerPlayer serverPlayer) || hud == null) {
            return;
        }
        String normalizedId = requireId(id);
        long now = serverPlayer.level().getGameTime();
        Entry entry = createEntry(hud, now);
        if (entry == null) {
            remove(serverPlayer, normalizedId);
            return;
        }
        HUDS.computeIfAbsent(serverPlayer.getUUID(), ignored -> new LinkedHashMap<>())
                .put(normalizedId, entry);
        send(serverPlayer, normalizedId, entry, false);
    }

    /**
     * 启动一个计时型 HUD。
     *
     * @param player HUD 所属玩家
     * @param id HUD 唯一 ID
     * @param hud 计时型 HUD 描述
     */
    public static void startTimer(Player player, String id, CooldownHud hud) {
        if (hud == null || hud.type() != CooldownHud.Type.TIMER) {
            throw new IllegalArgumentException("startTimer requires a TIMER cooldown HUD");
        }
        show(player, id, hud);
    }

    /**
     * 显示或更新一个永久概率型 HUD。
     *
     * @param player HUD 所属玩家
     * @param id HUD 唯一 ID
     * @param hud 概率型 HUD 描述
     */
    public static void showProbability(Player player, String id, CooldownHud hud) {
        if (hud == null || hud.type() != CooldownHud.Type.PROBABILITY) {
            throw new IllegalArgumentException("showProbability requires a PROBABILITY cooldown HUD");
        }
        show(player, id, hud);
    }

    /**
     * 触发一个只显示五秒的 HUD。
     *
     * @param player HUD 所属玩家
     * @param id HUD 唯一 ID
     * @param hud 触发型 HUD 描述
     */
    public static void trigger(Player player, String id, CooldownHud hud) {
        if (hud == null || hud.type() != CooldownHud.Type.TRIGGER) {
            throw new IllegalArgumentException("trigger requires a TRIGGER cooldown HUD");
        }
        show(player, id, hud);
    }

    /**
     * 移除一个 HUD。
     *
     * @param player HUD 所属玩家
     * @param id HUD 唯一 ID
     */
    public static void remove(Player player, String id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        String normalizedId = requireId(id);
        Map<String, Entry> entries = HUDS.get(serverPlayer.getUUID());
        if (entries != null) {
            entries.remove(normalizedId);
            if (entries.isEmpty()) {
                HUDS.remove(serverPlayer.getUUID());
            }
        }
        send(serverPlayer, normalizedId, null, true);
    }

    /**
     * 移除玩家的全部 HUD。
     *
     * @param player HUD 所属玩家
     */
    public static void clear(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Map<String, Entry> entries = HUDS.remove(serverPlayer.getUUID());
        if (entries == null) {
            return;
        }
        for (String id : entries.keySet()) {
            send(serverPlayer, id, null, true);
        }
    }

    /**
     * 同步玩家当前的全部 HUD，供登录、重生和跨维度时使用。
     *
     * @param player HUD 所属玩家
     */
    public static void sync(ServerPlayer player) {
        Map<String, Entry> entries = HUDS.get(player.getUUID());
        if (entries == null) {
            return;
        }
        long now = player.level().getGameTime();
        for (Map.Entry<String, Entry> hud : new ArrayList<>(entries.entrySet())) {
            Entry entry = hud.getValue();
            if (entry.expired(now)) {
                entries.remove(hud.getKey());
                send(player, hud.getKey(), null, true);
            } else {
                send(player, hud.getKey(), entry, false);
            }
        }
    }

    /**
     * 清理到期的计时型和触发型 HUD。
     *
     * @param player 当前 tick 的玩家
     */
    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Map<String, Entry> entries = HUDS.get(serverPlayer.getUUID());
        if (entries == null) {
            return;
        }
        long now = serverPlayer.level().getGameTime();
        for (String id : new ArrayList<>(entries.keySet())) {
            Entry entry = entries.get(id);
            if (entry != null && entry.expired(now)) {
                entries.remove(id);
                send(serverPlayer, id, null, true);
            }
        }
        if (entries.isEmpty()) {
            HUDS.remove(serverPlayer.getUUID());
        }
    }

    /** 玩家离开服务器时清理服务端缓存。 */
    public static void forget(Player player) {
        HUDS.remove(player.getUUID());
    }

    private static Entry createEntry(CooldownHud hud, long now) {
        return switch (hud.type()) {
            case TIMER -> {
                long total = Math.max(1L, Math.round(hud.value() * 20.0D));
                yield new Entry(hud, now + total, total);
            }
            case PROBABILITY -> new Entry(hud, Long.MAX_VALUE, 0L);
            case TRIGGER -> new Entry(hud, now + TRIGGER_DISPLAY_TICKS,
                    TRIGGER_DISPLAY_TICKS);
        };
    }

    private static void send(ServerPlayer player, String id, Entry entry, boolean remove) {
        long now = player.level().getGameTime();
        long remaining = entry == null ? 0L : Math.max(0L, entry.endTick() - now);
        long total = entry == null ? 0L : entry.totalTicks();
        CooldownHud hud = entry == null ? null : entry.hud();
        PayDayNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CooldownHudSyncPacket(player.getId(), id, remove,
                        hud == null ? CooldownHud.Type.TIMER.ordinal() : hud.type().ordinal(),
                        hud == null || hud.texture() == null ? "" : hud.texture().toString(),
                        hud == null ? 0.0D : hud.value(),
                        hud == null ? net.minecraft.network.chat.Component.translatable(
                                "cooldown_hud.payday.unknown") : hud.name(),
                        remaining, total));
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Cooldown HUD id must not be blank");
        }
        return id;
    }

    private record Entry(CooldownHud hud, long endTick, long totalTicks) {
        private boolean expired(long now) {
            return hud.type() != CooldownHud.Type.PROBABILITY && now >= endTick;
        }
    }
}
