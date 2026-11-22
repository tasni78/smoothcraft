package com.smoothcraft.client;

import net.minecraft.util.Util;

/**
 * 进入世界后经过了多少毫秒。
 *
 * 用途：给「提前放行」做保底 —— 原版要等脚下那一格网格编译完成才放行，
 * 我们允许不等，但也不能无限久地站在空洞里，所以最多等 N 毫秒就先放人进去。
 */
public final class WorldLoadTimer {
	private static long startedAt = -1L;

	private WorldLoadTimer() {
	}

	/** 每次进入世界（setLevel）时调用，重新开始计时。 */
	public static void reset() {
		startedAt = -1L;
	}

	/** 首次调用时开始计时，返回已经过的毫秒数。 */
	public static long elapsedMs() {
		if (startedAt < 0L) {
			startedAt = Util.getMillis();
			return 0L;
		}
		return Util.getMillis() - startedAt;
	}
}
