package com.nisovin.magicspells.util.glow.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Scoreboard;

import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.glow.PacketBasedGlowManager;

public class PacketEventsGlowManager extends PacketBasedGlowManager implements Listener {

	private final List<WrapperPlayServerTeams> TEAM_PACKETS = new ArrayList<>();

	public PacketEventsGlowManager() {
		for (NamedTextColor color : NamedTextColor.NAMES.values()) {
			String name = "magicspells:" + color;

			TEAM_PACKETS.add(new WrapperPlayServerTeams(
				name,
				WrapperPlayServerTeams.TeamMode.CREATE,
				new WrapperPlayServerTeams.ScoreBoardTeamInfo(
					Component.text(name),
					null,
					null,
					WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
					WrapperPlayServerTeams.CollisionRule.ALWAYS,
					color,
					// TODO: Make sure this is correct
					WrapperPlayServerTeams.OptionData.ALL
				)
			));
		}
	}

	@Override
	public void load() {
		super.load();

		MagicSpells.registerEvents(this);
	}

	@Override
	protected void updateGlow(@Nullable Player player, @NotNull Entity entity, @Nullable GlowData prev, @NotNull GlowData curr) {
		if (prev != null && prev.color() == curr.color()) return;

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
				WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				entity.getScoreboardEntryName()
			);
		} else if (prev != null) {
			Scoreboard scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = resetTeamPacket(entity, scoreboard, prev);
			reset = true;
		}

		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();

		if (player != null) {
			if (teamPacket != null) manager.sendPacket(player, teamPacket);
			manager.sendPacket(player, metadataPacket);

			return;
		}

		Pair<UUID, UUID> pair = ObjectObjectMutablePair.of(null, entity.getUniqueId());
		for (Player viewer : entity.getTrackedBy()) {
			GlowDataMap targetedMap = perPlayerGlows.get(pair.left(viewer.getUniqueId()));
			if (targetedMap != null && targetedMap.get().compareTo(curr) < 0) continue;

			if (reset) {
				Scoreboard scoreboard = viewer.getScoreboard();
				if (scoreboard != mainScoreboard) {
					manager.sendPacket(viewer, resetTeamPacket(entity, scoreboard, prev));
					manager.sendPacket(viewer, metadataPacket);

					continue;
				}
			}

			if (teamPacket != null) manager.sendPacket(viewer, teamPacket);
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
			teamPacket = resetTeamPacket(entity, scoreboard, data);
		}

		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();

		if (player != null) {
			if (teamPacket != null) manager.sendPacket(player, teamPacket);
			manager.sendPacket(player, metadataPacket);

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

			Scoreboard scoreboard = viewer.getScoreboard();
			if (scoreboard != mainScoreboard) {
				manager.sendPacket(viewer, resetTeamPacket(entity, scoreboard, data));
				manager.sendPacket(viewer, metadataPacket);

				continue;
			}

			if (teamPacket != null) manager.sendPacket(viewer, teamPacket);
			manager.sendPacket(viewer, metadataPacket);
		}
	}

	@Override
	protected void sendTeamPackets(Player player) {
		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();
		for (WrapperPlayServerTeams packet : TEAM_PACKETS) manager.sendPacket(player, packet);
	}

	@Override
	protected void cancelTask(int taskId) {
		MagicSpells.cancelTask(taskId);
	}

	@Override
	protected int scheduleDelayedTask(Runnable runnable, long delay) {
		return MagicSpells.scheduleDelayedTask(runnable, delay);
	}

	private WrapperPlayServerTeams resetTeamPacket(@NotNull Entity entity, @NotNull Scoreboard scoreboard, @NotNull GlowData data) {
		Team team = scoreboard.getEntityTeam(entity);
		if (team != null) {
			return new WrapperPlayServerTeams(
				team.getName(),
				WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				entity.getScoreboardEntryName()
			);
		}

		return new WrapperPlayServerTeams(
			"magicspells:" + data.color(),
			WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
			(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
			entity.getScoreboardEntryName()
		);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		sendTeamPackets(event.getPlayer());
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

}
