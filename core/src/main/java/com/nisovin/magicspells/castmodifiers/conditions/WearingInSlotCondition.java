package com.nisovin.magicspells.castmodifiers.conditions;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.List;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlotGroup;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("wearinginslot")
public class WearingInSlotCondition extends Condition {

	private final Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);
	private final List<MagicItemData> items = new ArrayList<>();
	private boolean emptyCheck = false;

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;

		String[] data = var.split("=");
		if (data.length != 2) return false;

		Predicate<EquipmentSlot> validSlots = null;
		Predicate<EquipmentSlot> invalidSlots = null;

		for (String groupString : data[0].split(",")) {
			boolean negate = false;
			if (groupString.startsWith("!")) {
				groupString = groupString.substring(1);
				negate = true;
			}

			EquipmentSlotGroup group = switch (groupString.toLowerCase()) {
				case "helm", "hat" -> EquipmentSlotGroup.HEAD;
				case "tunic" -> EquipmentSlotGroup.CHEST;
				case "leg", "pant" -> EquipmentSlotGroup.LEGS;
				case "boot", "shoe" -> EquipmentSlotGroup.FEET;
				case String s -> EquipmentSlotGroup.getByName(s);
			};

			if (group == null) {
				MagicDebug.warn("Invalid equipment slot group '%s' %s.", MagicDebug.resolveFullPath());
				return false;
			}

			if (negate) {
				if (invalidSlots == null) invalidSlots = group.negate();
				else invalidSlots = invalidSlots.and(group.negate());
			} else {
				if (validSlots == null) validSlots = group;
				else validSlots = validSlots.or(group);
			}
		}

		if (validSlots == null) validSlots = s -> true;
		if (invalidSlots == null) invalidSlots = s -> true;

		Predicate<EquipmentSlot> slotPredicate = validSlots.and(invalidSlots);

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!slotPredicate.test(slot)) continue;
			slots.add(slot);
		}

		for (String magicItemString : data[1].split(MagicItemDataParser.DATA_REGEX)) {
			if (List.of("0", "air", "empty").contains(magicItemString)) {
				emptyCheck = true;
				continue;
			}

			MagicItemData itemData = MagicItems.getMagicItemDataFromString(magicItemString);
			if (itemData == null) return false;

			items.add(itemData);
		}

		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		EntityEquipment equipment = caster.getEquipment();
		if (equipment == null) return false;

		for (EquipmentSlot slot : slots) {
			if (!caster.canUseEquipmentSlot(slot)) continue;

			ItemStack item = equipment.getItem(slot);
			if (contains(item)) return true;
		}

		return false;
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean contains(ItemStack item) {
		if (item.isEmpty()) return emptyCheck;

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (itemData == null) return false;

		for (MagicItemData data : items) {
			if (data.matches(itemData))
				return true;
		}

		return false;
	}

}
