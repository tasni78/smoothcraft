package com.smoothcraft.mixin;

import com.smoothcraft.client.ItemAggregator;
import com.smoothcraft.client.Stats;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 每帧开头的清理钩子。
 *
 * runTick 每帧调用一次，在这里清空「本帧已画过的掉落物」集合，
 * 以及重置每帧统计。放在 runTick 而不是某个渲染方法里，
 * 是因为它一定先于本帧的任何实体渲染。
 */
@Mixin(Minecraft.class)
public class FrameResetMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void smoothcraft$beginFrame(boolean renderLevel, CallbackInfo ci) {
		ItemAggregator.beginFrame();
		Stats.resetFrameCounter();
	}
}
