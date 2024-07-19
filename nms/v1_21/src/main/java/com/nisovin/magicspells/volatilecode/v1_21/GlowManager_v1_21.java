package com.nisovin.magicspells.volatilecode.v1_21;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

import com.google.common.collect.MapMaker;

import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;

import com.nisovin.magicspells.util.glow.PacketBasedGlowManager;
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper;

public class GlowManager_v1_21 extends PacketBasedGlowManager implements Listener {

	private final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
	private final List<Packet<?>> TEAM_PACKETS = new ArrayList<>();
	private final MethodHandle TEAM_PACKET_HANDLE;
	private final VolatileCodeHelper helper;

	private final ConcurrentHashMap<String, UUID> scoreboardNames = new ConcurrentHashMap<>();
	private final Set<Packet<?>> handled = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

	public GlowManager_v1_21(VolatileCodeHelper helper) {
		this.helper = helper;

		Scoreboard scoreboard = new Scoreboard();
		for (ChatFormatting formatting : ChatFormatting.values()) {
			if (!formatting.isColor()) continue;

			PlayerTeam team = new PlayerTeam(scoreboard, "magicspells:" + formatting.getName());
			team.setColor(formatting);

			TEAM_PACKETS.add(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
		}

		MethodHandle handle;
		try {
			handle = MethodHandles
				.privateLookupIn(ClientboundSetPlayerTeamPacket.class, MethodHandles.lookup())
				.findConstructor(
					ClientboundSetPlayerTeamPacket.class,
					MethodType.methodType(void.class, String.class, int.class, Optional.class, Collection.class)
				);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			helper.error("Encountered an error while initializing GlowManager_v1_21");
			e.printStackTrace();

			handle = null;
		}

		TEAM_PACKET_HANDLE = handle;
	}

	@Override
	public void load() {
		super.load();

		helper.registerEvents(this);

		Bukkit.getOnlinePlayers().forEach(this::addGlowChannelHandler);
	}

	@Override
	public void unload() {
		Bukkit.getOnlinePlayers().forEach(this::removeGlowChannelHandler);

		synchronized (this) {
			super.unload();

			handled.clear();
			scoreboardNames.clear();
		}
	}

	@Override
	protected void updateGlow(@Nullable Player player, @NotNull Entity entity, @Nullable GlowData prev, @NotNull GlowData curr) {
		if (prev != null && prev.color() == curr.color()) return;

		net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();

		ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(
			entity.getEntityId(),
			list(new SynchedEntityData.DataValue<>(
				0,
				EntityDataSerializers.BYTE,
				(byte) (handle.getEntityData().get(DATA_SHARED_FLAGS_ID) | 0x40)
			))
		);

		handled.add(entityDataPacket);

		var mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

		ClientboundSetPlayerTeamPacket teamPacket = null;
		boolean reset = false;
		if (curr.color() != null) {
			scoreboardNames.put(handle.getScoreboardName(), handle.getUUID());

			try {
				teamPacket = (ClientboundSetPlayerTeamPacket) TEAM_PACKET_HANDLE.invoke(
					"magicspells:" + curr.color(),
					3,
					Optional.empty(),
					list(handle.getScoreboardName())
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}

			handled.add(teamPacket);
		} else if (prev != null) {
			var scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = resetTeamPacket(entity, scoreboard, prev);
			reset = true;
		}

		if (player != null) {
			ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

			if (teamPacket != null) connection.send(teamPacket);
			connection.send(entityDataPacket);

			return;
		}

		Pair<UUID, UUID> pair = ObjectObjectMutablePair.of(null, entity.getUniqueId());
		for (Player viewer : entity.getTrackedBy()) {
			GlowDataMap targetedMap = perPlayerGlows.get(pair.left(viewer.getUniqueId()));
			if (targetedMap != null && targetedMap.get().compareTo(curr) < 0) continue;

			ServerGamePacketListenerImpl connection = ((CraftPlayer) viewer).getHandle().connection;

			if (reset) {
				var scoreboard = viewer.getScoreboard();

				if (scoreboard != mainScoreboard) {
					connection.send(resetTeamPacket(entity, scoreboard, prev));
					connection.send(entityDataPacket);

					continue;
				}
			}

			if (teamPacket != null) connection.send(teamPacket);
			connection.send(entityDataPacket);
		}
	}

	@Override
	protected void resetGlow(@Nullable Player player, @NotNull Entity entity, @NotNull GlowData data, boolean checkPerPlayer) {
		net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();

		ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(
			entity.getEntityId(),
			list(new SynchedEntityData.DataValue<>(
				0,
				EntityDataSerializers.BYTE,
				handle.getEntityData().get(DATA_SHARED_FLAGS_ID)
			))
		);

		handled.add(entityDataPacket);

		var mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

		ClientboundSetPlayerTeamPacket teamPacket = null;
		if (data.color() == null) {
			var scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = resetTeamPacket(entity, scoreboard, data);
		}

		if (player != null) {
			ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

			if (teamPacket != null) connection.send(teamPacket);
			connection.send(entityDataPacket);

			return;
		}

		Pair<UUID, UUID> pair = checkPerPlayer ? ObjectObjectMutablePair.of(null, entity.getUniqueId()) : null;
		for (Player viewer : entity.getTrackedBy()) {
			if (checkPerPlayer) {
				GlowDataMap targetedMap = perPlayerGlows.get(pair.left(viewer.getUniqueId()));

				if (targetedMap != null) {
					GlowData curr = targetedMap.get();

					if (curr != null) {
						updateGlow(viewer, entity, data, curr);
						continue;
					}
				}
			}

			ServerGamePacketListenerImpl connection = ((CraftPlayer) viewer).getHandle().connection;

			var scoreboard = viewer.getScoreboard();
			if (scoreboard != mainScoreboard) {
				connection.send(resetTeamPacket(entity, scoreboard, data));
				connection.send(entityDataPacket);

				continue;
			}

			if (teamPacket != null) connection.send(teamPacket);
			connection.send(entityDataPacket);
		}
	}

	@Override
	protected void sendTeamPackets(Player player) {
		ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
		for (Packet<?> packet : TEAM_PACKETS) connection.send(packet);
	}

	@Override
	protected void cancelTask(int taskId) {
		helper.cancelTask(taskId);
	}

	@Override
	protected int scheduleDelayedTask(Runnable runnable, long delay) {
		return helper.scheduleDelayedTask(runnable, delay);
	}

	private ClientboundSetPlayerTeamPacket resetTeamPacket(@NotNull Entity entity, @NotNull org.bukkit.scoreboard.Scoreboard scoreboard, @NotNull GlowData data) {
		try {
			Team team = scoreboard.getEntityTeam(entity);

			if (team != null) {
				return (ClientboundSetPlayerTeamPacket) TEAM_PACKET_HANDLE.invoke(
					team.getName(),
					3,
					Optional.empty(),
					list(entity.getScoreboardEntryName())
				);
			}

			return (ClientboundSetPlayerTeamPacket) TEAM_PACKET_HANDLE.invoke(
				"magicspells:" + data.color(),
				4,
				Optional.empty(),
				list(entity.getScoreboardEntryName())
			);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private void addGlowChannelHandler(Player player) {
		ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();

		pipeline.addBefore("unbundler", "magicspells:glow_channel_handler", new GlowChannelHandler(player));
	}

	private void removeGlowChannelHandler(Player player) {
		ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();

		if (pipeline.get("magicspells:glow_channel_handler") != null)
			pipeline.remove("magicspells:glow_channel_handler");
	}

	private <T> List<T> list(T element) {
		List<T> list = new ArrayList<>(1);
		list.add(element);

		return list;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		sendTeamPackets(event.getPlayer());
		addGlowChannelHandler(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		removeGlowChannelHandler(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTrackEntity(PlayerTrackEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getEntity();

		scheduleDelayedTask(() -> {
			GlowData data = getGlowData(player.getUniqueId(), entity.getUniqueId());
			if (data == null) return;

			updateGlow(player, entity, null, data);
		}, 0);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTrackEntity(PlayerUntrackEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getEntity();
		Pair<UUID, UUID> pair = Pair.of(player.getUniqueId(), entity.getUniqueId());

		synchronized (this) {
			GlowData data = getGlowData(pair);
			if (data == null) return;

			perPlayerGlows.remove(pair);
			resetGlow(player, entity, data, false);
		}
	}

	private class GlowChannelHandler extends ChannelOutboundHandlerAdapter {

		private final Player player;

		private GlowChannelHandler(Player player) {
			this.player = player;
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			switch (msg) {
				case ClientboundSetEntityDataPacket packet -> handleEntityData(packet);
				case ClientboundSetPlayerTeamPacket packet -> {
					msg = handleTeamPacket(packet);
					if (msg == null) return;
				}
				default -> {}
			}

			super.write(ctx, msg, promise);
		}

		private void handleEntityData(ClientboundSetEntityDataPacket packet) {
			if (glows.isEmpty() && perPlayerGlows.isEmpty()) return;
			if (handled.remove(packet)) return;

			List<SynchedEntityData.DataValue<?>> packedItems = packet.packedItems();
			if (packedItems.isEmpty()) return;

			SynchedEntityData.DataValue<?> item = packedItems.getFirst();
			if (item.id() != 0) return;

			byte flags = (byte) item.value();
			if ((flags & 0x40) > 0) return;

			net.minecraft.world.entity.Entity entity = ((CraftPlayer) player).getHandle().level().moonrise$getEntityLookup().get(packet.id());
			if (entity == null) return;

			GlowData data = getGlowData(player.getUniqueId(), entity.getUUID());
			if (data == null) return;

			flags |= 0x40;
			packedItems.set(0, new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, flags));
		}

		private ClientboundSetPlayerTeamPacket handleTeamPacket(ClientboundSetPlayerTeamPacket packet) {
			if (glows.isEmpty() && perPlayerGlows.isEmpty()) return packet;
			if (handled.remove(packet)) return packet;

			ClientboundSetPlayerTeamPacket.Action playerAction = packet.getPlayerAction();
			if (playerAction == null || packet.getTeamAction() != null) return packet;

			UUID playerUUID = player.getUniqueId();

			Collection<String> entries = packet.getPlayers();
			if (entries.isEmpty()) return packet;

			Iterator<String> iterator = entries.iterator();
			if (entries.size() == 1) {
				UUID entityUUID = scoreboardNames.get(iterator.next());
				if (entityUUID == null) return packet;

				GlowData data = getGlowData(playerUUID, entityUUID);
				if (data != null && data.color() != null) return null;

				return packet;
			}

			List<String> filtered = new ArrayList<>(entries.size());
			boolean modified = false;

			while (iterator.hasNext()) {
				String entry = iterator.next();

				UUID entityUUID = scoreboardNames.get(entry);
				if (entityUUID == null) {
					filtered.add(entry);
					continue;
				}

				GlowData data = getGlowData(playerUUID, entityUUID);
				if (data == null || data.color() == null) {
					filtered.add(entry);
					continue;
				}

				modified = true;
			}

			if (!modified) return packet;
			if (filtered.isEmpty()) return null;

			try {
				return (ClientboundSetPlayerTeamPacket) TEAM_PACKET_HANDLE.invoke(
					packet.getName(),
					playerAction == ClientboundSetPlayerTeamPacket.Action.ADD ? 3 : 4,
					packet.getParameters(),
					filtered
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

	}

}
