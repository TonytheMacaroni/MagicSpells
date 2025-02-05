package com.nisovin.magicspells.volatilecode.latest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Iterables;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.volatilecode.VolatileCodeHelper;
import com.nisovin.magicspells.util.glow.PacketBasedGlowManager;

public class VolatileGlowManagerLatest extends PacketBasedGlowManager {

	private static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);

	private final Set<Packet<?>> handled = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
	private final List<Packet<?>> teamPackets = new ArrayList<>();
	private final MethodHandle teamPacketHandle;
	private final VolatileCodeHelper helper;

	public VolatileGlowManagerLatest(VolatileCodeHelper helper) {
		this.helper = helper;

		ConfigurationSection config = helper.getMainConfig();

		boolean seeFriendlyInvisibles = config.getBoolean("glow-spell-scoreboard-teams.see-friendly-invisibles", false);
		CollisionRule collision = getStringOption("collision-rule", CollisionRule.ALWAYS, CollisionRule::byName, config, helper::error);
		Visibility visibility = getStringOption("name-tag-visibility", Visibility.ALWAYS, Visibility::byName, config, helper::error);

		Scoreboard scoreboard = new Scoreboard();
		for (ChatFormatting formatting : ChatFormatting.values()) {
			if (!formatting.isColor()) continue;

			PlayerTeam team = new PlayerTeam(scoreboard, "magicspells:" + formatting.getName());
			team.setSeeFriendlyInvisibles(seeFriendlyInvisibles);
			team.setNameTagVisibility(visibility);
			team.setCollisionRule(collision);
			team.setColor(formatting);

			teamPackets.add(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
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
		teamPacketHandle = handle;

		helper.registerEvents(this);
	}

	@Override
	public void load() {
		super.load();

		Bukkit.getOnlinePlayers().forEach(this::addGlowChannelHandler);
	}

	@Override
	public void unload() {
		Bukkit.getOnlinePlayers().forEach(this::removeGlowChannelHandler);

		synchronized (this) {
			super.unload();

			handled.clear();
		}
	}

	@Override
	protected void updateGlow(@Nullable Player player, @NotNull Entity entity, @Nullable GlowData prev, @NotNull GlowData curr) {
		if (prev != null && prev.color() == curr.color()) return;

		curr.lastEntryName(getScoreboardEntryName(player, entity));

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
			try {
				teamPacket = (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
					"magicspells:" + curr.color(),
					3,
					Optional.empty(),
					list(curr.lastEntryName())
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}

			scoreboardNames.put(curr.lastEntryName(), handle.getUUID());
			handled.add(teamPacket);
		} else if (prev != null) {
			var scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = resetTeamPacket(scoreboard, prev);

			scoreboardNames.put(curr.lastEntryName(), handle.getUUID());
			reset = true;
		}

		if (player != null) {
			ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

			if (teamPacket != null) connection.send(teamPacket);
			connection.send(entityDataPacket);

			return;
		}

		Iterable<Player> trackedPlayers = entity.getTrackedBy();
		if (entity instanceof Player p)
			trackedPlayers = Iterables.concat(trackedPlayers, Collections.singleton(p));

		Pair<UUID, UUID> pair = ObjectObjectMutablePair.of(null, entity.getUniqueId());
		for (Player viewer : trackedPlayers) {
			GlowDataMap targetedMap = perPlayerGlows.get(pair.left(viewer.getUniqueId()));
			if (targetedMap != null && targetedMap.get().compareTo(curr) < 0) continue;

			ServerGamePacketListenerImpl connection = ((CraftPlayer) viewer).getHandle().connection;

			if (reset) {
				var scoreboard = viewer.getScoreboard();

				if (scoreboard != mainScoreboard) {
					connection.send(resetTeamPacket(scoreboard, prev));
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
		if (data.color() != null) {
			var scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = resetTeamPacket(scoreboard, data);
		}

		if (player != null) {
			ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

			if (teamPacket != null) connection.send(teamPacket);
			connection.send(entityDataPacket);

			return;
		}

		Iterable<Player> trackedPlayers = entity.getTrackedBy();
		if (entity instanceof Player p)
			trackedPlayers = Iterables.concat(trackedPlayers, Collections.singleton(p));

		Pair<UUID, UUID> pair = checkPerPlayer ? ObjectObjectMutablePair.of(null, entity.getUniqueId()) : null;
		for (Player viewer : trackedPlayers) {
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
				connection.send(resetTeamPacket(scoreboard, data));
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
		for (Packet<?> packet : teamPackets) connection.send(packet);
	}

	@Override
	protected void removeTeamPackets() {
		List<ClientboundSetPlayerTeamPacket> packets = new ArrayList<>();

		Scoreboard scoreboard = new Scoreboard();
		for (ChatFormatting formatting : ChatFormatting.values()) {
			if (!formatting.isColor()) continue;

			PlayerTeam team = new PlayerTeam(scoreboard, "magicspells:" + formatting.getName());
			team.setColor(formatting);

			packets.add(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
			for (ClientboundSetPlayerTeamPacket packet : packets) connection.send(packet);
		}
	}

	@Override
	protected void cancelTask(int taskId) {
		helper.cancelTask(taskId);
	}

	@Override
	protected int scheduleDelayedTask(Runnable runnable, long delay) {
		return helper.scheduleDelayedTask(runnable, delay);
	}

	private ClientboundSetPlayerTeamPacket resetTeamPacket(@NotNull org.bukkit.scoreboard.Scoreboard scoreboard, @NotNull GlowData data) {
		try {
			Team team = scoreboard.getEntryTeam(data.lastEntryName());

			if (team != null) {
				return (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
					team.getName(),
					3,
					Optional.empty(),
					list(data.lastEntryName())
				);
			}

			return (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
				"magicspells:" + data.color(),
				4,
				Optional.empty(),
				list(data.lastEntryName())
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

	// Maybe instead use ChannelInitializeListenerHolder somehow?

	@Override
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		super.onPlayerJoin(event);
		addGlowChannelHandler(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		removeGlowChannelHandler(event.getPlayer());
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

			Collection<String> entries = packet.getPlayers();
			if (entries.isEmpty()) return packet;

			UUID playerUUID = player.getUniqueId();

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
				return (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
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
