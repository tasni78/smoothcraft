package com.smoothcraft.mixin;

import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 限制名牌渲染距离。
 *
 * 注入点：EntityRenderer#shouldShowName —— 原版决定「要不要给这个实体画名字」的地方，
 * 第二个参数就是到相机的距离平方，直接拿来用，不需要自己算。
 * 注意这里只关名字，不关实体本身。
 */
@Mixin(EntityRenderer.class)
public class NameTagCullMixin {
	@Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true)
	private void smoothcraft$cullFarNameTags(Entity entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
		if (!Boolean.TRUE.equals(cir.getReturnValue())) {
			return;
		}

		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.nameTagCulling.enabled) {
			return;
		}

		double max = cfg.nameTagCulling.maxDistance;
		if (distanceSq > max * max) {
			cir.setReturnValue(false);
		}
	}
}
