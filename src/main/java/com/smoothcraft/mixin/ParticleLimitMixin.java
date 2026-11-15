package com.smoothcraft.mixin;

import com.smoothcraft.client.PerfGovernor;
import com.smoothcraft.client.Stats;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 每刻粒子总量限制。
 *
 * 注入点：ParticleEngine#add —— 所有新粒子最终都要经过这里。
 * 超过预算的粒子直接不创建（连对象都不 new，省的是 CPU 和内存分配，不只是绘制）。
 *
 * 开了 followAdaptive 之后，帧率守护收缩剔除距离时，粒子预算也会等比缩小，
 * 于是「掉帧 → 少粒子 → 少开销」形成正反馈，比只砍距离见效更快。
 */
@Mixin(ParticleEngine.class)
public class ParticleLimitMixin {
	@Unique
	private int smoothcraft$addedThisTick;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$limitParticles(Particle particle, CallbackInfo ci) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.particleLimiting.enabled) {
			return;
		}

		int budget = Math.max(16, cfg.particleLimiting.maxPerTick);
		if (cfg.particleLimiting.followAdaptive) {
			budget = Math.max(16, (int) (budget * PerfGovernor.scale()));
		}

		if (smoothcraft$addedThisTick >= budget) {
			Stats.droppedParticles++;
			ci.cancel();
			return;
		}
		smoothcraft$addedThisTick++;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void smoothcraft$resetCounter(CallbackInfo ci) {
		smoothcraft$addedThisTick = 0;
	}
}
