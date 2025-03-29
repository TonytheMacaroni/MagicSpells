package com.nisovin.magicspells.util.glow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.events.DisguiseEvent;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.events.UndisguiseEvent;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;

import com.nisovin.magicspells.util.glow.PacketBasedGlowManager.ScoreboardEntry;

public class LibsDisguiseHelper {

	public static ScoreboardEntry getDisguisedScoreboardEntry(@Nullable Player player, @NotNull Entity entity) {
		Disguise disguise = player == null ? DisguiseAPI.getDisguise(entity) : DisguiseAPI.getDisguise(player, entity);
		if (disguise == null) return new ScoreboardEntry(entity.getScoreboardEntryName());

		if (entity.equals(player) && !disguise.isSelfDisguiseVisible())
			return new ScoreboardEntry(entity.getScoreboardEntryName());

		return getDisguisedScoreboardEntry(entity, disguise);
	}

	public static ScoreboardEntry getDisguisedScoreboardEntry(@NotNull Entity entity, @NotNull Disguise disguise) {
		String entry = disguise instanceof PlayerDisguise pd ? pd.getProfileName() : disguise.getUUID().toString();
		return new ScoreboardEntry(entity.getScoreboardEntryName(), entry);
	}

	public static int getSelfDisguiseId() {
		return DisguiseAPI.getSelfDisguiseId();
	}

	public static Listener createLibDisguisesListener(PacketBasedGlowManager<?, ?, ?> glowManager) {
		return new Listener() {

			@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
			public void onDisguise(DisguiseEvent event) {
				Disguise disguise = event.getDisguise();
				if (!disguise.isSelfDisguiseVisible() || !(event.getDisguised() instanceof Player player))
					return;

				// Self-disguising is usually delayed by 2 ticks, at the time of writing. As such, we must delay refreshing
				// the scoreboard entry to avoid glow colors flickering. Adjust as necessary for disguise delay.
				glowManager.scheduleDelayedTask(() -> {
					if (disguise.isDisguiseInUse())
						glowManager.refreshScoreboardEntry(player, getDisguisedScoreboardEntry(player, disguise));
				}, 2);
			}

			@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
			public void onUndisguise(UndisguiseEvent event) {
				Disguise disguise = event.getDisguise();
				if (!disguise.isSelfDisguiseVisible() || !(event.getDisguised() instanceof Player player))
					return;

				glowManager.refreshScoreboardEntry(player, new ScoreboardEntry(player.getScoreboardEntryName()));
			}

		};
	}

}
