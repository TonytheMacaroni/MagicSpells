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
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("unequip")
public class UnequipListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		for (String s : var.split(MagicItemDataParser.DATA_REGEX)) {
			MagicItemData itemData = MagicItems.getMagicItemDataFromString(s.trim());
			if (itemData != null) items.add(itemData);
		}
	}

	@OverridePriority
	@EventHandler
	public void onUnequip(EntityEquipmentChangedEvent event) {
		LivingEntity caster = event.getEntity();
		if (!canTrigger(caster)) return;

		if (!items.isEmpty()) {
			boolean check = false;

			for (Map.Entry<EquipmentSlot, EquipmentChange> entry : event.getEquipmentChanges().entrySet()) {
				EquipmentSlot slot = entry.getKey();
				if (!slot.isArmor()) continue;

				EquipmentChange change = entry.getValue();

				ItemStack oldItem = change.oldItem();
				if (oldItem.isEmpty()) return;

				MagicItemData oldData = MagicItems.getMagicItemDataFromItemStack(oldItem);
				if (oldData == null) return;

				ItemStack newItem = change.newItem();
				MagicItemData newData = MagicItems.getMagicItemDataFromItemStack(newItem);

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
			if (data.matches(oldData) && (newData == null || !data.matches(newData)))
				return true;

		return false;
	}

}
