package com.smoothcraft.client.gui;

import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * SmoothCraft 设置界面。
 *
 * 26.x 的界面是「提取渲染状态」模式：不再重写 render(GuiGraphics...)，
 * 而是重写 extractRenderState(GuiGraphicsExtractor, ...)，背景由 Screen 外层自动绘制，
 * 我们只要先 super（让控件生效）再补自己的文字即可。
 *
 * 分三页而不是滚动列表：每页最多 6 行，任何 GUI 缩放下都不会把控件挤出去，
 * 也省掉了一整套滚动/裁剪逻辑，稳得多。
 */
public class SmoothCraftConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 22;
	private static final int PAGES = 5;

	private static final int COLOR_TITLE = 0xFFFFFFFF;
	private static final int COLOR_HINT = 0xFFA0A8B4;
	private static final int COLOR_WARN = 0xFFFF8A6B;

	private final SmoothCraftConfig cfg;
	private int page;

	public SmoothCraftConfigScreen(SmoothCraftConfig cfg) {
		super(Component.translatableWithFallback("smoothcraft.gui.title", "SmoothCraft 设置"));
		this.cfg = cfg;
	}

	@Override
	protected void init() {
		rebuild();
	}

	private void rebuild() {
		clearWidgets();

		int left = this.width / 2 - 155;
		int boxX = left;
		int boxW = 148;
		int sliderX = left + 156;
		int sliderW = 154;
		int y = 44;

		switch (this.page) {
			case 0 -> {
				toggle(boxX, y, boxW, "entity_culling", "实体渲染剔除", cfg.entityCulling.enabled,
					v -> cfg.entityCulling.enabled = v);
				slider(sliderX, y, sliderW, "other_distance", "其他实体距离", 32, 256, 8,
					cfg.entityCulling.maxDistance, " 格", v -> cfg.entityCulling.maxDistance = v);
				y += ROW_HEIGHT;

				slider(boxX, y, boxW + sliderW + 2, "mob_distance", "生物剔除距离", 32, 256, 8,
					cfg.entityCulling.mobsDistance, " 格", v -> cfg.entityCulling.mobsDistance = v);
				y += ROW_HEIGHT;

				slider(boxX, y, boxW + sliderW + 2, "item_distance", "掉落物剔除距离", 8, 128, 4,
					cfg.entityCulling.itemsDistance, " 格", v -> cfg.entityCulling.itemsDistance = v);
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "block_entity_culling", "方块实体剔除", cfg.blockEntityCulling.enabled,
					v -> cfg.blockEntityCulling.enabled = v);
				slider(sliderX, y, sliderW, "block_entity_distance", "距离", 16, 192, 8,
					cfg.blockEntityCulling.maxDistance, " 格", v -> cfg.blockEntityCulling.maxDistance = v);
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "name_tag", "名牌距离限制", cfg.nameTagCulling.enabled,
					v -> cfg.nameTagCulling.enabled = v);
				slider(sliderX, y, sliderW, "name_tag_distance", "距离", 8, 128, 4,
					cfg.nameTagCulling.maxDistance, " 格", v -> cfg.nameTagCulling.maxDistance = v);
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "shadow_culling", "阴影剔除", cfg.entityCulling.cullShadows,
					v -> cfg.entityCulling.cullShadows = v);
				slider(sliderX, y, sliderW, "shadow_distance", "距离", 16, 128, 8,
					cfg.entityCulling.shadowDistance, " 格", v -> cfg.entityCulling.shadowDistance = v);
			}
			case 1 -> {
				slider(boxX, y, boxW + sliderW + 2, "particle_budget", "每刻粒子预算", 16, 2000, 16,
					cfg.particleLimiting.maxPerTick, " 个", v -> cfg.particleLimiting.maxPerTick = v.intValue());
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "particle_budget_adaptive", "预算跟随掉帧收缩", cfg.particleLimiting.followAdaptive,
					v -> cfg.particleLimiting.followAdaptive = v);
				toggle(sliderX, y, sliderW, "item_cluster", "掉落物渲染优化", cfg.itemCluster.enabled,
					v -> cfg.itemCluster.enabled = v);
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "particle_filter", "粒子距离过滤", cfg.particleFilter.enabled,
					v -> cfg.particleFilter.enabled = v);
				slider(sliderX, y, sliderW, "particle_filter_distance", "距离", 8, 64, 4,
					cfg.particleFilter.maxDistance, " 格", v -> cfg.particleFilter.maxDistance = v);
				y += ROW_HEIGHT;

				slider(boxX, y, boxW + sliderW + 2, "item_copies", "一叠最多画几份（原版最多 5）", 1, 5, 1,
					cfg.itemCluster.maxCopiesPerStack, " 份", v -> cfg.itemCluster.maxCopiesPerStack = v.intValue());
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "disable_clouds", "关闭云层", cfg.rendering.disableClouds,
					v -> cfg.rendering.disableClouds = v);
				toggle(sliderX, y, sliderW, "skip_stars", "跳过星空", cfg.rendering.skipStars,
					v -> cfg.rendering.skipStars = v);
			}
			case 2 -> {
				toggle(boxX, y, boxW, "adaptive", "帧率守护", cfg.adaptive.enabled,
					v -> cfg.adaptive.enabled = v);
				slider(sliderX, y, sliderW, "target_fps", "目标帧率", 30, 240, 10,
					cfg.adaptive.targetFps, " FPS", v -> cfg.adaptive.targetFps = v.intValue());
				y += ROW_HEIGHT;

				slider(boxX, y, boxW + sliderW + 2, "min_scale", "收缩下限（占原距离的百分比）", 20, 100, 5,
					cfg.adaptive.minScale * 100.0D, " %", v -> cfg.adaptive.minScale = v / 100.0D);
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "server_throttle", "服务端远处实体降频", cfg.serverThrottle.enabled,
					v -> cfg.serverThrottle.enabled = v);
				slider(sliderX, y, sliderW, "mspt_threshold", "MSPT 阈值", 20, 100, 5,
					cfg.serverThrottle.msptThreshold, " ms", v -> cfg.serverThrottle.msptThreshold = v);
				y += ROW_HEIGHT;

				slider(boxX, y, boxW + sliderW + 2, "throttle_distance", "降频距离（玩家附近不降频）", 16, 96, 8,
					cfg.serverThrottle.maxDistance, " 格", v -> cfg.serverThrottle.maxDistance = v);
			}
			case 3 -> {
				toggle(boxX, y, boxW, "hud", "性能面板（F8）", cfg.hud.enabled,
					v -> cfg.hud.enabled = v);
				slider(sliderX, y, sliderW, "hud_scale", "缩放", 50, 200, 5,
					cfg.hud.scalePercent, " %", v -> cfg.hud.scalePercent = v.floatValue());
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW, "hud_background", "绘制背景", cfg.hud.background,
					v -> cfg.hud.background = v);
				slider(sliderX, y, sliderW, "hud_opacity", "背景不透明度", 0, 100, 5,
					cfg.hud.backgroundAlpha * 100.0D / 255.0D, " %",
					v -> cfg.hud.backgroundAlpha = (int) Math.round(v * 255.0D / 100.0D));
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW + sliderW + 2, "hud_snap", "拖动时吸附到网格（8 像素）", cfg.hud.snapToGrid,
					v -> cfg.hud.snapToGrid = v);
				y += ROW_HEIGHT;

				addRenderableWidget(Button.builder(
						Component.translatableWithFallback("smoothcraft.gui.hud_position", "调整面板位置…"),
						b -> Minecraft.getInstance().setScreen(new SmoothCraftHudEditorScreen(cfg)))
					.bounds(boxX, y, boxW + sliderW + 2, 20).build());
			}
			default -> {
				toggle(boxX, y, boxW, "world_load", "进入世界优化", cfg.worldLoad.enabled,
					v -> cfg.worldLoad.enabled = v);
				toggle(sliderX, y, sliderW, "skip_loading_wait", "提前放行", cfg.worldLoad.skipLoadingWait,
					v -> cfg.worldLoad.skipLoadingWait = v);
				y += ROW_HEIGHT;

				toggle(boxX, y, boxW + sliderW + 2, "ramp_enabled", "入场后视距爬坡", cfg.worldLoad.rampEnabled,
					v -> cfg.worldLoad.rampEnabled = v);
				y += ROW_HEIGHT;

				slider(boxX, y, boxW + sliderW + 2, "ramp_start", "爬坡起始视距", 1, 16, 1,
					cfg.worldLoad.startChunks, " 格", v -> cfg.worldLoad.startChunks = v.intValue());
				y += ROW_HEIGHT;

				slider(boxX, y, boxW + sliderW + 2, "ramp_speed", "每秒增加视距", 1, 8, 1,
					cfg.worldLoad.chunksPerSecond, " 格", v -> cfg.worldLoad.chunksPerSecond = v.intValue());
			}
		}

		buildBottomBar(left);
	}

	private void buildBottomBar(int left) {
		int y = this.height - 30;

		addRenderableWidget(Button.builder(Component.literal("< 上一页"), b -> {
			if (this.page > 0) {
				this.page--;
				rebuild();
			}
		}).bounds(left, y, 64, 20).build());

		addRenderableWidget(Button.builder(Component.literal("下一页 >"), b -> {
			if (this.page < PAGES - 1) {
				this.page++;
				rebuild();
			}
		}).bounds(left + 68, y, 64, 20).build());

		addRenderableWidget(Button.builder(Component.literal("保存并关闭"), b -> {
			SmoothCraftConfig.save();
			this.onClose();
		}).bounds(left + 148, y, 100, 20).build());

		addRenderableWidget(Button.builder(Component.literal("取消"), b -> {
			// 放弃本次修改：重新读盘，把内存里的配置还原成文件内容
			SmoothCraftConfig.load();
			this.onClose();
		}).bounds(left + 252, y, 58, 20).build());
	}

	private void toggle(int x, int y, int w, String key, String fallback, boolean initial, Consumer<Boolean> setter) {
		Checkbox box = Checkbox.builder(label(key, fallback), this.font)
			.pos(x, y)
			.maxWidth(w)
			.selected(initial)
			.onValueChange((checkbox, value) -> setter.accept(value))
			.build();
		addRenderableWidget(box);
	}

	private void slider(int x, int y, int w, String key, String fallback, double min, double max, double step,
						double initial, String unit, Consumer<Double> setter) {
		addRenderableWidget(new ValueSlider(x, y, w, label(key, fallback), min, max, step, initial, unit, setter));
	}

	/** 有翻译就用翻译，没有就退回中文原文（不会出现裸 key）。 */
	private static Component label(String key, String fallback) {
		return Component.translatableWithFallback("smoothcraft.opt." + key, fallback);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		// 先让控件绘制（super 会遍历 renderables），再补标题与说明
		super.extractRenderState(g, mouseX, mouseY, partialTick);

		String title = this.title.getString();
		g.text(this.font, title, (this.width - this.font.width(title)) / 2, 18, COLOR_TITLE);

		String pageInfo = String.format("第 %d / %d 页", this.page + 1, PAGES);
		g.text(this.font, pageInfo, (this.width - this.font.width(pageInfo)) / 2, 32, COLOR_HINT);

		int left = this.width / 2 - 155;
		int hintY = this.height - 44;
		g.text(this.font, "改动即时生效，点「保存并关闭」写入配置文件", left, hintY, COLOR_HINT);

		if (!cfg.enabled) {
			g.text(this.font, "注意：总开关当前为关闭（F9 可切换），所有优化不会生效", left, hintY + 11, COLOR_WARN);
		}
	}

	@Override
	public void onClose() {
		// Esc 直接关闭时也把改动落盘，避免「看着改了其实没保存」
		SmoothCraftConfig.save();
		super.onClose();
	}

	/**
	 * 数值滑块：把 0~1 的归一化位置映射回 [min, max] 并取整到 step。
	 */
	private static final class ValueSlider extends AbstractSliderButton {
		private final Component label;
		private final double min;
		private final double max;
		private final double step;
		private final String unit;
		private final Consumer<Double> setter;

		ValueSlider(int x, int y, int w, Component label, double min, double max, double step,
					double initial, String unit, Consumer<Double> setter) {
			super(x, y, w, 20, Component.empty(), (initial - min) / (max - min));
			this.label = label;
			this.min = min;
			this.max = max;
			this.step = step;
			this.unit = unit;
			this.setter = setter;
			updateMessage();
		}

		private double currentValue() {
			double raw = this.min + this.value * (this.max - this.min);
			return Math.round(raw / this.step) * this.step;
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(this.label.getString() + "：" + format(currentValue()) + this.unit));
		}

		@Override
		protected void applyValue() {
			this.setter.accept(currentValue());
		}

		private String format(double v) {
			if (this.step >= 1.0D) {
				return String.valueOf(Math.round(v));
			}
			return String.format("%.1f", v);
		}
	}
}
