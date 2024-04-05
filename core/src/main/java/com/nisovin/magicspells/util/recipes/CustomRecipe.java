package com.nisovin.magicspells.util.recipes;

import java.util.*;
import java.lang.reflect.Field;

import com.destroystokyo.paper.MaterialTags;
import com.destroystokyo.paper.MaterialSetTag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.ConfigReaderUtil;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public abstract class CustomRecipe {

	private static final Map<String, MaterialSetTag> MATERIAL_TAGS = new HashMap<>();
	static {
		for (Field field : MaterialTags.class.getDeclaredFields()) {
			try {
				if (!(field.get(null) instanceof MaterialSetTag tag)) continue;
				MATERIAL_TAGS.put(field.getName(), tag);
			}
			catch (IllegalAccessException ignored) {}
		}
	}

	protected boolean error = false;

	protected ConfigurationSection config;

	protected String group;
	protected ItemStack result;
	protected NamespacedKey namespaceKey;

	private CustomRecipe() {}

	public CustomRecipe(ConfigurationSection config) {
		this.config = config;

		// Recipe group
		group = config.getString("group", "");

		// Result item
		MagicItem magicItem = getMagicItem(config.get("result"));
		if (magicItem == null) {
			MagicDebug.error("Invalid magic item defined for 'result' on custom recipe '%s'.", config.getName());
			error = true;
			return;
		}

		result = magicItem.getItemStack().clone();

		// Result quantity
		int quantity = config.getInt("quantity", 1);
		result.setAmount(Math.max(1, quantity));

		// Namespace key
		String namespaceKeyString = config.getString("namespace-key", config.getName());
		try {
			namespaceKey = new NamespacedKey(MagicSpells.getInstance(), namespaceKeyString);
		} catch (IllegalArgumentException e) {
			MagicDebug.error(e, "Invalid 'namespace-key' on custom recipe '%s': %s", config.getName(), namespaceKeyString);
			error = true;
		}
	}

	public boolean hadError() {
		return error;
	}

	public abstract Recipe build();

	protected RecipeChoice resolveRecipeChoice(String path) {
		if (!config.isList(path)) {
			Object object = config.get(path);
			if (object instanceof String tagName && tagName.startsWith("tag:")) {
				MaterialSetTag tag = resolveMaterialTag(path, tagName);
				return tag == null ? null : new RecipeChoice.MaterialChoice(tag);
			}

			MagicItem magicItem = getMagicItem(object);
			if (magicItem == null) {
				MagicDebug.error("Invalid magic item defined for '%s' on custom recipe '%s'.", path, config.getName());
				error = true;
				return null;
			}

			return new RecipeChoice.ExactChoice(getLoreVariants(magicItem));
		}

		boolean isExpectingTags = false;
		List<ItemStack> items = new ArrayList<>();
		List<Material> materials = new ArrayList<>();
		List<?> list = config.getList(path, new ArrayList<>());
		for (int i = 0; i < list.size(); i++) {
			Object object = list.get(i);

			if (object instanceof String tagName && tagName.startsWith("tag:")) {
				isExpectingTags = true;
				MaterialSetTag tag = resolveMaterialTag(path, tagName);
				if (tag == null) return null;
				materials.addAll(tag.getValues());
				continue;
			}

			if (isExpectingTags) {
				MagicDebug.error("Invalid entry on custom recipe '%s' at index %d of '%s' - you cannot mix material tags and item-based recipe choices together.", config.getName(), i, path);
				error = true;
				return null;
			}

			MagicItem magicItem = getMagicItem(object);
			if (magicItem == null) {
				MagicDebug.error("Invalid magic item listed on custom recipe '%s' at index %d of '%s'.", config.getName(), i, path);
				error = true;
				return null;
			}

			items.addAll(getLoreVariants(magicItem));
		}
		return isExpectingTags ?
				new RecipeChoice.MaterialChoice(materials) :
				new RecipeChoice.ExactChoice(items);
	}

	private List<ItemStack> getLoreVariants(MagicItem magicItem) {
		List<ItemStack> list = new ArrayList<>();
		ItemStack originalItem = magicItem.getItemStack().clone();
		list.add(originalItem);
		ItemStack item = originalItem.clone();

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return list;
		Component displayName = meta.displayName();
		if (displayName == null) return list;
		if (displayName.hasDecoration(TextDecoration.ITALIC)) return list;
		// Remove default "false" italics.
		meta.displayName(displayName.decoration(TextDecoration.ITALIC, TextDecoration.State.NOT_SET));
		item.setItemMeta(meta);
		list.add(item);
		return list;

	}

	protected MaterialSetTag resolveMaterialTag(String path, String tagName) {
		tagName = tagName.replaceFirst("tag:", "");

		MaterialSetTag tag = MATERIAL_TAGS.get(tagName.toUpperCase());
		if (tag == null) {
			MagicDebug.error("Invalid material tag '%s' on option '%s' of custom recipe '%s'.", tagName, path, config.getName());
			error = true;
		}

		return tag;
	}

	protected MagicItem getMagicItem(Object object) {
		if (object instanceof String string) return MagicItems.getMagicItemFromString(string);
		if (object instanceof Map<?, ?> map) {
			ConfigurationSection config = ConfigReaderUtil.mapToSection(map);
			return MagicItems.getMagicItemFromSection(config);
		}
		return null;
	}

	protected <T extends Enum<T>> T resolveEnum(Class<T> enumClass, String path, T def, String type) {
		String received = config.getString(path);
		if (received == null) return def;

		try {
			return Enum.valueOf(enumClass, received.toUpperCase());
		} catch (IllegalArgumentException e) {
			MagicDebug.error("Invalid %s '%s' for option '%s' on custom recipe '%s'.", type, received, path, config.getName());
			error = true;
		}

		return null;
	}

}
