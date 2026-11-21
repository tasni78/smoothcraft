package com.smoothcraft.client;

import com.smoothcraft.config.SmoothCraftConfig;

/**
 * 区块编译节流。
 *
 * 跑图卡顿的成因不是"平均帧数低"，而是帧时间尖峰：区块一进入视野就集体变脏，
 * 所有编译任务同时入队，随后几帧里网格上传集中爆发，帧时间从 8ms 跳到 40ms。
 *
 * 这里给每帧提交给编译队列的任务数设一个上限，把洪峰摊到多帧。
 *
 * 为什么跳过是安全的：LevelRenderer 每帧都会遍历脏区块并调用 rebuildSectionAsync，
 * 而该方法只做「创建任务 + 入队」，脏标记不在这里清 —— 所以本帧被跳过的区块下一帧还会再来，
 * 不会出现"永久不重建"的空洞。
 *
 * 玩家自己改动造成的脏区块（isDirtyFromPlayer，比如刚挖掉一块方块）永远放行，
 * 否则会出现"挖了半天眼前还是旧地形"的诡异现象。
 */
public final class ChunkCompileBudget {
	private static int allowedThisFrame;

	private ChunkCompileBudget() {
	}

	/** 每帧开头重置预算（由 FrameResetMixin 调用）。 */
	public static void beginFrame() {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		allowedThisFrame = cfg.chunkCompile.enabled
			? Math.max(1, cfg.chunkCompile.maxPerFrame)
			: Integer.MAX_VALUE;
	}

	/** 返回 true 表示本帧还有额度，可以提交。 */
	public static boolean tryAcquire(boolean fromPlayer) {
		// 玩家自己改动引起的重建永远放行，避免眼前地形不更新
		if (fromPlayer) {
			return true;
		}
		if (allowedThisFrame <= 0) {
			Stats.throttledChunkRebuilds++;
			return false;
		}
		allowedThisFrame--;
		return true;
	}
}
