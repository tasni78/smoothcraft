package com.smoothcraft.server;

import com.smoothcraft.config.SmoothCraftConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /smoothcraft status   —— 查看实时 TPS / MSPT / 近一秒被降频的实体数
 * /smoothcraft reload   —— 热重载配置（不需要重启服）
 */
public final class SmoothCraftCommand {
	private SmoothCraftCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("smoothcraft")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("status").executes(ctx -> {
				SmoothCraftConfig cfg = SmoothCraftConfig.get();
				String msg = String.format(
					"SmoothCraft | TPS %.1f | MSPT %.1fms | 近1秒降频 %d 个实体 | 累计 %d | 阈值 %.0fms | %s",
					ServerPerf.tps(),
					ServerPerf.mspt(),
					ServerPerf.throttledLastSecond(),
					ServerPerf.throttledTotal(),
					cfg.serverThrottle.msptThreshold,
					cfg.enabled && cfg.serverThrottle.enabled ? "已启用" : "已关闭");
				ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
				return 1;
			}))
			.then(Commands.literal("reload").executes(ctx -> {
				SmoothCraftConfig.reload();
				ctx.getSource().sendSuccess(() -> Component.literal("SmoothCraft 配置已重新加载"), true);
				return 1;
			}))
		);
	}
}
