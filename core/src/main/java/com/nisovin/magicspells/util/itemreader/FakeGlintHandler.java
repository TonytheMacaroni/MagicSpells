package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.FAKE_GLINT;

public class FakeGlintHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isBoolean(FAKE_GLINT.getKey())) return invalidIfSet(config, FAKE_GLINT);

		if (config.getBoolean(FAKE_GLINT.getKey())) {
			meta.setEnchantmentGlintOverride(true);
			data.setAttribute(FAKE_GLINT, true);
		}

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(FAKE_GLINT) || !data.getAttribute(FAKE_GLINT)) return;

		meta.setEnchantmentGlintOverride(true);
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!meta.hasEnchantmentGlintOverride() || !meta.getEnchantmentGlintOverride()) return;

		data.setAttribute(FAKE_GLINT, true);
	}

}
