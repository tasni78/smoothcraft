package com.smoothcraft.client;

import com.smoothcraft.config.SmoothCraftConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 性能面板：左上角显示帧率、被剔除的实体数、丢弃的粒子数、当前自适应比例。
 *
 * 26.x 的 HUD 改成了「提取渲染状态」模式（GuiGraphicsExtractor），
 * 不再是老的 GuiGraphics 直接 drawString，所以这里调的是 g.text(...)。
 */
public final class PerfHud implements HudElement {
	private static final int BG_COLOR = 0x8C0B1420;
	private static final int TITLE_COLOR = 0xFF60E2FF;
	private static final int TEXT_COLOR = 0xFFE8E8E8;
	private static final int WARN_COLOR = 0xFFFF8A6B;
	private static final int LINE_HEIGHT = 10;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.hud.enabled) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.options.hideGui) {
			return;
		}

		int fps = mc.getFps();
		float scale = PerfGovernor.scale();
		int percent = Math.round(scale * 100.0F);

		String line1 = "SmoothCraft  " + fps + " FPS";
		String line2 = "剔除  实体 " + Stats.culledEntitiesPerSec + "  方块实体 " + Stats.culledBlockEntitiesPerSec
			+ "  阴影 " + Stats.culledShadowsPerSec;
		String line3 = "粒子  丢弃 " + Stats.droppedParticlesPerSec + "  距离过滤 " + Stats.filteredParticlesPerSec + " / 秒";
		String line4 = "自适应 " + percent + "%   实体距离 " + Math.round(cfg.entityCulling.maxDistance * scale) + " 格";

		String[] lines = {line1, line2, line3, line4};

		int x = Math.max(0, cfg.hud.x);
		int y = Math.max(0, cfg.hud.y);

		if (cfg.hud.background) {
			int width = 0;
			for (String line : lines) {
				width = Math.max(width, mc.font.width(line));
			}
			g.fill(x - 3, y - 3, x + width + 4, y + lines.length * LINE_HEIGHT + 1, BG_COLOR);
		}

		int ty = y;
		g.text(mc.font, line1, x, ty, TITLE_COLOR);
		ty += LINE_HEIGHT;
		g.text(mc.font, line2, x, ty, TEXT_COLOR);
		ty += LINE_HEIGHT;
		g.text(mc.font, line3, x, ty, TEXT_COLOR);
		ty += LINE_HEIGHT;
		g.text(mc.font, line4, x, ty, PerfGovernor.struggling() ? WARN_COLOR : TEXT_COLOR);
	}
}
