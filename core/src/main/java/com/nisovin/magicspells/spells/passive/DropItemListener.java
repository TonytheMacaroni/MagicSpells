package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerDropItemEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

// Optional trigger variable that may contain a pipe separated list of items to accept
@Name("dropitem")
public class DropItemListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();
	
	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var, MagicItemDataParser.DATA_REGEX_PATTERN),
			Converters.MAGIC_ITEM_DATA,
			ConversionTarget.addTo(items)
		);
	}
	
	@OverridePriority
	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		if (!items.isEmpty()) {
			ItemStack item = event.getItemDrop().getItemStack();
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
