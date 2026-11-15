package com.smoothcraft;

import com.smoothcraft.config.SmoothCraftConfig;
import com.smoothcraft.server.ServerPerf;
import com.smoothcraft.server.SmoothCraftCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用入口（客户端和服务端都会加载）。
 * 这里只挂服务端相关的东西：TPS 采样 + /smoothcraft 命令。
 *
 * 用 ModInitializer 而不是 DedicatedServerModInitializer，是为了让单人游戏
 * （集成服务器）也能跑到这段逻辑，本地开世界就能用 /smoothcraft status 看数据。
 */
public class SmoothCraft implements ModInitializer {
	public static final String MOD_ID = "smoothcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();

		ServerTickEvents.END_SERVER_TICK.register(ServerPerf::onEndTick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			SmoothCraftCommand.register(dispatcher));

		LOGGER.info("[SmoothCraft] 服务端模块：MSPT 超过 {}ms 时，对 {} 格外的 {} 降频（每 {} 刻一次）",
			fmt(cfg.serverThrottle.msptThreshold),
			fmt(cfg.serverThrottle.maxDistance),
			cfg.serverThrottle.entityTypes,
			Math.max(2, cfg.serverThrottle.skipRatio));
		LOGGER.info("[SmoothCraft] 用 /smoothcraft status 查看实时 TPS");
	}

	private static String fmt(double v) {
		return String.format("%.0f", v);
	}
}
