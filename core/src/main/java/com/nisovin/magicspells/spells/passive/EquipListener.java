package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent.EquipmentChange;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("equip")
public class EquipListener extends PassiveListener {

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
	public void onEquip(EntityEquipmentChangedEvent event) {
		LivingEntity caster = event.getEntity();
		if (!canTrigger(caster)) return;

		if (!items.isEmpty()) {
			boolean check = false;

			for (Map.Entry<EquipmentSlot, EquipmentChange> entry : event.getEquipmentChanges().entrySet()) {
				EquipmentSlot slot = entry.getKey();
				if (!slot.isArmor()) continue;

				EquipmentChange change = entry.getValue();

				ItemStack newItem = change.newItem();
				if (newItem.isEmpty()) return;

				MagicItemData newData = MagicItems.getMagicItemDataFromItemStack(newItem);
				if (newData == null) return;

				ItemStack oldItem = change.oldItem();
				MagicItemData oldData = MagicItems.getMagicItemDataFromItemStack(oldItem);

				if (contains(oldData, newData)) {
					check = true;
					break;
				}
			}

			if (!check) return;
		}

		passiveSpell.activate(caster);
	}

	private boolean contains(MagicItemData oldData, MagicItemData newData) {
		for (MagicItemData data : items)
			if ((oldData == null || !data.matches(oldData)) && data.matches(newData))
				return true;

		return false;
	}

}
