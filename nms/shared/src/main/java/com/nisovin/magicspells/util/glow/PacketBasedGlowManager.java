package com.nisovin.magicspells.util.glow;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public abstract class PacketBasedGlowManager implements GlowManager {

	private static boolean firstInstance = true;

	protected final Object2ObjectMap<Pair<UUID, UUID>, GlowDataMap> perPlayerGlows = new Object2ObjectOpenHashMap<>();
	protected final Object2ObjectMap<UUID, GlowDataMap> glows = new Object2ObjectOpenHashMap<>();
	protected int sequence = Integer.MIN_VALUE;

	@Override
	public void load() {
		if (firstInstance) {
			Bukkit.getOnlinePlayers().forEach(this::sendTeamPackets);
			firstInstance = false;
		}
	}

	@Override
	public void unload() {
		synchronized (this) {
			glows.forEach((key, value) -> {
				Entity entity = Bukkit.getEntity(key);
				if (entity == null) return;

				resetGlow(null, entity, value.get(), false);
			});

			perPlayerGlows.forEach((key, value) -> {
				if (glows.containsKey(key.right())) return;

				Player player = Bukkit.getPlayer(key.left());
				if (player == null) return;

				Entity entity = Bukkit.getEntity(key.right());
				if (entity == null) return;

				resetGlow(player, entity, value.get(), false);
			});

			glows.clear();
			perPlayerGlows.clear();
		}
	}

	@Override
	public void applyGlow(@NotNull Entity entity, @NotNull NamespacedKey key, @Nullable NamedTextColor color, int priority, @Range(from = 0, to = Integer.MAX_VALUE) int duration) {
		GlowData data = new GlowData(priority, sequence++, color);
		UUID uuid = entity.getUniqueId();

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(null, uuid);

			GlowDataMap map = glows.computeIfAbsent(uuid, i -> new GlowDataMap());

			GlowData old = map.put(key, data);
			if (old != null && old.taskId != -1) cancelTask(old.taskId);

			if (duration > 0) {
				data.taskId = scheduleDelayedTask(() -> {
					synchronized (this) {
						Entity e = Bukkit.getEntity(uuid);
						if (e == null) {
							map.remove(key);
							if (map.isEmpty()) glows.remove(uuid);
							return;
						}

						removeGlow(e, key);
					}
				}, duration);
			}

			curr = getGlowData(null, uuid);
		}

		updateGlow(null, entity, prev, curr);
	}

	@Override
	public void applyGlow(@NotNull Player player, @NotNull Entity entity, @NotNull NamespacedKey key, @NotNull NamedTextColor color, int priority, @Range(from = 0, to = Integer.MAX_VALUE) int duration) {
		Pair<UUID, UUID> pair = Pair.of(player.getUniqueId(), entity.getUniqueId());
		GlowData data = new GlowData(priority, sequence++, color);

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(pair);

			GlowDataMap map = perPlayerGlows.computeIfAbsent(pair, i -> new GlowDataMap());

			GlowData old = map.put(key, data);
			if (old != null && old.taskId != -1) cancelTask(old.taskId);

			if (duration > 0) {
				data.taskId = scheduleDelayedTask(() -> {
					synchronized (this) {
						Player p = Bukkit.getPlayer(pair.left());
						if (p == null) {
							map.remove(key);
							if (map.isEmpty()) perPlayerGlows.remove(pair);
							return;
						}

						Entity e = Bukkit.getEntity(pair.right());
						if (e == null) {
							map.remove(key);
							if (map.isEmpty()) perPlayerGlows.remove(pair);
							return;
						}

						removeGlow(p, e, key);
					}
				}, duration);
			}

			curr = getGlowData(pair);
		}

		updateGlow(player, entity, prev, curr);
	}

	@Override
	public void removeGlow(@NotNull Entity entity, @NotNull NamespacedKey key) {
		UUID uuid = entity.getUniqueId();

		GlowDataMap map = glows.get(uuid);
		if (map == null) return;

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(null, uuid);

			map.remove(key);
			if (map.isEmpty()) glows.remove(uuid);

			curr = getGlowData(null, uuid);
		}

		if (curr == null) {
			resetGlow(null, entity, prev, true);
			return;
		}

		updateGlow(null, entity, prev, curr);
	}

	@Override
	public void removeGlow(@NotNull Player player, @NotNull Entity entity, @NotNull NamespacedKey key) {
		Pair<UUID, UUID> pair = Pair.of(player.getUniqueId(), entity.getUniqueId());

		GlowDataMap map = perPlayerGlows.get(pair);
		if (map == null) return;

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(pair);

			map.remove(key);
			if (map.isEmpty()) perPlayerGlows.remove(pair);

			curr = getGlowData(pair);
		}

		if (curr == null) {
			resetGlow(player, entity, prev, false);
			return;
		}

		updateGlow(player, entity, prev, curr);
	}

	protected GlowData getGlowData(@Nullable UUID player, @NotNull UUID entity) {
		synchronized (this) {
			GlowDataMap map = glows.get(entity);
			GlowData data = map == null ? null : map.get();

			GlowDataMap targetedMap = perPlayerGlows.get(Pair.of(player, entity));
			if (targetedMap == null) return data;

			GlowData targetedData = targetedMap.get();
			if (data == null) return targetedData;

			return data.compareTo(targetedData) < 0 ? data : targetedData;
		}
	}

	protected GlowData getGlowData(@NotNull Pair<@NotNull UUID, @NotNull UUID> pair) {
		synchronized (this) {
			GlowDataMap map = glows.get(pair.right());
			GlowData data = map == null ? null : map.get();

			GlowDataMap targetedMap = perPlayerGlows.get(pair);
			if (targetedMap == null) return data;

			GlowData targetedData = targetedMap.get();
			if (data == null) return targetedData;

			return data.compareTo(targetedData) < 0 ? data : targetedData;
		}
	}

	protected abstract void updateGlow(@Nullable Player player, @NotNull Entity entity, @Nullable GlowData prev, @NotNull GlowData curr);

	protected abstract void resetGlow(@Nullable Player player, @NotNull Entity entity, @NotNull GlowData data, boolean checkPerPlayer);

	protected abstract void sendTeamPackets(Player player);

	protected abstract void cancelTask(int taskId);

	protected abstract int scheduleDelayedTask(Runnable runnable, long delay);

	protected static final class GlowData implements Comparable<GlowData> {

		private final NamedTextColor color;
		private final int priority;
		private final int sequence;
		private int taskId;

		public GlowData(int priority, int sequence, NamedTextColor color) {
			this.priority = priority;
			this.sequence = sequence;
			this.color = color;

			taskId = -1;
		}

		public NamedTextColor color() {
			return color;
		}

		public int priority() {
			return priority;
		}

		public int sequence() {
			return sequence;
		}

		public int taskId() {
			return taskId;
		}

		public void taskId(int taskId) {
			this.taskId = taskId;
		}

		@Override
		public int compareTo(@NotNull GlowData o) {
			int compare = o.priority - priority;
			return compare == 0 ? o.sequence - sequence : compare;
		}

	}

	protected static final class GlowDataMap {

		private final Map<NamespacedKey, GlowData> keyedGlowData = new Object2ObjectOpenHashMap<>();
		private final PriorityQueue<GlowData> glowData = new PriorityQueue<>();

		public GlowDataMap() {

		}

		public GlowData put(NamespacedKey key, GlowData data) {
			GlowData old = keyedGlowData.put(key, data);
			if (old != null) glowData.remove(old);

			glowData.add(data);

			return old;
		}

		public void remove(NamespacedKey key) {
			GlowData old = keyedGlowData.remove(key);
			if (old == null) return;

			glowData.remove(old);
		}

		public GlowData get() {
			return glowData.peek();
		}

		public boolean isEmpty() {
			return keyedGlowData.isEmpty();
		}

	}

}
