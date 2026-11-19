package com.smoothcraft.client.gui;

import com.smoothcraft.client.PerfHud;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 性能面板位置编辑器。
 *
 * 为什么需要单独一个界面：游戏内鼠标是被锁定的（用来转视角），
 * 所以直接在主界面里拖是拖不动的，必须让鼠标释放出来。
 *
 * 交互：
 *   - 按住左键拖动面板，实时跟随
 *   - 吸附：靠近屏幕边缘自动贴边，其余位置吸附到 8 像素网格
 *   - 拖动时显示青色边框与贴边参考线
 *   - Esc 或「完成」保存并返回设置界面
 */
public class SmoothCraftHudEditorScreen extends Screen {
	private static final int GRID = 8;
	private static final int EDGE_SNAP = 12;

	private static final int ACCENT = 0xFF60E2FF;
	private static final int HINT = 0xFFA0A8B4;

	private final SmoothCraftConfig cfg;

	private float posX;
	private float posY;
	private boolean dragging;
	private float grabOffsetX;
	private float grabOffsetY;
	private boolean snappedLeftRight;
	private boolean snappedTopBottom;

	public SmoothCraftHudEditorScreen(SmoothCraftConfig cfg) {
		super(Component.translatableWithFallback("smoothcraft.gui.hud_editor", "调整性能面板位置"));
		this.cfg = cfg;
		this.posX = cfg.hud.x;
		this.posY = cfg.hud.y;
	}

	@Override
	protected void init() {
		int bottom = this.height - 30;

		addRenderableWidget(Button.builder(
				Component.translatableWithFallback("smoothcraft.gui.hud_reset", "重置到左上角"),
				b -> {
					this.posX = 4;
					this.posY = 4;
					commit();
				})
			.bounds(this.width / 2 - 110, bottom, 110, 20).build());

		addRenderableWidget(Button.builder(
				Component.translatableWithFallback("smoothcraft.gui.done", "完成"),
				b -> this.onClose())
			.bounds(this.width / 2 + 5, bottom, 105, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);

		float scale = cfg.hud.scale();
		int w = Math.round(PerfHud.panelWidth(this.font) * scale);
		int h = Math.round(PerfHud.panelHeight() * scale);

		// 贴边参考线（只在贴边状态下画）
		if (this.snappedLeftRight) {
			int lineX = Math.round(this.posX) <= 0 ? 0 : this.width - 1;
			g.fill(lineX, 0, lineX + 1, this.height, ACCENT);
		}
		if (this.snappedTopBottom) {
			int lineY = Math.round(this.posY) <= 0 ? 0 : this.height - 1;
			g.fill(0, lineY, this.width, lineY + 1, ACCENT);
		}

		// 与游戏内完全一致的画法（同一份代码，不会出现预览和实际不一致）
		PerfHud.draw(g, this.font, Math.round(this.posX), Math.round(this.posY), scale, cfg.hud.effectiveAlpha());

		if (this.dragging) {
			int left = Math.round(this.posX) - PerfHud.PADDING;
			int top = Math.round(this.posY) - PerfHud.PADDING;
			int right = left + w + PerfHud.PADDING * 2 + 1;
			int bottom = top + h + PerfHud.PADDING * 2;
			g.fill(left, top, right, top + 1, ACCENT);
			g.fill(left, bottom - 1, right, bottom, ACCENT);
			g.fill(left, top, left + 1, bottom, ACCENT);
			g.fill(right - 1, top, right, bottom, ACCENT);
		}

		String hint = "按住左键拖动面板 · 靠近边缘自动贴边 · Esc 保存";
		g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 12, HINT);

		String coords = "位置 X " + Math.round(this.posX) + "  Y " + Math.round(this.posY)
			+ "   缩放 " + Math.round(cfg.hud.scalePercent) + "%";
		g.text(this.font, coords, (this.width - this.font.width(coords)) / 2, 24, HINT);

		if (!cfg.hud.enabled) {
			String warn = "面板当前已关闭（F8 可打开），这里仍可调整位置";
			g.text(this.font, warn, (this.width - this.font.width(warn)) / 2, 36, 0xFFFF8A6B);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && insidePanel(event.x(), event.y())) {
			this.dragging = true;
			this.grabOffsetX = (float) event.x() - this.posX;
			this.grabOffsetY = (float) event.y() - this.posY;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging) {
			applyPosition((float) event.x() - this.grabOffsetX, (float) event.y() - this.grabOffsetY);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging) {
			this.dragging = false;
			commit();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		commit();
		// Esc / 完成 都回到设置界面，不用重新按 F6
		Minecraft.getInstance().setScreen(new SmoothCraftConfigScreen(this.cfg));
	}

	private boolean insidePanel(double mouseX, double mouseY) {
		float scale = cfg.hud.scale();
		int w = Math.round(PerfHud.panelWidth(this.font) * scale);
		int h = Math.round(PerfHud.panelHeight() * scale);
		return mouseX >= this.posX - PerfHud.PADDING && mouseX <= this.posX + w + PerfHud.PADDING
			&& mouseY >= this.posY - PerfHud.PADDING && mouseY <= this.posY + h + PerfHud.PADDING;
	}

	/** 夹进屏幕 + 边缘吸附 + 网格吸附。 */
	private void applyPosition(float rawX, float rawY) {
		float scale = cfg.hud.scale();
		int w = Math.round(PerfHud.panelWidth(this.font) * scale);
		int h = Math.round(PerfHud.panelHeight() * scale);

		int x = PerfHud.clamp(Math.round(rawX), 0, Math.max(0, this.width - w));
		int y = PerfHud.clamp(Math.round(rawY), 0, Math.max(0, this.height - h));

		this.snappedLeftRight = false;
		if (x <= EDGE_SNAP) {
			x = 0;
			this.snappedLeftRight = true;
		} else if (x + w >= this.width - EDGE_SNAP) {
			x = Math.max(0, this.width - w);
			this.snappedLeftRight = true;
		} else if (cfg.hud.snapToGrid) {
			x = Math.round(x / (float) GRID) * GRID;
		}

		this.snappedTopBottom = false;
		if (y <= EDGE_SNAP) {
			y = 0;
			this.snappedTopBottom = true;
		} else if (y + h >= this.height - EDGE_SNAP) {
			y = Math.max(0, this.height - h);
			this.snappedTopBottom = true;
		} else if (cfg.hud.snapToGrid) {
			y = Math.round(y / (float) GRID) * GRID;
		}

		this.posX = x;
		this.posY = y;
	}

	private void commit() {
		cfg.hud.x = Math.round(this.posX);
		cfg.hud.y = Math.round(this.posY);
		SmoothCraftConfig.save();
	}
}
