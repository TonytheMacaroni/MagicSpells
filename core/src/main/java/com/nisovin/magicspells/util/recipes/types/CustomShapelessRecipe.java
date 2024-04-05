package com.nisovin.magicspells.util.recipes.types;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.recipes.CustomRecipe;

public class CustomShapelessRecipe extends CustomRecipe {

	private final CraftingBookCategory category;
	private final List<RecipeChoice> ingredients = new ArrayList<>();

	public CustomShapelessRecipe(ConfigurationSection config) {
		super(config);
		category = resolveEnum(CraftingBookCategory.class, "category" , CraftingBookCategory.MISC, "crafting book category");

		String path = "ingredients";
		ConfigurationSection ingredientsConfig;
		if (config.isList(path)) {
			// Backward compatibility:
			List<?> ingredientList = config.getList(path, new ArrayList<>());
			ingredientsConfig = config.createSection(path);
			for (int i = 0; i < ingredientList.size(); i++) {
				ingredientsConfig.set(i + "", ingredientList.get(i));
			}
		} else ingredientsConfig = config.getConfigurationSection(path);

		if (ingredientsConfig == null) {
			MagicDebug.error("No ingredients defined for custom shapeless recipe '%s'.", config.getName());
			return;
		}

		for (String key : ingredientsConfig.getKeys(false)) {
			RecipeChoice choice = resolveRecipeChoice(path + "." + key);
			if (choice == null) continue;

			ingredients.add(choice);
		}
	}

	@Override
	public Recipe build() {
		ShapelessRecipe recipe = new ShapelessRecipe(namespaceKey, result);
		recipe.setGroup(group);
		recipe.setCategory(category);
		ingredients.forEach(recipe::addIngredient);
		return recipe;
	}

}
