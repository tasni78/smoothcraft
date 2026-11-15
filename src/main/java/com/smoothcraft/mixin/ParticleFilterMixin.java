package com.smoothcraft.mixin;

import com.smoothcraft.client.PerfGovernor;
import com.smoothcraft.client.Stats;
import com.smoothcraft.config.ResolvedConfig;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * 按距离过滤指定粒子（火把火焰、各类烟、营火烟、岩浆滴落…）。
 *
 * 注入点：ParticleEngine#makeParticle —— 这是粒子「真正被 new 出来」的地方，
 * 返回 null 时上层 createParticle 已经做了判空（原版对未知粒子类型就返回 null），
 * 所以直接返回 null 是官方允许的路径，不会炸。
 *
 * 好处是省在两头：既不用构造粒子对象（CPU + 内存分配），也不用进渲染管线。
 */
@Mixin(ParticleEngine.class)
public class ParticleFilterMixin {
	@Inject(method = "makeParticle", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$filterFarParticles(ParticleOptions options, double x, double y, double z,
												double xSpeed, double ySpeed, double zSpeed,
												CallbackInfoReturnable<Particle> cir) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.particleFilter.enabled) {
			return;
		}

		Set<ParticleType<?>> filtered = ResolvedConfig.particleTypes();
		if (filtered.isEmpty()) {
			return;
		}
		if (!filtered.contains(options.getType())) {
			return;
		}

		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		double max = PerfGovernor.scale(cfg.particleFilter.maxDistance);
		if (player.distanceToSqr(x, y, z) > max * max) {
			Stats.filteredParticles++;
			cir.setReturnValue(null);
		}
	}
}
