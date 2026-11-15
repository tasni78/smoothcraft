package com.smoothcraft.mixin;

import com.smoothcraft.client.PerfGovernor;
import com.smoothcraft.client.Stats;
import com.smoothcraft.config.SmoothCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 远距离实体不渲染 —— 按类别给不同阈值。
 *
 * 注入点：EntityRenderDispatcher#shouldRender —— 原版为每个实体做可见性判断的统一入口，
 * 返回 false 就等于「这个实体这一帧不用画」，不影响实体自身的逻辑与碰撞。
 *
 * 分类的意义：掉落物和经验球在刷怪塔/农场附近能堆到上千个，
 * 它们每个都要走一遍渲染管线，但视觉上又最不重要，所以阈值压得最低。
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityCullMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$cullFarEntities(Entity entity, Frustum frustum, double camX, double camY, double camZ,
											 CallbackInfoReturnable<Boolean> cir) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled || !cfg.entityCulling.enabled) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		// 自己 / 正在骑乘的实体 / 第一人称手部 永远不能剔除
		if (entity == mc.getCameraEntity() || entity == mc.player) {
			return;
		}

		double threshold;
		if (entity instanceof Player) {
			if (cfg.entityCulling.keepPlayersAlways) {
				return;
			}
			threshold = cfg.entityCulling.playersDistance;
		} else if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
			threshold = cfg.entityCulling.itemsDistance;
		} else if (entity instanceof LivingEntity) {
			threshold = cfg.entityCulling.mobsDistance;
		} else {
			threshold = cfg.entityCulling.maxDistance;
		}

		double max = PerfGovernor.scale(threshold);
		if (entity.distanceToSqr(camX, camY, camZ) > max * max) {
			Stats.culledEntities++;
			cir.setReturnValue(false);
		}
	}
}
