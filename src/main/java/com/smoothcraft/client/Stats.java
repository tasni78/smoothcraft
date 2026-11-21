package com.smoothcraft.client;

/**
 * 轻量运行统计，给 HUD 用。
 * 只在客户端主线程写入，不需要原子类型。
 */
public final class Stats {
	// 累计值（每秒被 PerfGovernor 结算一次）
	public static int culledEntities;
	public static int culledBlockEntities;
	public static int culledShadows;
	public static int droppedParticles;
	public static int filteredParticles;

	// 上一秒的结算结果（HUD 显示这个）
	public static int culledEntitiesPerSec;
	public static int culledBlockEntitiesPerSec;
	public static int culledShadowsPerSec;
	public static int droppedParticlesPerSec;
	public static int filteredParticlesPerSec;

	private Stats() {
	}

	// ---- 每帧统计（由 FrameResetMixin 在每帧开头清零）----
	/** 本帧被「同格同类」聚合掉的掉落物数量。 */
	public static int aggregatedItems;
	/** 本帧被压掉的额外模型份数（如 64 堆叠原本 5 份、压到 3 份 → 记 2）。 */
	public static int suppressedCopies;
	/** 本帧被节流推迟的区块编译任务数（不是丢弃，下一帧会重试）。 */
	public static int throttledChunkRebuilds;

	public static void resetFrameCounter() {
		aggregatedItems = 0;
		suppressedCopies = 0;
		throttledChunkRebuilds = 0;
	}

	public static void flush() {
		culledEntitiesPerSec = culledEntities;
		culledBlockEntitiesPerSec = culledBlockEntities;
		culledShadowsPerSec = culledShadows;
		droppedParticlesPerSec = droppedParticles;
		filteredParticlesPerSec = filteredParticles;

		culledEntities = 0;
		culledBlockEntities = 0;
		culledShadows = 0;
		droppedParticles = 0;
		filteredParticles = 0;
	}

	public static void reset() {
		culledEntities = 0;
		culledBlockEntities = 0;
		culledShadows = 0;
		droppedParticles = 0;
		filteredParticles = 0;

		culledEntitiesPerSec = 0;
		culledBlockEntitiesPerSec = 0;
		culledShadowsPerSec = 0;
		droppedParticlesPerSec = 0;
		filteredParticlesPerSec = 0;
	}
}
