package com.smoothcraft.server;

import com.smoothcraft.config.ResolvedConfig;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

/**
 * 服务端 TPS 守护：监控 MSPT，在服务器吃力时对「远处的杂项实体」降频。
 *
 * 设计原则是「平时完全不动手」：
 *   1. 只有单刻耗时超过阈值（默认 45ms，即 TPS 掉到 22 以下）才开始节流；
 *   2. 只碰配置里列出的实体类型，默认仅掉落物和经验球；
 *   3. 只有附近 32 格内没有任何玩家时才降频 —— 玩家眼前的掉落物永远正常跳动。
 *
 * 这样即使配置写错，最坏情况也只是「远处掉落物动得慢一点」，不会影响玩法逻辑。
 */
public final class ServerPerf {
	private static volatile double mspt;
	private static volatile double tps = 20.0D;

	private static long throttledTotal;
	private static long throttledThisSecond;
	private static long throttledLastSecond;

	private ServerPerf() {
	}

	public static void onEndTick(MinecraftServer server) {
		float smoothed = server.getCurrentSmoothedTickTime();
		mspt = Math.max(0.0D, smoothed);
		tps = smoothed <= 0.0F ? 20.0D : Math.min(20.0D, 1000.0D / smoothed);

		if (server.getTickCount() % 20 == 0) {
			throttledLastSecond = throttledThisSecond;
			throttledThisSecond = 0;
		}
	}

	public static double mspt() {
		return mspt;
	}

	public static double tps() {
		return tps;
	}

	public static long throttledTotal() {
		return throttledTotal;
	}

	public static long throttledLastSecond() {
		return throttledLastSecond;
	}

	/** 返回 true 表示这一 tick 跳过该实体（降频）。 */
	public static boolean shouldThrottle(Entity entity) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.serverThrottle.enabled) {
			return false;
		}

		// 服务器不忙的时候完全不干预
		if (mspt < cfg.serverThrottle.msptThreshold) {
			return false;
		}

		Set<EntityType<?>> targets = ResolvedConfig.entityTypes();
		if (targets.isEmpty() || !targets.contains(entity.getType())) {
			return false;
		}

		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}

		// 用游戏时间而不是实体自己的 tickCount —— 被跳过的实体 tickCount 不再增长，
		// 拿它做取模会导致「永远被跳过」的死循环。
		int ratio = Math.max(2, cfg.serverThrottle.skipRatio);
		long phase = level.getGameTime() + entity.getId();
		if (phase % ratio == 0L) {
			return false;
		}

		double maxSq = cfg.serverThrottle.maxDistance * cfg.serverThrottle.maxDistance;
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(entity) < maxSq) {
				return false;
			}
		}

		throttledTotal++;
		throttledThisSecond++;
		return true;
	}
}
