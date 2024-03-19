package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

import net.kyori.adventure.text.Component;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.LORE;

public class LoreHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isList(LORE.getKey()) && !config.isString(LORE.getKey())) return invalidIfSet(config, LORE);

		List<Component> lore = new ArrayList<>();
		if (config.isList(LORE.getKey())) {
			List<String> loreLines = config.getStringList(LORE.getKey());
			for (String line : loreLines) lore.add(Util.getMiniMessage(line));
		} else {
			String line = config.getString(LORE.getKey());
			lore.add(Util.getMiniMessage(line));
		}

		if (!lore.isEmpty()) {
			meta.lore(lore);
			data.setAttribute(LORE, meta.lore());
		}

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(LORE)) return;

		meta.lore(data.getAttribute(LORE));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!meta.hasLore()) return;

		data.setAttribute(LORE, meta.lore());
	}

}
