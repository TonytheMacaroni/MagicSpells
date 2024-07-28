package com.nisovin.magicspells.listeners;

import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.Multimap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.managers.VariableManager;

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

		try (var ignored = MagicDebug.section(DebugCategory.VARIABLES, "Saving variables on quit for player '%s'.", player.getName())) {
			if (!variableManager.getDirtyPlayerVariables().contains(player.getName())) {
				MagicDebug.info("Variables not marked dirty - skipping save.");
				return;
			}

			variableManager.savePlayerVariables(player.getName(), Util.getUniqueId(player));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void variableModsCast(SpellCastEvent event) {
		Spell spell = event.getSpell();

		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.VARIABLES)
			.message("Processing 'variable-mods-cast' for spell '%s'.", spell)
			.path("variable-mods-cast", DebugPath.Type.LIST)
		)) {
			if (event.isCancelled()) {
				MagicDebug.info("Spell cast was cancelled - variable modifiers skipped.");
				return;
			}

			Spell.SpellCastState state = event.getSpellCastState();
			if (state != Spell.SpellCastState.NORMAL) {
				MagicDebug.info("Spell cast state is %s - variable modifiers skipped.", state);
				return;
			}

			Multimap<String, VariableMod> varMods = event.getSpell().getVariableModsCast();
			if (varMods == null || varMods.isEmpty()) {
				MagicDebug.info("No variable modifiers found.");
				return;
			}

			if (!(event.getCaster() instanceof Player caster)) {
				MagicDebug.info("Caster is not a player - variable modifiers skipped.");
				return;
			}

			int i = 0;
			for (Map.Entry<String, VariableMod> entry : varMods.entries()) {
				String variable = entry.getKey();
				VariableMod mod = entry.getValue();

				try (var ignored1 = MagicDebug.section("Applying variable modifier '%s' to variable '%s'.", mod.getValue(), variable)
					.pushPath(String.valueOf(++i), DebugPath.Type.LIST_ENTRY)
				) {
					MagicDebug.info("Old value: %s.", (Supplier<String>) () -> variableManager.getStringValue(variable, caster));

					String newValue = variableManager.processVariableMods(variable, mod, caster, event.getSpellData());
					MagicDebug.info("New value: %s.", newValue);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void variableModsCasted(SpellCastedEvent event) {
		Spell spell = event.getSpell();

		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.VARIABLES)
			.message("Processing 'variable-mods-casted' for spell '%s'.", spell)
			.path("variable-mods-casted", DebugPath.Type.LIST)
		)) {
			Spell.SpellCastState state = event.getSpellCastState();
			if (state != Spell.SpellCastState.NORMAL) {
				MagicDebug.info("Spell cast state is %s - variable modifiers skipped.", state);
				return;
			}

			Spell.PostCastAction action = event.getPostCastAction();
			if (action == Spell.PostCastAction.ALREADY_HANDLED) {
				MagicDebug.info("Post cast action is %s - variable modifiers skipped.", action);
				return;
			}

			Multimap<String, VariableMod> varMods = event.getSpell().getVariableModsCasted();
			if (varMods == null || varMods.isEmpty()) {
				MagicDebug.info("No variable modifiers found.");
				return;
			}

			if (!(event.getCaster() instanceof Player caster)) {
				MagicDebug.info("Caster is not a player - variable modifiers skipped.");
				return;
			}

			int i = 0;
			for (Map.Entry<String, VariableMod> entry : varMods.entries()) {
				String variable = entry.getKey();
				VariableMod mod = entry.getValue();

				try (var ignored1 = MagicDebug.section("Applying variable modifier '%s' to variable '%s'.", mod.getValue(), variable)
					.pushPath(String.valueOf(++i), DebugPath.Type.LIST_ENTRY)
				) {
					MagicDebug.info("Old value: %s.", (Supplier<String>) () -> variableManager.getStringValue(variable, caster));

					String newValue = variableManager.processVariableMods(variable, mod, caster, event.getSpellData());
					MagicDebug.info("New value: %s.", newValue);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void variableModsTarget(SpellTargetEvent event) {
		Spell spell = event.getSpell();

		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.VARIABLES)
			.message("Processing 'variable-mods-target' for spell '%s'.", spell)
			.path("variable-mods-target", DebugPath.Type.LIST)
		)) {
			if (event.isCancelled()) {
				MagicDebug.info("Spell targeting was cancelled - variable modifiers skipped.");
				return;
			}

			Multimap<String, VariableMod> varMods = event.getSpell().getVariableModsTarget();
			if (varMods == null || varMods.isEmpty()) {
				MagicDebug.info("No variable modifiers found.");
				return;
			}

			if (!(event.getCaster() instanceof Player caster)) {
				MagicDebug.info("Caster is not a player - variable modifiers skipped.");
				return;
			}

			if (!(event.getTarget() instanceof Player target)) {
				MagicDebug.info("Target is not a player - variable modifiers skipped.");
				return;
			}

			int i = 0;
			for (Map.Entry<String, VariableMod> entry : varMods.entries()) {
				String variable = entry.getKey();
				VariableMod mod = entry.getValue();

				try (var ignored1 = MagicDebug.section("Applying variable modifier '%s' to variable '%s'.", mod.getValue(), variable)
					.pushPath(String.valueOf(++i), DebugPath.Type.LIST_ENTRY)
				) {
					Player playerToMod = target;

					String[] splits = variable.split(":");
					if (splits.length > 1) {
						if (splits[0].equals("caster")) playerToMod = caster;
						else if (!splits[0].equals("target")) {
							MagicDebug.warn("Invalid variable modifier target '%s' %s.", splits[0], MagicDebug.resolveFullPath());
							continue;
						}

						variable = splits[1];
					}

					String variableName = variable;
					MagicDebug.info("Old value: %s.", (Supplier<String>) () -> variableManager.getStringValue(variableName, caster));

					String newValue = variableManager.processVariableMods(variableName, mod, playerToMod, event.getSpellData());
					MagicDebug.info("New value: %s.", newValue);
				}
			}
		}
	}

}
