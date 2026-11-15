package com.smoothcraft.mixin;

import com.smoothcraft.client.PerfGovernor;
import com.smoothcraft.client.Stats;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 远处实体的落地阴影不画。
 *
 * 注入点：EntityRenderer#getShadowRadius。返回 0 时原版认为没有阴影，直接跳过那块半透明四边形，
 * 所以不会出现「阴影残留」之类的问题——只是远处不画了。
 * 距离直接取渲染状态里的 distanceToCameraSq，不用自己再算一遍。
 */
@Mixin(EntityRenderer.class)
public class ShadowCullMixin {
	@Inject(method = "getShadowRadius", at = @At("RETURN"), cancellable = true)
	private void smoothcraft$cullFarShadows(EntityRenderState state, CallbackInfoReturnable<Float> cir) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.entityCulling.cullShadows) {
			return;
		}
		if (cir.getReturnValueF() <= 0.0F) {
			return;
		}

		double max = PerfGovernor.scale(cfg.entityCulling.shadowDistance);
		if (state.distanceToCameraSq > max * max) {
			Stats.culledShadows++;
			cir.setReturnValue(0.0F);
		}
	}
}
