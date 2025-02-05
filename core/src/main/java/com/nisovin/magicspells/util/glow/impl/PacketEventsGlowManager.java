package com.nisovin.magicspells.util.glow.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

import com.google.common.collect.Iterables;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.configuration.ConfigurationSection;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.glow.PacketBasedGlowManager;

public class PacketEventsGlowManager extends PacketBasedGlowManager {

	private final List<WrapperPlayServerTeams> teamPackets;
	private final GlowPacketListener listener;

	public PacketEventsGlowManager() {
		ConfigurationSection config = MagicSpells.getInstance().getMagicConfig().getMainConfig();

		boolean seeFriendlyInvisibles = config.getBoolean("general.glow-spell-scoreboard-teams.see-friendly-invisibles", false);
		OptionData optionData = seeFriendlyInvisibles ? OptionData.ALL : OptionData.FRIENDLY_FIRE;

		NameTagVisibility visibility = getStringOption("name-tag-visibility", NameTagVisibility.ALWAYS, NameTagVisibility::fromID, config, MagicSpells::error);
		CollisionRule collisionRule = getStringOption("collision-rule", CollisionRule.ALWAYS, CollisionRule::fromID, config, MagicSpells::error);

		teamPackets = NamedTextColor.NAMES.values()
			.stream()
			.map(color -> {
				String name = "magicspells:" + color;

				return new WrapperPlayServerTeams(
					name,
					TeamMode.CREATE,
					new WrapperPlayServerTeams.ScoreBoardTeamInfo(
						Component.text(name),
						null,
						null,
						visibility,
						collisionRule,
						color,
						optionData
					)
				);
			})
			.toList();

		listener = new GlowPacketListener();

		MagicSpells.registerEvents(this);
	}

	@Override
	public void load() {
		super.load();

		PacketEvents.getAPI().getEventManager().registerListener(listener);
	}

	@Override
	public void unload() {
		PacketEvents.getAPI().getEventManager().unregisterListener(listener);

		synchronized (this) {
			super.unload();

			scoreboardNames.clear();
		}
	}

	@Override
	protected void updateGlow(@Nullable Player player, @NotNull Entity entity, @Nullable GlowData prev, @NotNull GlowData curr) {
		if (prev != null && prev.color() == curr.color()) return;

		curr.lastEntryName(getScoreboardEntryName(player, entity));

		WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(
			entity.getEntityId(),
			List.of(new EntityData(
				0,
				EntityDataTypes.BYTE,
				(byte) (MagicSpells.getVolatileCodeHandler().getEntityMetadata(entity) | 0x40)
			))
		);

		Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

		WrapperPlayServerTeams teamPacket = null;
		boolean reset = false;
		if (curr.color() != null) {
			teamPacket = new WrapperPlayServerTeams(
				"magicspells:" + curr.color(),
				TeamMode.ADD_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				curr.lastEntryName()
			);

			scoreboardNames.put(curr.lastEntryName(), entity.getUniqueId());
		} else if (prev != null) {
			Scoreboard scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = resetTeamPacket(scoreboard, prev);

			scoreboardNames.put(curr.lastEntryName(), entity.getUniqueId());
			reset = true;
		}

		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();

		if (player != null) {
			if (teamPacket != null) manager.sendPacketSilently(player, teamPacket);
			manager.sendPacket(player, metadataPacket);

			return;
		}

		Iterable<Player> trackedPlayers = entity.getTrackedBy();
		if (entity instanceof Player p)
			trackedPlayers = Iterables.concat(trackedPlayers, Collections.singleton(p));

		Pair<UUID, UUID> pair = ObjectObjectMutablePair.of(null, entity.getUniqueId());
		for (Player viewer : trackedPlayers) {
			GlowDataMap targetedMap = perPlayerGlows.get(pair.left(viewer.getUniqueId()));
			if (targetedMap != null && targetedMap.get().compareTo(curr) < 0) continue;

			if (reset) {
				Scoreboard scoreboard = viewer.getScoreboard();
				if (scoreboard != mainScoreboard) {
					manager.sendPacketSilently(viewer, resetTeamPacket(scoreboard, prev));
					manager.sendPacket(viewer, metadataPacket);

					continue;
				}
			}

			if (teamPacket != null) manager.sendPacketSilently(viewer, teamPacket);
			manager.sendPacket(viewer, metadataPacket);
		}
	}

