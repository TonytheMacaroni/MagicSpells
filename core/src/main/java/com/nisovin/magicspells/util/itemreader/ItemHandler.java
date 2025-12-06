package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public abstract class ItemHandler {

	public abstract boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data);

	public abstract void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data);

	public abstract void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data);

	public static boolean invalidIfSet(@NotNull ConfigurationSection config, @NotNull MagicItemData.MagicItemAttribute<?> attribute) {
		return invalidIfSet(config, attribute.getKey());
	}

	public static boolean invalidIfSet(@NotNull ConfigurationSection config, @NotNull String @NotNull... keys) {
		for (String key : keys) {
			if (config.isSet(key)) {
				MagicDebug.warn("Invalid value '%s' found for magic item option '%s' %s.", config.getString(key), key, MagicDebug.resolveFullPath());
				return false;
			}
		}

		return true;
	}

}
