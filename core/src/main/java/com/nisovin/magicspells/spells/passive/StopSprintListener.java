package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("stopsprint")
public class StopSprintListener extends PassiveListener {

	@Override
	public void initialize(@NotNull String var) {
	}

	@OverridePriority
	@EventHandler
	public void onSprint(PlayerToggleSprintEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;
		if (event.isSprinting()) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
