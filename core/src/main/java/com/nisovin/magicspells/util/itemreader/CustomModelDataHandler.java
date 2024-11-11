package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.CUSTOM_MODEL_DATA;

public class CustomModelDataHandler extends ItemHandler {

	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		String key = CUSTOM_MODEL_DATA.getKey();
		if (!config.isInt(key)) return invalidIfSet(config, key);

		int customModelData = config.getInt(key);

		meta.setCustomModelData(customModelData);
		data.setAttribute(CUSTOM_MODEL_DATA, customModelData);

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(CUSTOM_MODEL_DATA)) return;

		meta.setCustomModelData(data.getAttribute(CUSTOM_MODEL_DATA));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!meta.hasCustomModelData()) return;

		data.setAttribute(CUSTOM_MODEL_DATA, meta.getCustomModelData());
	}

}
