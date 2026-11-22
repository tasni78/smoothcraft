package com.smoothcraft.client;

import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;
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
	/** 已进入世界但还没真正"看见画面"（加载界面还开着）。 */
	private static boolean pending;
	private static int currentLimit = -1;
	private static int currentTarget = -1;

	private RenderDistanceRamp() {
	}

	/** 进入世界时调用，等加载界面关掉之后才真正开始爬坡。 */
	public static void onWorldJoin() {
		pending = true;
		startedAt = -1L;
		currentLimit = -1;
		currentTarget = -1;
	}

	/** 退出世界时调用，恢复原状。 */
	public static void reset() {
		pending = false;
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
		if (!cfg.enabled || !cfg.worldLoad.enabled || !cfg.worldLoad.rampEnabled) {
			return configured;
		}

		// 加载界面还开着的时候不爬坡：那段时间不渲染世界，
		// 提前爬完等于白爬，还会让刚进世界的头几帧视距剧烈变化。
		if (pending) {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.level == null || mc.screen != null) {
				return configured;
			}
			startedAt = Util.getMillis();
			pending = false;
		}

		if (startedAt < 0L) {
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
