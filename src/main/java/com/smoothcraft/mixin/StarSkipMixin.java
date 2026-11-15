package com.smoothcraft.mixin;

import com.smoothcraft.config.SmoothCraftConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 可选：跳过星空绘制（默认关闭）。
 * 星空是一千多个小四边形，收益不大，但在低端集显上关掉确实能省一点点，
 * 而且夜景基本没人盯着星星看。注入点是私有方法 renderStars，Mixin 允许。
 */
@Mixin(SkyRenderer.class)
public class StarSkipMixin {
	@Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$skipStars(float partialTick, PoseStack poseStack, CallbackInfo ci) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (cfg.enabled && cfg.rendering.skipStars) {
			ci.cancel();
		}
	}
}
