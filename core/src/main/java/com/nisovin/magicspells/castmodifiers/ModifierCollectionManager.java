package com.nisovin.magicspells.castmodifiers;

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.ModifierSet.ModifierData;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.MagicConfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModifierCollectionManager {

	private final Map<String, ModifierCollection> collections = new HashMap<>();

	private Queue<String> loading;

	public ModifierCollectionManager() {

	}

	public void loadCollections() {
		collections.clear();

		MagicConfig magicConfig = MagicSpells.getMagicConfig();

		ConfigurationSection modifierCollections = magicConfig.getSection("general.modifiers");
		if (modifierCollections == null) {
			MagicDebug.info("No modifier collections found.");
			return;
		}

		Set<String> keys = modifierCollections.getKeys(false);
		if (keys.isEmpty()) {
			MagicDebug.info("No modifier collections found.");
			return;
		}

		loading = new ArrayDeque<>();

		for (String internalName : keys)
			if (!collections.containsKey(internalName))
				loadCollection(internalName);

		loading = null;
	}

	private ModifierCollection loadCollection(String internalName) {
		MagicConfig magicConfig = MagicSpells.getMagicConfig();
		String fileName = magicConfig.getFile(MagicConfig.Category.MODIFIERS, internalName);

		try (var ignored = MagicDebug.section(builder -> builder
			.category(DebugCategory.MODIFIERS)
			.message("Loading modifier collection '%s'.", internalName)
			.resetPath()
			.path(fileName, DebugPath.Type.FILE)
			.path("general", DebugPath.Type.SECTION, false)
			.path("modifiers", DebugPath.Type.SECTION)
			.path(internalName, DebugPath.Type.SECTION)
		)) {
			boolean circular = loading.contains(internalName);
			loading.add(internalName);

			if (circular) {
				String circularLoadOrder = loading.stream()
					.dropWhile(name -> !name.equals(internalName))
					.collect(Collectors.joining(" -> "));

				MagicDebug.warn("Could not load modifier collection %s,a due to circular loading: %s", MagicDebug.resolveFullPath(), circularLoadOrder);
				return null;
			}

			ConfigurationSection section = magicConfig.getSection("general.modifiers." + internalName);
			if (section == null) {
				MagicDebug.warn("Invalid modifier collection %s.", MagicDebug.resolvePath(internalName));
				return null;
			}

			String passConditionString = section.getString("pass-condition", "ALL");

			ModifierCollection.PassCondition passCondition;
			try {
				passCondition = ModifierCollection.PassCondition.valueOf(passConditionString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.error("Invalid modifier collection %s - invalid 'pass-condition' '%s'.", MagicDebug.resolvePath(internalName), passConditionString);
				return null;
			}

			List<String> modifierStrings = section.getStringList("checks");
			if (modifierStrings.isEmpty()) {
				MagicDebug.error("Invalid modifier collection %s - 'checks' is not defined.", MagicDebug.resolvePath(internalName));
				return null;
			}

			List<ModifierData> modifiers;
			try (var ignored1 = MagicDebug.section("Resolving 'checks'.")
				.pushPath("checks", DebugPath.Type.LIST)
			) {
				modifiers = ModifierSet.getModifierData(null, modifierStrings);
			}

			ModifierCollection collection = new ModifierCollection(internalName, modifiers, passCondition);
			collections.put(internalName, collection);

			return collection;
		} finally {
			loading.remove();
		}
	}

	@Nullable
	public ModifierCollection getCollection(@NotNull String internalName) {
		if (loading == null) return collections.get(internalName);

		ModifierCollection collection = collections.get(internalName);
		if (collection != null) return collection;

		return loadCollection(internalName);
	}

}
