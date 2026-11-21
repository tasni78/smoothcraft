package com.smoothcraft.client;

import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.util.Util;

/**
 * 入场视距爬坡。
 *
 * 原版进世界后，渲染器会立刻按设定视距去请求/编译所有区块的网格，
 * 开局那几秒往往就是被这件事占满的（几百上千个区块一起排队）。
 *
 * 这里在「进入世界」时记一个时间戳，之后把有效视距从 startChunks 慢慢涨到设定值：
 * 只影响渲染与雾（Options#getEffectiveRenderDistance 的返回值），
 * 不改设置项、不触发区块重载，所以不会有「改视距导致整个场景重建」的抖动。
 */
public final class RenderDistanceRamp {
	private static long startedAt = -1L;
	private static int currentLimit = -1;
	private static int currentTarget = -1;

	private RenderDistanceRamp() {
	}

	/** 进入世界时调用，开始爬坡。 */
	public static void onWorldJoin() {
		startedAt = Util.getMillis();
		currentLimit = -1;
		currentTarget = -1;
	}

	/** 退出世界时调用，恢复原状。 */
	public static void reset() {
		startedAt = -1L;
		currentLimit = -1;
		currentTarget = -1;
	}

	public static boolean ramping() {
		return startedAt >= 0L;
	}

	/** 把「设置里的视距」钳到当前允许的值。返回 configured 表示不限制。 */
	public static int apply(int configured) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.worldLoad.enabled || !cfg.worldLoad.rampEnabled || startedAt < 0L) {
			return configured;
		}

		long elapsed = Util.getMillis() - startedAt;
		int perSecond = Math.max(1, cfg.worldLoad.chunksPerSecond);
		int allowed = Math.max(1, cfg.worldLoad.startChunks) + (int) (elapsed * perSecond / 1000L);

		if (allowed >= configured) {
			// 爬坡结束，之后完全不干预
			startedAt = -1L;
			currentLimit = configured;
			currentTarget = configured;
			return configured;
		}

		int limited = Math.min(configured, allowed);
		currentLimit = limited;
		currentTarget = configured;
		return limited;
	}

	/** 给 HUD 用的状态文本；不在爬坡时返回 null。 */
	public static String status() {
		if (!ramping() || currentLimit < 0 || currentTarget < 0 || currentLimit >= currentTarget) {
			return null;
		}
		return "视距 " + currentLimit + "→" + currentTarget;
	}
}
