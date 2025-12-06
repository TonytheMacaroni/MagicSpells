package com.nisovin.magicspells.spells.passive;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.GrindstoneInventory;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("grindstone")
public class GrindstoneListener extends PassiveListener {

	private Set<MagicItemData> upperItem;
	private Set<MagicItemData> lowerItem;
	private Set<MagicItemData> resultItem;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		String[] split = var.split(" ", 3);

		if (split.length > 0 && !split[0].equals("any")) {
			upperItem = Conversion.convert(
				ConversionSource.split(split[0], MagicItemDataParser.DATA_REGEX_PATTERN),
				Converters.MAGIC_ITEM_DATA,
				ConversionTarget.set(true)
			);
		}

		if (split.length > 1 && !split[1].equals("any")) {
			lowerItem = Conversion.convert(
				ConversionSource.split(split[1], MagicItemDataParser.DATA_REGEX_PATTERN),
				Converters.MAGIC_ITEM_DATA,
				ConversionTarget.set(true)
			);
		}

		if (split.length > 2 && !split[2].equals("any")) {
			resultItem = Conversion.convert(
				ConversionSource.split(split[0], MagicItemDataParser.DATA_REGEX_PATTERN),
				Converters.MAGIC_ITEM_DATA,
				ConversionTarget.set(true)
			);
		}
	}

	@OverridePriority
	@EventHandler
	public void onGrindstone(PrepareResultEvent event) {
		Inventory inventory = event.getInventory();
		if (!(inventory instanceof GrindstoneInventory grindstone)) return;

		LivingEntity caster = event.getView().getPlayer();
		if (!canTrigger(caster)) return;

		if (upperItem != null && !contains(upperItem, grindstone.getUpperItem())) return;
		if (lowerItem != null && !contains(lowerItem, grindstone.getLowerItem())) return;
		if (resultItem != null && !contains(resultItem, grindstone.getResult())) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setResult(null);
	}

	private boolean contains(Set<MagicItemData> items, ItemStack item) {
		if (item == null) item = new ItemStack(Material.AIR);

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (itemData == null) return false;

		for (MagicItemData data : items)
			if (data.matches(itemData))
				return true;

		return false;
	}

}
