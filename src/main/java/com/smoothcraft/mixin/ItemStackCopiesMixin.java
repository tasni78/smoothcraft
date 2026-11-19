package com.smoothcraft.mixin;

import com.smoothcraft.client.Stats;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 限制「一叠掉落物画几份模型」。
 *
 * 原版 getRenderedAmount 的规则是：1→1 份、≥16→2 份、≥32→3 份、≥48→4 份、满 64→5 份。
 * 也就是说一个满堆叠的掉落物要画 5 份模型 —— 农场里几百个满堆叠，光这一项就吃掉大量几何量。
 *
 * 这里只压「画几份」，不动堆叠数量本身，所以拾取、合并、消失计时全都不受影响，纯渲染。
 * 注入静态方法要求 handler 也是 static。
 */
@Mixin(ItemClusterRenderState.class)
public class ItemStackCopiesMixin {
	@Inject(method = "getRenderedAmount", at = @At("RETURN"), cancellable = true)
	private static void smoothcraft$capRenderedCopies(int count, CallbackInfoReturnable<Integer> cir) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.itemCluster.enabled) {
			return;
		}

		int cap = Math.max(1, Math.min(5, cfg.itemCluster.maxCopiesPerStack));
		int current = cir.getReturnValue();
		if (current > cap) {
			Stats.suppressedCopies += current - cap;
			cir.setReturnValue(cap);
		}
	}
}
