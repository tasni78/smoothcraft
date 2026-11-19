package com.smoothcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SmoothCraft 配置文件。
 * 存放于 .minecraft/config/smoothcraft.json，启动时读取，缺失时自动生成默认值。
 * 游戏内按 F7 可热重载。
 */
public final class SmoothCraftConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("smoothcraft-config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static SmoothCraftConfig instance;

	/** 总开关：关掉后本模组的所有优化都不再生效（方便排查问题）。 */
	public boolean enabled = true;

	public EntityCulling entityCulling = new EntityCulling();
	public BlockEntityCulling blockEntityCulling = new BlockEntityCulling();
	public ParticleLimiting particleLimiting = new ParticleLimiting();
	/** 按距离过滤指定粒子（火把、营火这类，客户端）。 */
	public ParticleFilter particleFilter = new ParticleFilter();
	/** 掉落物数量过多时的渲染优化。 */
	public ItemCluster itemCluster = new ItemCluster();
	/** 服务端：MSPT 过高时对远处杂项实体降频。 */
	public ServerThrottle serverThrottle = new ServerThrottle();
	public NameTagCulling nameTagCulling = new NameTagCulling();
	public Rendering rendering = new Rendering();
	public Adaptive adaptive = new Adaptive();
	public Hud hud = new Hud();

	public static class EntityCulling {
		/** 是否启用实体渲染剔除。 */
		public boolean enabled = true;
		/** 普通实体（掉落方块、船、矿车、箭等）的剔除距离。 */
		public double maxDistance = 96.0D;
		/** 生物（动物/怪物）的剔除距离。 */
		public double mobsDistance = 80.0D;
		/** 掉落物与经验球的剔除距离，这类实体数量最容易爆炸。 */
		public double itemsDistance = 32.0D;
		/** 玩家的剔除距离（keepPlayersAlways 为 true 时不生效）。 */
		public double playersDistance = 128.0D;
		/** 超出距离的实体不再绘制落地阴影（省一批半透明四边形）。 */
		public boolean cullShadows = true;
		/** 阴影剔除距离。 */
		public double shadowDistance = 48.0D;
		/** 玩家永远渲染（防止 PVP 时看不到远处对手）。 */
		public boolean keepPlayersAlways = true;
	}

	public static class BlockEntityCulling {
		/** 是否启用方块实体渲染剔除（箱子、告示牌、刷怪笼等）。 */
		public boolean enabled = true;
		/** 超过这个距离（格）的方块实体不再渲染。 */
		public double maxDistance = 64.0D;
	}

	public static class ParticleLimiting {
		/** 是否启用粒子总量限制。 */
		public boolean enabled = true;
		/** 每游戏刻最多新建多少个粒子，超出部分直接丢弃。 */
		public int maxPerTick = 400;
		/** 帧率守护启动时，预算是否跟着等比缩小。 */
		public boolean followAdaptive = true;
	}

	public static class ParticleFilter {
		/** 是否按距离过滤指定粒子。 */
		public boolean enabled = true;
		/** 超过这个距离（格）就不再生成这些粒子。 */
		public double maxDistance = 24.0D;
		/**
		 * 要过滤的粒子类型（注册表 ID）。默认是火把火焰、各类烟、营火烟、岩浆滴落这类
		 * 「贴图小、数量多、远处根本看不出」的环境粒子。
		 */
		public java.util.List<String> types = new java.util.ArrayList<>(java.util.List.of(
			"minecraft:flame",
			"minecraft:small_flame",
			"minecraft:soul_fire_flame",
			"minecraft:smoke",
			"minecraft:large_smoke",
			"minecraft:campfire_cosy_smoke",
			"minecraft:campfire_signal_smoke",
			"minecraft:lava",
			"minecraft:dripping_lava",
			"minecraft:falling_lava",
			"minecraft:spore_blossom_air",
			"minecraft:crimson_spore",
			"minecraft:warped_spore"
		));
	}

	public static class ItemCluster {
		/** 是否启用掉落物渲染优化。 */
		public boolean enabled = true;
		/**
		 * 一叠最多画几份模型。原版按数量递增：1→1、≥16→2、≥32→3、≥48→4、满 64→5 份。
		 * 压到 3 份观感几乎不变，但满堆叠的几何量少了 40%。
		 */
		public int maxCopiesPerStack = 3;
		/** 同一方块位置内的同种物品，一帧只画一个（其余跳过），农场场景收益最大。 */
		public boolean aggregateSameBlock = true;
	}

	public static class ServerThrottle {
		/** 是否启用服务端远处实体降频。 */
		public boolean enabled = true;
		/** 距离最近玩家超过这个距离（格）才考虑降频。 */
		public double maxDistance = 32.0D;
		/** 只有服务器单刻耗时高于这个值（毫秒）才开始降频 —— 平时完全不干预。 */
		public double msptThreshold = 45.0D;
		/** 每 N 刻 tick 一次（2 = 隔一刻，3 = 三刻一次）。 */
		public int skipRatio = 2;
		/** 参与降频的实体类型（注册表 ID）。默认只有掉落物和经验球。 */
		public java.util.List<String> entityTypes = new java.util.ArrayList<>(java.util.List.of(
			"minecraft:item",
			"minecraft:experience_orb"
		));
	}

	public static class NameTagCulling {
		/** 是否限制名牌渲染距离。 */
		public boolean enabled = true;
		/** 超过这个距离（格）不再显示名字。 */
		public double maxDistance = 32.0D;
	}

	public static class Rendering {
		/** 关闭云层渲染（能省一点填充率，视觉上有变化，默认关闭）。 */
		public boolean disableClouds = false;
		/** 关闭星空绘制（夜景里的一批小四边形，收益很小，默认关闭）。 */
		public boolean skipStars = false;
	}

	public static class Adaptive {
		/** 帧率守护：掉帧时自动收缩剔除距离，回到目标帧率后再慢慢恢复。 */
		public boolean enabled = true;
		/** 目标帧率。 */
		public int targetFps = 60;
		/** 自适应的最低收缩比例（0.4 = 最多把剔除距离压到原来的 40%）。 */
		public double minScale = 0.4D;
	}

	public static class Hud {
		/** 性能面板开关，可用 F8 随时切换。 */
		public boolean enabled = true;
		/** 面板左上角坐标（GUI 像素）。 */
		public int x = 4;
		public int y = 4;
		/** 是否绘制半透明背景。 */
		public boolean background = true;
		/** 背景不透明度 0~255。 */
		public int backgroundAlpha = 140;
		/** 文字缩放百分比，100 = 原始大小（允许 50~200）。 */
		public float scalePercent = 100.0F;
		/** 拖动时是否吸附到 8 像素网格。 */
		public boolean snapToGrid = true;

		/** 归一化后的缩放系数。 */
		public float scale() {
			float percent = Math.max(50.0F, Math.min(200.0F, scalePercent));
			return percent / 100.0F;
		}

		/** 实际生效的背景 alpha：关掉背景就返回 0。 */
		public int effectiveAlpha() {
			return background ? Math.max(0, Math.min(255, backgroundAlpha)) : 0;
		}
	}

	public static SmoothCraftConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("smoothcraft.json");
	}

	public static synchronized void load() {
		Path path = configPath();
		SmoothCraftConfig loaded = null;

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, SmoothCraftConfig.class);
			} catch (Exception e) {
				LOGGER.warn("[SmoothCraft] 配置文件读取失败，将使用默认值。", e);
			}
		}

		if (loaded == null) {
			loaded = new SmoothCraftConfig();
		}

		// 老配置缺少新字段时（Gson 会留空），用默认值补上
		loaded.fillDefaults();
		instance = loaded;
		save();
	}

	/** 热重载：重新读盘，失败则保留当前配置。 */
	public static synchronized void reload() {
		load();
		LOGGER.info("[SmoothCraft] 配置已重新加载。");
	}

	private void fillDefaults() {
		if (entityCulling == null) entityCulling = new EntityCulling();
		if (blockEntityCulling == null) blockEntityCulling = new BlockEntityCulling();
		if (particleLimiting == null) particleLimiting = new ParticleLimiting();
		if (particleFilter == null) particleFilter = new ParticleFilter();
		if (itemCluster == null) itemCluster = new ItemCluster();
		if (serverThrottle == null) serverThrottle = new ServerThrottle();
		if (nameTagCulling == null) nameTagCulling = new NameTagCulling();
		if (rendering == null) rendering = new Rendering();
		if (adaptive == null) adaptive = new Adaptive();
		if (hud == null) hud = new Hud();

		// 老配置里缺的列表字段补上默认值
		if (particleFilter.types == null) {
			particleFilter.types = new ParticleFilter().types;
		}
		if (serverThrottle.entityTypes == null) {
			serverThrottle.entityTypes = new ServerThrottle().entityTypes;
		}
	}

	public static synchronized void save() {
		if (instance == null) {
			return;
		}
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("[SmoothCraft] 配置文件写入失败：{}", path, e);
		}
	}
}
