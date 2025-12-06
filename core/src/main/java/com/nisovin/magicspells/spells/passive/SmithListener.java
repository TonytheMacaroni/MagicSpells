package com.nisovin.magicspells.spells.passive;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.event.inventory.PrepareSmithingEvent;

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

@Name("smith")
public class SmithListener extends PassiveListener {

	private Set<MagicItemData> firstItem;
	private Set<MagicItemData> secondItem;
	private Set<MagicItemData> resultItem;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		String[] split = var.split(" ", 3);

		if (split.length > 0) {
			if (!split[0].equals("any")) {
				firstItem = Conversion.convert(
					ConversionSource.split(split[0], MagicItemDataParser.DATA_REGEX_PATTERN),
					Converters.MAGIC_ITEM_DATA,
					ConversionTarget.set(true)
				);
			}
		}

		if (split.length > 1) {
			if (!split[1].equals("any")) {
				secondItem = Conversion.convert(
					ConversionSource.split(split[1], MagicItemDataParser.DATA_REGEX_PATTERN),
					Converters.MAGIC_ITEM_DATA,
					ConversionTarget.set(true)
				);
			}
		}

		if (split.length > 2) {
			if (!split[2].equals("any")) {
				resultItem = Conversion.convert(
					ConversionSource.split(split[2], MagicItemDataParser.DATA_REGEX_PATTERN),
					Converters.MAGIC_ITEM_DATA,
					ConversionTarget.set(true)
				);
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onSmith(PrepareSmithingEvent event) {
		SmithingInventory inventory = event.getInventory();

		LivingEntity caster = event.getView().getPlayer();
		if (!canTrigger(caster)) return;

		if (firstItem != null && !contains(firstItem, inventory.getInputEquipment())) return;
		if (secondItem != null && !contains(secondItem, inventory.getInputMineral())) return;
		if (resultItem != null && !contains(resultItem, inventory.getResult())) return;

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
