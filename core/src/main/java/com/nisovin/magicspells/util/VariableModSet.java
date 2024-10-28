package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.managers.VariableManager;
import com.nisovin.magicspells.util.VariableMod.VariableOwner;

public class VariableModSet {

	private final List<VariableModData> modifiers;
	private final String path;

	private VariableModSet(@NotNull List<VariableModData> modifiers, @NotNull String path) {
		this.modifiers = modifiers;
		this.path = path;
	}

	public static VariableModSet fromConfig(@NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.VARIABLE_MODIFIERS, "Processing variable modifier set '%s'.", MagicDebug.resolveShortPath(path))
			.pushPaths(path, DebugPath.Type.LIST)
		) {
			List<String> modifierStrings = config.getStringList(path);
			if (modifierStrings.isEmpty()) {
				MagicDebug.info("No variable modifiers found.");
				return new VariableModSet(Collections.emptyList(), path);
			}

			List<VariableModData> modifiers = new ArrayList<>();
			for (int i = 0; i < modifierStrings.size(); i++) {
				String modifierString = modifierStrings.get(i);

				try (var ignored1 = MagicDebug.section("Processing variable modifier '%s'.", modifierString)
					.pushPath(String.valueOf(i), DebugPath.Type.LIST_ENTRY)
				) {
					try {
						String[] variableAndModifier = modifierString.split(" ", 2);

						String variable = variableAndModifier[0];
						VariableOwner owner = null;

						String[] ownerAndVariable = variable.split(":", 2);
						if (ownerAndVariable.length > 1) {
							try {
								owner = VariableOwner.valueOf(ownerAndVariable[0].toUpperCase());
								variable = ownerAndVariable[1];
							} catch (IllegalArgumentException e) {
								MagicDebug.warn("Invalid variable owner '%s' %s.", ownerAndVariable[0], MagicDebug.resolveFullPath());
								continue;
							}
						}

						modifiers.add(new VariableModData(variable, owner, new VariableMod(variableAndModifier[1]), modifierString, i));
					} catch (Exception e) {
						MagicDebug.error("Invalid variable modifier '%s' %s.", modifierString, MagicDebug.resolveFullPath(path));
					}
				}
			}

			return new VariableModSet(modifiers, path);
		}
	}

	public void process(@NotNull SpellData data) {
		process(VariableOwner.CASTER, data);
	}

	public void process(@NotNull VariableOwner owner, @NotNull SpellData data) {
		try (var ignored = MagicDebug.section(DebugCategory.VARIABLE_MODIFIERS, "Processing variable modifier set '%s'.", MagicDebug.resolveShortPath(path))
			.pushPaths(path, DebugPath.Type.LIST)
		) {
			VariableManager variableManager = MagicSpells.getVariableManager();

			for (VariableModData modifier : modifiers) {
				try (var ignored1 = MagicDebug.section("Processing variable modifier '%s'.", modifier.text)
					.pushPath(String.valueOf(modifier.ordinal), DebugPath.Type.LIST_ENTRY)
				) {
					VariableOwner variableOwner = modifier.owner == null ? owner : modifier.owner;
					Player playerToMod = switch (variableOwner) {
						case CASTER -> data.caster() instanceof Player player ? player : null;
						case TARGET -> data.target() instanceof Player player ? player : null;
					};

					if (playerToMod == null) {
						MagicDebug.info("Variable owner '%s' is not a player - skipping.", variableOwner);
						continue;
					}

					variableManager.processVariableMods(modifier.variable, modifier.variableMod, playerToMod, data);
				}
			}
		}
	}

	public boolean isEmpty() {
		return modifiers.isEmpty();
	}

	private record VariableModData(String variable, VariableOwner owner, VariableMod variableMod, String text, int ordinal) {

	}

}
