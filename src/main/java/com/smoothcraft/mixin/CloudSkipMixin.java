package com.smoothcraft.mixin;

import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 可选：完全跳过云层绘制（默认关闭）。
 * 高空视野或云层密集时这一步的填充率开销不小，关掉能换几帧。
 */
@Mixin(CloudRenderer.class)
public class CloudSkipMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$skipClouds(int color, CloudStatus status, float partialTick, int ticks,
										Vec3 cameraPos, long gameTime, float f, CallbackInfo ci) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (cfg.enabled && cfg.rendering.disableClouds) {
			ci.cancel();
		}
	}
}
