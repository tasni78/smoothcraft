package com.smoothcraft.client;

import com.smoothcraft.config.SmoothCraftConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmoothCraftClient implements ClientModInitializer {
	public static final String MOD_ID = "smoothcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();

		SmoothCraftKeys.register();

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "perf_hud"), new PerfHud());

		// 每游戏刻：处理按键 + 自适应采样
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			SmoothCraftKeys.handleInput();
			PerfGovernor.onEndTick(mc);
		});

		LOGGER.info("[SmoothCraft] 流畅工坊已加载");
		LOGGER.info("[SmoothCraft] 剔除距离：生物 {} 格 / 掉落物 {} 格 / 其他 {} 格 / 方块实体 {} 格 / 名牌 {} 格",
			fmt(cfg.entityCulling.mobsDistance), fmt(cfg.entityCulling.itemsDistance),
			fmt(cfg.entityCulling.maxDistance), fmt(cfg.blockEntityCulling.maxDistance),
			fmt(cfg.nameTagCulling.maxDistance));
		LOGGER.info("[SmoothCraft] 快捷键：F7 重载配置 / F8 性能面板 / F9 总开关；配置：{}",
			SmoothCraftConfig.configPath());
	}

	private static String fmt(double v) {
		return String.format("%.0f", v);
	}
}
