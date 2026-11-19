package com.smoothcraft.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * 掉落物「同格同类」去重。
 *
 * 农场、刷怪塔附近经常有几十个同种掉落物挤在同一格里，视觉上本来就完全重叠。
 * 每帧只画第一个、其余跳过，画面几乎没有差别，但绘制数量能砍掉一大截。
 *
 * 实现要点：状态每帧清空一次（由 FrameResetMixin 调用 beginFrame），
 * 判断用 `Set.add` 的返回值 —— 已经存在返回 false，一次哈希查找搞定。
 */
public final class ItemAggregator {
	/** 0x9E3779B97F4A7C15 是 64 位黄金比例常数，乘它把坐标打散，减少相邻格子聚成一堆。 */
	private static final long HASH_MULTIPLIER = 0x9E3779B97F4A7C15L;

	private static final Set<Long> SEEN = new HashSet<>(256);

	private ItemAggregator() {
	}

	/** 每帧开始时清空（本帧已经画过的「格子 + 物品种类」集合）。 */
	public static void beginFrame() {
		if (!SEEN.isEmpty()) {
			SEEN.clear();
		}
	}

	/** 返回 true 表示同格同类的掉落物本帧已经画过，可以跳过。 */
	public static boolean isDuplicate(ItemEntity item) {
		int itemId = BuiltInRegistries.ITEM.getId(item.getItem().getItem());
		long key = item.blockPosition().asLong() * HASH_MULTIPLIER + itemId;
		return !SEEN.add(key);
	}
}
