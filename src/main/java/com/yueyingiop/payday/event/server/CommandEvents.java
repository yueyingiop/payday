package com.yueyingiop.payday.event.server;

import com.yueyingiop.payday.command.ExposureRiskCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Registers PayDay server commands. */
public final class CommandEvents {
    private CommandEvents() {
    }

    /**
     * 注册暴露风险相关指令。
     * @param event 事件。
     */
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ExposureRiskCommand.register(event.getDispatcher());
    }
}
