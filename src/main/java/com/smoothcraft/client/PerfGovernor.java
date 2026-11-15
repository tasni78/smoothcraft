package com.smoothcraft.client;

import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;

/**
 * 帧率守护 / 动态距离调度。
 *
 * 思路：每 20 游戏刻（约 1 秒）采样一次当前帧率，
 * 掉到目标帧率以下就按比例收缩剔除距离，帧率富余时再慢慢放回来。
 * 玩家感知不到生硬的「一刀切」，而是画面远处的生物/方块实体逐渐淡出。
 */
public final class PerfGovernor {
	private static final int SAMPLE_INTERVAL = 20;

	private static float scale = 1.0F;
	private static int ticks;
	private static int lastFps = -1;
	/** 上一次采样时是否处于掉帧状态，给 HUD 上色用。 */
	private static boolean struggling;

	private PerfGovernor() {
	}

	/** 当前的自适应缩放系数，1.0 表示不收缩。 */
	public static float scale() {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.adaptive.enabled) {
			return 1.0F;
		}
		return scale;
	}

	/** 当前实际生效的默认实体剔除距离。 */
	public static double effectiveEntityDistance() {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		return Math.max(16.0D, cfg.entityCulling.maxDistance * scale());
	}

	/** 按类别阈值套上自适应系数后的最终距离。 */
	public static double scale(double baseDistance) {
		return Math.max(8.0D, baseDistance * scale());
	}

	public static int lastFps() {
		return lastFps;
	}

	public static boolean struggling() {
		return struggling;
	}

	public static void onEndTick(Minecraft mc) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();

		if (!cfg.enabled || !cfg.adaptive.enabled) {
			scale = 1.0F;
			struggling = false;
			return;
		}

		if (++ticks < SAMPLE_INTERVAL) {
			return;
		}
		ticks = 0;

		// 结算上一秒的剔除统计，供 HUD 显示
		Stats.flush();

		// 不在世界里（主菜单 / 加载中）时不做判断，直接把系数恢复满
		if (mc.level == null || mc.player == null || mc.isPaused()) {
			scale = 1.0F;
			struggling = false;
			return;
		}

		int fps = mc.getFps();
		lastFps = fps;

		double minScale = Math.max(0.2D, Math.min(1.0D, cfg.adaptive.minScale));
		int target = Math.max(15, cfg.adaptive.targetFps);

		if (fps < target * 0.9D) {
			// 明显掉帧：收缩一点，掉得越狠收缩越快
			double drop = fps < target * 0.5D ? 0.12D : 0.06D;
			scale = (float) Math.max(minScale, scale - drop);
			struggling = true;
		} else if (fps > target * 1.1D) {
			// 帧率有余量：非常缓慢地恢复，避免来回抖动
			scale = (float) Math.min(1.0D, scale + 0.02D);
			struggling = false;
		} else {
			struggling = false;
		}
	}

	/** 重置所有状态（切换世界时调用）。 */
	public static void reset() {
		scale = 1.0F;
		ticks = 0;
		lastFps = -1;
		struggling = false;
		Stats.reset();
	}
}
