package com.smoothcraft.mixin;

import com.smoothcraft.client.PerfGovernor;
import com.smoothcraft.client.Stats;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 远距离方块实体不渲染（箱子、告示牌、刷怪笼、床、钟……）。
 *
 * 注入点：BlockEntityRenderDispatcher#tryExtractRenderState。
 * 该方法返回 null 时原版调用点会直接跳过这个方块实体（LevelRenderer 里写了 null 判断），
 * 属于「官方本来就允许的空值路径」，所以最安全。
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityCullMixin {
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$cullFarBlockEntities(BlockEntity blockEntity, float partialTick,
												  ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
												  CallbackInfoReturnable<BlockEntityRenderState> cir) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.blockEntityCulling.enabled) {
			return;
		}

		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		double max = PerfGovernor.scale(cfg.blockEntityCulling.maxDistance);
		BlockPos pos = blockEntity.getBlockPos();
		double dx = pos.getX() + 0.5D - player.getX();
		double dy = pos.getY() + 0.5D - player.getY();
		double dz = pos.getZ() + 0.5D - player.getZ();

		if (dx * dx + dy * dy + dz * dz > max * max) {
			Stats.culledBlockEntities++;
			cir.setReturnValue(null);
		}
	}
}
