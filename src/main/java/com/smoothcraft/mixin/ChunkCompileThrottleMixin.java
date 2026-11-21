package com.smoothcraft.mixin;

import com.smoothcraft.client.ChunkCompileBudget;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
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
 * 注意目标类是包内可见的内部类，只能用 targets = "..." 指定，方法用 @Shadow 抽象声明。
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class ChunkCompileThrottleMixin {
	@Shadow
	public abstract boolean isDirtyFromPlayer();

	@Inject(method = "rebuildSectionAsync", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$throttleRebuild(RenderRegionCache cache, CallbackInfo ci) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.chunkCompile.enabled) {
			return;
		}
		if (!ChunkCompileBudget.tryAcquire(this.isDirtyFromPlayer())) {
			ci.cancel();
		}
	}
}
