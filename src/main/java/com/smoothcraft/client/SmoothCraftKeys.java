package com.smoothcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.smoothcraft.client.gui.SmoothCraftConfigScreen;
import com.smoothcraft.config.SmoothCraftConfig;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键：
 *   F6 —— 打开设置界面（可视化开关所有功能）
 *   F7 —— 热重载配置文件（手工改 JSON 之后用）
 *   F8 —— 开关性能面板
 *   F9 —— 开关整个模组（排查兼容性时一键关掉所有优化）
 */
public final class SmoothCraftKeys {
	private static KeyMapping openConfig;
	private static KeyMapping toggleHud;
	private static KeyMapping toggleMod;
	private static KeyMapping reloadConfig;

	private SmoothCraftKeys() {
	}

	public static void register() {
		openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.smoothcraft.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, KeyMapping.Category.MISC));
		toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.smoothcraft.toggle_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, KeyMapping.Category.MISC));
		toggleMod = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.smoothcraft.toggle_mod", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, KeyMapping.Category.MISC));
		reloadConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.smoothcraft.reload_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, KeyMapping.Category.MISC));
	}

	public static void handleInput() {
		if (toggleHud == null) {
			return;
		}

		boolean changed = false;

		while (openConfig.consumeClick()) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen == null) {
				mc.setScreen(new SmoothCraftConfigScreen(SmoothCraftConfig.get()));
			}
		}

		while (toggleHud.consumeClick()) {
			SmoothCraftConfig cfg = SmoothCraftConfig.get();
			cfg.hud.enabled = !cfg.hud.enabled;
			changed = true;
			SmoothCraftClient.LOGGER.info("[SmoothCraft] 性能面板：{}", cfg.hud.enabled ? "开" : "关");
		}

		while (toggleMod.consumeClick()) {
			SmoothCraftConfig cfg = SmoothCraftConfig.get();
			cfg.enabled = !cfg.enabled;
			changed = true;
			if (!cfg.enabled) {
				PerfGovernor.reset();
			}
			SmoothCraftClient.LOGGER.info("[SmoothCraft] 模组总开关：{}", cfg.enabled ? "开" : "关");
		}

		while (reloadConfig.consumeClick()) {
			// 设置界面持有旧的配置实例引用，此时热重载会替换 instance，
			// 导致界面上未保存的修改在「保存」时静默丢失。与 F6 一样，界面打开时不响应。
			if (Minecraft.getInstance().screen == null) {
				SmoothCraftConfig.reload();
			}
		}

		if (changed) {
			SmoothCraftConfig.save();
		}
	}
}
