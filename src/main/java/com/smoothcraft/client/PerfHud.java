package com.smoothcraft.client;

import com.smoothcraft.config.SmoothCraftConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/**
 * 性能面板：显示帧率、剔除统计、自适应比例。
 *
 * 26.x 的 HUD 是「提取渲染状态」模式（GuiGraphicsExtractor），不是老的 drawString。
 *
 * 位置、缩放、背景不透明度都是可配置的：面板的绘制逻辑抽成了静态方法，
 * 这样「HUD 编辑器」界面可以拿同一份代码在拖动位置实时预览，不会出现两套画法不一致。
 */
public final class PerfHud implements HudElement {
	private static final int BG_RGB = 0x0B1420;
	private static final int TITLE_COLOR = 0xFF60E2FF;
	private static final int TEXT_COLOR = 0xFFE8E8E8;
	private static final int WARN_COLOR = 0xFFFF8A6B;
	public static final int LINE_HEIGHT = 10;
	/** 背景相对文字的外扩距离。 */
	public static final int PADDING = 3;

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

		float scale = cfg.hud.scale();
		int width = panelWidth(mc.font);
		int height = panelHeight();

		int x = clamp(cfg.hud.x, 0, Math.max(0, g.guiWidth() - Math.round(width * scale)));
		int y = clamp(cfg.hud.y, 0, Math.max(0, g.guiHeight() - Math.round(height * scale)));

		draw(g, mc.font, x, y, scale, cfg.hud.effectiveAlpha());
	}

	/** 面板内容宽度（未缩放，单位 GUI 像素）。 */
	public static int panelWidth(Font font) {
		String[] lines = lines();
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, font.width(line));
		}
		return width;
	}

	/** 面板内容高度（未缩放）。 */
	public static int panelHeight() {
		return lines().length * LINE_HEIGHT;
	}

	/** 在指定位置绘制面板；坐标是面板左上角（文字起点减去内边距后的那个点）。 */
	public static void draw(GuiGraphicsExtractor g, Font font, int x, int y, float scale, int bgAlpha) {
		String[] lines = lines();
		int width = panelWidth(font);
		int height = panelHeight();

		if (bgAlpha > 0) {
			int color = (bgAlpha << 24) | BG_RGB;
			// 背景不参与文字缩放，直接把尺寸乘上缩放系数
			g.fill(
				x - PADDING,
				y - PADDING,
				x + Math.round(width * scale) + PADDING + 1,
				y + Math.round(height * scale) + PADDING,
				color);
		}

		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);

		// 缩放后要反向折算坐标，视觉上才仍然落在 (x, y)
		int tx = Math.round(x / scale);
		int ty = Math.round(y / scale);

		for (int i = 0; i < lines.length; i++) {
			int color;
			if (i == 0) {
				color = TITLE_COLOR;
			} else if (i == lines.length - 1) {
				color = PerfGovernor.struggling() ? WARN_COLOR : TEXT_COLOR;
			} else {
				color = TEXT_COLOR;
			}
			g.text(font, lines[i], tx, ty + i * LINE_HEIGHT, color);
		}

		pose.popMatrix();
	}

	private static String[] lines() {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		Minecraft mc = Minecraft.getInstance();
		float scale = PerfGovernor.scale();
		int percent = Math.round(scale * 100.0F);

		return new String[]{
			"SmoothCraft  " + (mc == null ? 0 : mc.getFps()) + " FPS",
			"剔除  实体 " + Stats.culledEntitiesPerSec + "  方块实体 " + Stats.culledBlockEntitiesPerSec
				+ "  阴影 " + Stats.culledShadowsPerSec,
			"粒子  丢弃 " + Stats.droppedParticlesPerSec + "  距离过滤 " + Stats.filteredParticlesPerSec + " / 秒",
			"自适应 " + percent + "%   实体距离 " + Math.round(cfg.entityCulling.maxDistance * scale) + " 格"
		};
	}

	/** 把面板夹在屏幕内，避免拖动后跑到看不见的地方。 */
	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
