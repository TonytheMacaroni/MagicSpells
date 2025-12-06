package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.COLOR;

public class LeatherArmorHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof LeatherArmorMeta armorMeta)) return true;

		String key = COLOR.getKey();
		if (!config.isString(key)) return invalidIfSet(config, key);

		String colorString = config.getString(key);

		Color color = ColorUtil.getColorFromHexString(colorString, false);
		if (color == null) {
			MagicDebug.warn("Invalid color '%s' %s.", colorString, MagicDebug.resolveFullPath(key));
			return false;
		}

		if (!color.equals(Bukkit.getItemFactory().getDefaultLeatherColor())) {
			armorMeta.setColor(color);
			data.setAttribute(COLOR, color);
		}

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof LeatherArmorMeta armorMeta) || !data.hasAttribute(COLOR)) return;

		armorMeta.setColor(data.getAttribute(COLOR));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof LeatherArmorMeta armorMeta)) return;

		Color color = armorMeta.getColor();
		if (!color.equals(Bukkit.getItemFactory().getDefaultLeatherColor())) data.setAttribute(COLOR, color);
	}

	public static Color getColor(ItemMeta meta) {
		return meta instanceof LeatherArmorMeta armorMeta ? armorMeta.getColor() : null;
	}

}
