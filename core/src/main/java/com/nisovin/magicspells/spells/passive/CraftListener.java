package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.CraftItemEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("craft")
public class CraftListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(@NotNull String var) {
		ConversionUtil.convert(
			ConversionSource.split(var, MagicItemDataParser.DATA_REGEX_PATTERN),
			ConversionTarget.consumer(items::add),
			Converters.MAGIC_ITEM_DATA
		);
	}

	@OverridePriority
	@EventHandler
	public void onCraft(CraftItemEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		HumanEntity caster = event.getWhoClicked();
		if (!canTrigger(caster)) return;

		if (!items.isEmpty()) {
			ItemStack item = event.getCurrentItem();
			if (item == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
			if (itemData == null || !contains(itemData)) return;
		}

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.matches(itemData)) return true;
		}
		return false;
	}

}
