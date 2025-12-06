package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

// Trigger variable of a pipe separated list of items to accept
@Name("rightclickitem")
public class RightClickItemListener extends PassiveListener {

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
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!isCancelStateOk(isCancelled(event))) return;
		if (!event.hasItem()) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		if (!items.isEmpty()) {
			ItemStack item = event.getItem();
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

	private boolean isCancelled(PlayerInteractEvent event) {
		return event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY;
	}

}
