package com.yueyingiop.payday.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yueyingiop.payday.exposure.ExposureRisk;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Registers the player-facing command for inspecting exposure risk. */
public final class ExposureRiskCommand {
    private ExposureRiskCommand() {
    }

    /**
     * 注册对应内容。
     * @param dispatcher 对应参数。
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("payday")
                .then(Commands.literal("exposure")
                        .executes(context -> showExposure(context.getSource()))));
    }

    /**
     * 向指令执行者显示当前暴露风险。
     * @param source 来源。
     * @return 对应的执行结果。
     */
    private static int showExposure(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            source.sendFailure(Component.translatable("command.payday.exposure.player_only"));
            return 0;
        }
        int risk = ExposureRisk.get(player);
        double multiplier = ExposureRisk.targetDistanceMultiplier(player);
        source.sendSuccess(() -> Component.translatable("command.payday.exposure.result", risk,
                String.format(java.util.Locale.ROOT, "%.3f", multiplier)), false);
        return risk;
    }
}
