package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.skill.SkillProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 技能数据的玩家生命周期处理。 */
public final class SkillEvents {
    private SkillEvents() {
    }

    @SubscribeEvent
    public static void copySkills(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            SkillProgress.copy(event.getOriginal(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void syncSkills(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillProgress.sync(player);
        }
    }
}
