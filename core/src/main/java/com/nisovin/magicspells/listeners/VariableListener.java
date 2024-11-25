package com.nisovin.magicspells.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.VariableModSet;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.managers.VariableManager;
import com.nisovin.magicspells.util.VariableMod.VariableOwner;

public class VariableListener implements Listener {

	private final VariableManager variableManager;

	public VariableListener() {
		variableManager = MagicSpells.getVariableManager();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		try (var ignored = MagicDebug.section(DebugCategory.VARIABLES, "Loading variables on login for player '%s'.", player.getName())) {
			variableManager.loadPlayerVariables(player.getName(), Util.getUniqueId(player));
			variableManager.loadBossBars(player);

			MagicSpells.scheduleDelayedTask(() -> variableManager.loadExpBar(player), 10);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		String name = player.getName();
		if (!variableManager.getDirtyPlayerVariables().contains(name)) return;

		try (var ignored = MagicDebug.section(DebugCategory.VARIABLES, "Saving variables on quit for player '%s'.", name)) {
			variableManager.savePlayerVariables(name, Util.getUniqueId(player));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void variableModsCast(SpellCastEvent event) {
		Spell.SpellCastState state = event.getSpellCastState();
		if (state != Spell.SpellCastState.NORMAL) {
			MagicDebug.info("Spell cast state is %s - variable modifiers skipped.", state);
			return;
		}

		VariableModSet varMods = event.getSpell().getVariableModsCast();
		if (varMods == null || varMods.isEmpty()) return;

		varMods.process(event.getSpellData());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void variableModsCasted(SpellCastedEvent event) {
		Spell.SpellCastState state = event.getSpellCastState();
		if (state != Spell.SpellCastState.NORMAL) {
			MagicDebug.info("Spell cast state is %s - variable modifiers skipped.", state);
			return;
		}

		Spell.PostCastAction action = event.getPostCastAction();
		if (action == Spell.PostCastAction.ALREADY_HANDLED) {
			MagicDebug.info("Post cast action is 'ALREADY_HANDLED' - variable modifiers skipped.");
			return;
		}

		VariableModSet varMods = event.getSpell().getVariableModsCasted();
		if (varMods == null || varMods.isEmpty()) return;

		varMods.process(event.getSpellData());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void variableModsTarget(SpellTargetEvent event) {
		VariableModSet varMods = event.getSpell().getVariableModsTarget();
		if (varMods == null || varMods.isEmpty()) return;

		varMods.process(VariableOwner.TARGET, event.getSpellData());
	}

}
