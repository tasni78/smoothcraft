package com.smoothcraft.mixin;

import com.smoothcraft.client.RenderDistanceRamp;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 入场视距爬坡的落点。
 *
 * LevelRenderer 每帧通过 Options#getEffectiveRenderDistance() 取有效视距（雾、可见区块范围都用它），
 * 这里按爬坡进度把返回值钳小。因为只是改「读到的值」，不触发设置变更回调，
 * 所以不会引起区块重载；ClientChunkCache 也不读这个方法，区块该收的照收。
 */
@Mixin(Options.class)
public class RenderDistanceRampMixin {
	@Inject(method = "getEffectiveRenderDistance", at = @At("RETURN"), cancellable = true)
	private void smoothcraft$clampDuringRamp(CallbackInfoReturnable<Integer> cir) {
		int configured = cir.getReturnValue();
		if (configured <= 0) {
			return;
		}
		int limited = RenderDistanceRamp.apply(configured);
		if (limited < configured) {
			cir.setReturnValue(limited);
		}
	}
}
