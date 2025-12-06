package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.REPAIR_COST;

public class RepairableHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof Repairable repairable)) return true;

		if (!config.isInt(REPAIR_COST.getKey())) return invalidIfSet(config, REPAIR_COST);

		int repairCost = config.getInt(REPAIR_COST.getKey());
		repairable.setRepairCost(repairCost);
		data.setAttribute(REPAIR_COST, repairCost);

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof Repairable repairable) || !data.hasAttribute(REPAIR_COST)) return;

		repairable.setRepairCost(data.getAttribute(REPAIR_COST));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof Repairable repairable) || !repairable.hasRepairCost()) return;

		data.setAttribute(REPAIR_COST, repairable.getRepairCost());
	}
	
}
