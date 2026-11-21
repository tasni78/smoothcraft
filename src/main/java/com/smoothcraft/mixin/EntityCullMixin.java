package com.smoothcraft.mixin;

import com.smoothcraft.client.ItemAggregator;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实体渲染剔除 + 掉落物聚合。
 *
 * 注入点：EntityRenderDispatcher#shouldRender —— 原版为每个实体做可见性判断的统一入口，
 * 返回 false 就等于「这个实体这一帧不用画」，不影响实体自身的逻辑与碰撞。
 *
 * 判断顺序很重要：先做距离剔除，再对「已经确定要画」的掉落物做同格去重。
 * 反过来的话，某个格子里的物品会先被标记成「画过了」、随后又被距离剔除掉，
 * 结果整格什么都不画 —— 那种「东西凭空消失」的 bug 就是这么来的。
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityCullMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void smoothcraft$cullAndAggregate(Entity entity, Frustum frustum, double camX, double camY, double camZ,
											  CallbackInfoReturnable<Boolean> cir) {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		if (!cfg.enabled) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		// 自己 / 正在骑乘的实体 / 第一人称手部 永远不能剔除
		if (entity == mc.getCameraEntity() || entity == mc.player) {
			return;
		}

		// 1) 距离剔除
		if (cfg.entityCulling.enabled && smoothcraft$isTooFar(cfg, entity, camX, camY, camZ)) {
			Stats.culledEntities++;
			cir.setReturnValue(false);
			return;
		}

		// 2) 掉落物同格同类聚合 —— 独立开关，不受实体剔除开关影响
		if (cfg.itemCluster.enabled && cfg.itemCluster.aggregateSameBlock
			&& entity instanceof ItemEntity item
			&& entity != mc.crosshairPickEntity
			&& ItemAggregator.isDuplicate(item)) {
			Stats.aggregatedItems++;
			cir.setReturnValue(false);
			return;
		}

		// 3) 经验球聚合（2×2×2 方块为格子）—— 刷怪塔门口经常一片经验球
		if (cfg.itemCluster.enabled && cfg.itemCluster.aggregateOrbs
			&& entity instanceof ExperienceOrb orb
			&& ItemAggregator.isDuplicateOrb(orb)) {
			Stats.aggregatedItems++;
			cir.setReturnValue(false);
		}
	}

	@Unique
	private static boolean smoothcraft$isTooFar(SmoothCraftConfig cfg, Entity entity,
												double camX, double camY, double camZ) {
		double threshold;
		if (entity instanceof Player) {
			if (cfg.entityCulling.keepPlayersAlways) {
				return false;
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
		return entity.distanceToSqr(camX, camY, camZ) > max * max;
	}
}
