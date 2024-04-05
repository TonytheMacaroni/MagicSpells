package com.nisovin.magicspells.util.recipes;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.Util;

public class CustomRecipes {

	private static final Map<NamespacedKey, Recipe> recipes = new HashMap<>();

	public static void create(ConfigurationSection config) {
		String typeName = config.getString("type");
		if (typeName == null) {
			MagicDebug.error(DebugCategory.RECIPES, "Recipe '%s' does not have a type defined.", config.getName());
			return;
		}

		CustomRecipeType type;
		try {
			type = CustomRecipeType.valueOf(typeName.toUpperCase());
		} catch (IllegalArgumentException e) {
			MagicDebug.error(DebugCategory.RECIPES, "Recipe '%s' has an invalid 'type' defined: %s.", config.getName(), typeName);
			return;
		}

		CustomRecipe customRecipe = type.newInstance(config);
		if (customRecipe.hadError()) return;

		try {
			Recipe recipe = customRecipe.build();
			recipes.put(customRecipe.namespaceKey, recipe);
			Bukkit.addRecipe(recipe);
			Util.forEachPlayerOnline(player -> player.discoverRecipe(customRecipe.namespaceKey));
		} catch (IllegalArgumentException e) {
			MagicDebug.error(e, "Encountered error while loading recipe '%s'.", config.getName());
		}
	}

	public static Map<NamespacedKey, Recipe> getRecipes() {
		return recipes;
	}

	public static void clearRecipes() {
		Util.forEachPlayerOnline(player -> player.undiscoverRecipes(recipes.keySet()));
		recipes.keySet().forEach(Bukkit::removeRecipe);
		recipes.clear();
		Bukkit.updateRecipes();
	}

}
