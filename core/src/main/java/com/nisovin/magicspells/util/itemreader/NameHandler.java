package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.NAME;

public class NameHandler extends ItemHandler {

	private static final String CONFIG_NAME = NAME.toString();

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isString(NAME.getKey())) return invalidIfSet(config, NAME);

		meta.displayName(Util.getMiniMessage(config.getString(CONFIG_NAME)));
		data.setAttribute(NAME, meta.displayName());

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(NAME)) return;

		meta.displayName(data.getAttribute(NAME));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!meta.hasDisplayName()) return;

		data.setAttribute(NAME, meta.displayName());
	}
	
}
