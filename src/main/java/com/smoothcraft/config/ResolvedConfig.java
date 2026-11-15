package com.smoothcraft.config;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 把配置里的字符串 ID 解析成注册表对象集合，并缓存起来。
 *
 * 为什么要缓存：粒子/实体的判断是在每帧、每刻的热路径上跑的，
 * 每次都做一次字符串比较或者注册表查找会白白吃掉 CPU。
 * 解析一次成 Set 之后，判断就是一次哈希查找。
 */
public final class ResolvedConfig {
	private static SmoothCraftConfig cachedConfig;
	private static List<String> cachedParticleIds;
	private static List<String> cachedEntityIds;

	private static Set<ParticleType<?>> particleTypes = Collections.emptySet();
	private static Set<EntityType<?>> entityTypes = Collections.emptySet();

	private ResolvedConfig() {
	}

	public static Set<ParticleType<?>> particleTypes() {
		refreshIfNeeded();
		return particleTypes;
	}

	public static Set<EntityType<?>> entityTypes() {
		refreshIfNeeded();
		return entityTypes;
	}

	private static void refreshIfNeeded() {
		SmoothCraftConfig cfg = SmoothCraftConfig.get();
		List<String> pids = cfg.particleFilter.types;
		List<String> eids = cfg.serverThrottle.entityTypes;

		// 配置对象或列表被替换（热重载）时才重新解析
		if (cfg == cachedConfig && pids == cachedParticleIds && eids == cachedEntityIds) {
			return;
		}

		cachedConfig = cfg;
		cachedParticleIds = pids;
		cachedEntityIds = eids;

		Set<ParticleType<?>> particles = new LinkedHashSet<>();
		if (pids != null) {
			for (String raw : pids) {
				Identifier id = parse(raw);
				if (id == null) {
					continue;
				}
				BuiltInRegistries.PARTICLE_TYPE.getOptional(id).ifPresent(particles::add);
			}
		}
		particleTypes = Collections.unmodifiableSet(particles);

		Set<EntityType<?>> entities = new LinkedHashSet<>();
		if (eids != null) {
			for (String raw : eids) {
				Identifier id = parse(raw);
				if (id == null) {
					continue;
				}
				BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(entities::add);
			}
		}
		entityTypes = Collections.unmodifiableSet(entities);
	}

	private static Identifier parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String s = raw.trim();
		int i = s.indexOf(':');
		try {
			if (i > 0) {
				return Identifier.fromNamespaceAndPath(s.substring(0, i), s.substring(i + 1));
			}
			return Identifier.fromNamespaceAndPath("minecraft", s);
		} catch (Exception e) {
			return null;
		}
	}
}
