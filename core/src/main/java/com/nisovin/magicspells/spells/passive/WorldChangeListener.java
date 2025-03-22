package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("worldchange")
public class WorldChangeListener extends PassiveListener {

	private final Set<String> worldNames = new HashSet<>();

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		for (String worldName : var.split(",")) {
			World world = Bukkit.getWorld(worldName);
			if (world == null) {
				MagicDebug.warn("Invalid world '%s' %s.", worldName, MagicDebug.resolveFullPath());
				continue;
			}

			worldNames.add(worldName);
		}
	}

	@OverridePriority
	@EventHandler
	public void onWorldChange(PlayerTeleportEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		World worldFrom = event.getFrom().getWorld();
		if (worldFrom == null) return;

		World worldTo = event.getTo().getWorld();
		if (worldTo == null || worldFrom.equals(worldTo)) return;

		if (!worldNames.isEmpty() && !worldNames.contains(worldTo.getName())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(event.getPlayer());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
