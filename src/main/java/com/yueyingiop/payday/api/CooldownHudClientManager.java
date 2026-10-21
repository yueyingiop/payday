package com.yueyingiop.payday.api;

import com.yueyingiop.payday.network.CooldownHudSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存客户端收到的冷却 HUD 状态，供 HUD 渲染器读取。
 *
 * <p>客户端使用本地游戏时间计算剩余时间，避免服务端每 tick 发送数据包。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CooldownHudClientManager {
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private CooldownHudClientManager() {
    }

    /**
     * 应用服务端同步的 HUD 数据。
     *
     * @param packet 同步数据包
     */
    public static void apply(CooldownHudSyncPacket packet) {
        if (packet.remove()) {
            ENTRIES.remove(packet.id());
            return;
        }
        CooldownHud.Type[] types = CooldownHud.Type.values();
        if (packet.type() < 0 || packet.type() >= types.length) {
            return;
        }
        ResourceLocation texture = packet.texture().isBlank()
                ? null : ResourceLocation.tryParse(packet.texture());
        if (!packet.texture().isBlank() && texture == null) {
            return;
        }
        long now = gameTime();
        ENTRIES.put(packet.id(), new Entry(
                CooldownHud.of(types[packet.type()], texture, packet.value(), packet.name(),
                        packet.showValue(), packet.percentage(), packet.textureU(),
                        packet.textureV()),
                now + Math.max(0L, packet.remainingTicks()),
                Math.max(0L, packet.totalTicks())));
    }

    /**
     * 获取当前有效 HUD，按服务端添加顺序排列。
     *
     * @return 当前有效 HUD 快照
     */
    public static List<VisibleHud> visible() {
        long now = gameTime();
        List<VisibleHud> result = new ArrayList<>();
        for (String id : new ArrayList<>(ENTRIES.keySet())) {
            Entry entry = ENTRIES.get(id);
            if (entry == null) {
                continue;
            }
            if (entry.hud().type() != CooldownHud.Type.PROBABILITY && now >= entry.endTick()) {
                ENTRIES.remove(id);
                continue;
            }
            result.add(new VisibleHud(id, entry.hud(),
                    Math.max(0L, entry.endTick() - now), entry.totalTicks()));
        }
        return result;
    }

    /** 清空客户端缓存。 */
    public static void clear() {
        ENTRIES.clear();
    }

    private static long gameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    /** 客户端用于渲染的 HUD 快照。 */
    public record VisibleHud(String id, CooldownHud hud, long remainingTicks, long totalTicks) {
        /**
         * 获取计时型 HUD 的剩余秒数。
         *
         * @return 剩余秒数
         */
        public double remainingSeconds() {
            return remainingTicks / 20.0D;
        }

        /**
         * 获取冷却进度，数值越大表示剩余越多。
         *
         * @return 0~1 的剩余比例
         */
        public float remainingProgress() {
            if (totalTicks <= 0L) {
                return 1.0F;
            }
            return Mth.clamp(remainingTicks / (float) totalTicks, 0.0F, 1.0F);
        }
    }

    private record Entry(CooldownHud hud, long endTick, long totalTicks) {
    }
}
