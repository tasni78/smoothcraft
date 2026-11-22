package com.smoothcraft.mixin;

import com.smoothcraft.client.ChunkCompileBudget;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 区块编译节流。
 *
 * 注入 RenderSection#rebuildSectionAsync —— LevelRenderer 每帧遍历脏区块时调它，
 * 方法体只有两句：createCompileTask + schedule，脏标记不在这里清。
 * 所以在这里取消是安全的：本帧被跳过的区块下一帧还会被 LevelRenderer 重新发现并重试。
 *
 * 两个永远不节流的例外：
 *   1. 玩家自己改动造成的脏区块（挖方块、放方块）—— 否则眼前地形不更新
 *   2. 玩家周围若干个区块内的段 —— 脚下的地形必须最快重建，
 *      否则会出现"站着看空洞、画面闪白"的现象
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class ChunkCompileThrottleMixin {
	@Shadow
	public abstract boolean isDirtyFromPlayer();

	@Shadow
	public abstract BlockPos getRenderOrigin();

	@Inject(method = "rebuildSectionAsync", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$throttleRebuild(RenderRegionCache cache, CallbackInfo ci) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.chunkCompile.enabled) {
			return;
		}

		if (this.isDirtyFromPlayer() || smoothcraft$nearPlayer(cfg.chunkCompile.playerExemptRadius)) {
			return;
		}

		if (!ChunkCompileBudget.tryAcquire(false)) {
			ci.cancel();
		}
	}

	/** 段中心是否落在玩家水平方向 N 个区块以内。 */
	private boolean smoothcraft$nearPlayer(int radiusChunks) {
		if (radiusChunks <= 0) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			// 没有玩家（加载中）时不节流，避免把首屏区块拖慢
			return true;
		}

		BlockPos origin = this.getRenderOrigin();
		double cx = origin.getX() + 8.0D - mc.player.getX();
		double cz = origin.getZ() + 8.0D - mc.player.getZ();
		double radius = radiusChunks * 16.0D;
		return cx * cx + cz * cz <= radius * radius;
	}
}
