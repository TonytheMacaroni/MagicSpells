package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.DURABILITY;

public class DurabilityHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isInt(DURABILITY.getKey())) return invalidIfSet(config, DURABILITY);

		if (!(meta instanceof Damageable damageable && item.getType().getMaxDurability() > 0)) {
			MagicDebug.warn("Invalid 'durability' specified %s - item type '%s' does not have durability.", MagicDebug.resolveFullPath(), item.getType().getKey().getKey());
			return false;
		}

		int durability = config.getInt(DURABILITY.getKey());

		damageable.setDamage(durability);
		data.setAttribute(DURABILITY, durability);

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(DURABILITY) || !(meta instanceof Damageable damageable && item.getType().getMaxDurability() > 0))
			return;

		damageable.setDamage(data.getAttribute(DURABILITY));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof Damageable damageable && item.getType().getMaxDurability() > 0))
			return;

		data.setAttribute(DURABILITY, damageable.getDamage());
	}

	public static int getDurability(ItemMeta meta) {
		if (!(meta instanceof Damageable damageable)) return -1;
		return damageable.getDamage();
	}

}