	@Override
	protected void resetGlow(@Nullable Player player, @NotNull Entity entity, @NotNull GlowData data, boolean checkPerPlayer) {
		WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(
			entity.getEntityId(),
			List.of(new EntityData(
				0,
				EntityDataTypes.BYTE,
				MagicSpells.getVolatileCodeHandler().getEntityMetadata(entity)
			))
		);

		Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

		WrapperPlayServerTeams teamPacket = null;
		if (data.color() != null) {
			Scoreboard scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = resetTeamPacket(scoreboard, data);
		}

		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();

		if (player != null) {
			if (teamPacket != null) manager.sendPacketSilently(player, teamPacket);
			manager.sendPacket(player, metadataPacket);

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

			Scoreboard scoreboard = viewer.getScoreboard();
			if (scoreboard != mainScoreboard) {
				manager.sendPacketSilently(viewer, resetTeamPacket(scoreboard, data));
				manager.sendPacket(viewer, metadataPacket);

				continue;
			}

			if (teamPacket != null) manager.sendPacketSilently(viewer, teamPacket);
			manager.sendPacket(viewer, metadataPacket);
		}
	}

	@Override
	protected void sendTeamPackets(Player player) {
		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();
		for (WrapperPlayServerTeams packet : teamPackets) manager.sendPacket(player, packet);
	}

	@Override
	protected void removeTeamPackets() {
		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();

		List<WrapperPlayServerTeams> packets = NamedTextColor.NAMES.values()
			.stream()
			.map(color -> {
				String name = "magicspells:" + color;
				return new WrapperPlayServerTeams(name, TeamMode.REMOVE, (WrapperPlayServerTeams.ScoreBoardTeamInfo) null);
			})
			.toList();

		for (Player player : Bukkit.getOnlinePlayers())
			for (WrapperPlayServerTeams packet : packets)
				manager.sendPacket(player, packet);
	}

	@Override
	protected void cancelTask(int taskId) {
		MagicSpells.cancelTask(taskId);
	}

	@Override
	protected int scheduleDelayedTask(Runnable runnable, long delay) {
		return MagicSpells.scheduleDelayedTask(runnable, delay);
	}

	private WrapperPlayServerTeams resetTeamPacket(@NotNull Scoreboard scoreboard, @NotNull GlowData data) {
		Team team = scoreboard.getEntryTeam(data.lastEntryName());
		if (team != null) {
			return new WrapperPlayServerTeams(
				team.getName(),
				TeamMode.ADD_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				data.lastEntryName()
			);
		}

		return new WrapperPlayServerTeams(
			"magicspells:" + data.color(),
			TeamMode.REMOVE_ENTITIES,
			(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
			data.lastEntryName()
		);
	}

	private final class GlowPacketListener extends PacketListenerAbstract {

		public GlowPacketListener() {
			super(PacketListenerPriority.LOWEST);
		}

		@Override
		public void onPacketSend(PacketSendEvent event) {
			try {
				if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) handleEntityData(event);
				else if (event.getPacketType() == PacketType.Play.Server.TEAMS) handleTeamPacket(event);
			} catch (Throwable throwable) {
				MagicSpells.error("Encountered an error while intercepting a packet - cancelling packet send.");
				throwable.printStackTrace();

				event.setCancelled(true);
			}
		}

		private void handleEntityData(PacketSendEvent event) {
			if (glows.isEmpty() && perPlayerGlows.isEmpty()) return;

			WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);

			List<EntityData> metadata = packet.getEntityMetadata();
			if (metadata.isEmpty()) return;

			EntityData entityData = metadata.getFirst();
			if (entityData.getIndex() != 0) return;

			byte flags = (byte) entityData.getValue();
			if ((flags & 0x40) > 0) return;

			Player player = event.getPlayer();

			Entity entity = MagicSpells.getVolatileCodeHandler().getEntityFromId(player.getWorld(), packet.getEntityId());
			if (entity == null) return;

			GlowData data = getGlowData(player.getUniqueId(), entity.getUniqueId());
			if (data == null) return;

			flags |= 0x40;
			entityData.setValue(flags);
		}

		private void handleTeamPacket(PacketSendEvent event) {
			if (glows.isEmpty() && perPlayerGlows.isEmpty()) return;

			WrapperPlayServerTeams packet = new WrapperPlayServerTeams(event);

			TeamMode mode = packet.getTeamMode();
			if (mode != TeamMode.REMOVE_ENTITIES && mode != TeamMode.ADD_ENTITIES) return;

			Collection<String> entries = packet.getPlayers();
			if (entries.isEmpty()) return;

			UUID playerUUID = ((Player) event.getPlayer()).getUniqueId();

			Iterator<String> iterator = entries.iterator();
			if (entries.size() == 1) {
				UUID entityUUID = scoreboardNames.get(iterator.next());
				if (entityUUID == null) return;

				GlowData data = getGlowData(playerUUID, entityUUID);
				if (data != null && data.color() != null) {
					event.setCancelled(true);
					return;
				}

				return;
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

			if (!modified) return;

			if (filtered.isEmpty()) {
				event.setCancelled(true);
				return;
			}

			packet.setPlayers(filtered);
		}

	}

}
