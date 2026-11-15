package com.smoothcraft.mixin;

import com.smoothcraft.server.ServerPerf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务端远处实体降频。
 *
 * 注入点：ServerLevel#tickNonPassenger —— 服务端每一刻给每个实体跑 tick() 的唯一入口，
 * 在 HEAD 取消就等于「这一 tick 不给这个实体算」，它的 AI、物理、计时都会跳过一帧。
 * 因为只对掉落物 / 经验球生效，而且离玩家 32 格内不碰，玩家是感觉不到的。
 */
@Mixin(ServerLevel.class)
public class ServerThrottleMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$throttleFarEntities(Entity entity, CallbackInfo ci) {
		if (ServerPerf.shouldThrottle(entity)) {
			ci.cancel();
		}
	}
}
