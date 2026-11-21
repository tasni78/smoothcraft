package com.smoothcraft.mixin;

import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 远处告示牌不渲染文字（木板照画）。
 *
 * 文字是单独一层几何（每个字都要排版、生成字体网格），远处既看不清又白费。
 * 做法是在渲染状态提取完之后，把正反面的文字换成一份空的 SignText —— 木板模型不受影响。
 *
 * 共用一份静态空实例，避免每帧 new；渲染器只读取文本内容，不会改写它。
 */
@Mixin(AbstractSignRenderer.class)
public class SignTextCullMixin {
	private static final SignText EMPTY_TEXT = new SignText();

	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
		at = @At("TAIL"))
	private void smoothcraft$cullFarText(SignBlockEntity sign, SignRenderState state, float partialTick,
										 Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay,
										 CallbackInfo ci) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.signText.enabled) {
			return;
		}

		BlockPos pos = state.blockPos;
		if (pos == null) {
			return;
		}

		double max = cfg.signText.maxDistance;
		double dx = pos.getX() + 0.5D - cameraPos.x;
		double dy = pos.getY() + 0.5D - cameraPos.y;
		double dz = pos.getZ() + 0.5D - cameraPos.z;

		if (dx * dx + dy * dy + dz * dz > max * max) {
			state.frontText = EMPTY_TEXT;
			state.backText = EMPTY_TEXT;
		}
	}
}
