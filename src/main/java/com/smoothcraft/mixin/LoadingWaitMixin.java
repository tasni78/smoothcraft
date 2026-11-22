package com.smoothcraft.mixin;

import com.smoothcraft.client.WorldLoadTimer;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 提前放行。
 *
 * 原版的就绪判定是 `levelRenderer.isSectionCompiledAndVisible(player.blockPosition())`，
 * 也就是必须等「脚下那一格的网格编译完成」才让你进世界 —— 这一段通常是几秒。
 *
 * 这里放宽成「玩家所在区块的数据已经收到」就放行：地形数据到位，能站人、能走，
 * 只是网格还在编译，所以进世界瞬间可能会看到一小片空洞，几帧到几秒内会补齐。
 * 想稳一点把这功能关掉即可（配置 worldLoad.skipLoadingWait）。
 */
@Mixin(targets = "net.minecraft.client.multiplayer.LevelLoadTracker$WaitingForPlayerChunk")
public abstract class LoadingWaitMixin {
	@Shadow
	@Final
	private LocalPlayer player;

	@Shadow
	@Final
	private ClientLevel level;

	@Inject(method = "isReady", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$allowBeforeCompile(CallbackInfoReturnable<Boolean> cir) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.worldLoad.enabled || !cfg.worldLoad.skipLoadingWait) {
			return;
		}
		if (this.player == null || this.level == null) {
			return;
		}

		BlockPos pos = this.player.blockPosition();
		if (!this.level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
			return;
		}

		// 早期版本是「拿到区块数据就立刻放行」，结果进世界时脚下网格还没编译完，
		// 视野里一片空洞、看着像闪白。现在改成「最多等 N 毫秒」：
		// 网格编译得快就跟原版一样，编译得慢也不会干等到天荒地老。
		int maxWait = Math.max(0, cfg.worldLoad.skipWaitMaxMs);
		if (WorldLoadTimer.elapsedMs() >= maxWait) {
			cir.setReturnValue(true);
		}
	}
}
