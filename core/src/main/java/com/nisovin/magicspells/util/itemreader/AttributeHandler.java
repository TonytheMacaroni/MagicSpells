package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import com.google.common.io.ByteStreams;
import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;
import com.google.common.io.ByteArrayDataOutput;

import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.AttributeUtil;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.managers.AttributeManager.AttributeInfo;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.ATTRIBUTES;

public class AttributeHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isList(ATTRIBUTES.getKey())) return invalidIfSet(config, ATTRIBUTES);

		Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();

		List<String> attributeStrings = config.getStringList(ATTRIBUTES.getKey());
		for (String attributeString : attributeStrings) {
			AttributeInfo info = getAttributeModifier(attributeString);
			if (info == null) return false;

			attributes.put(info.attribute(), info.attributeModifier());
		}

		if (!attributes.isEmpty()) {
			meta.setAttributeModifiers(attributes);
			data.setAttribute(ATTRIBUTES, attributes);
		}

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(ATTRIBUTES)) return;

		meta.setAttributeModifiers(data.getAttribute(ATTRIBUTES));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!meta.hasAttributeModifiers()) return;

		data.setAttribute(ATTRIBUTES, meta.getAttributeModifiers());
	}

	public static AttributeInfo getAttributeModifier(String attributeString) {
		String[] args = attributeString.split(" ");
		if (args.length < 2 || args.length > 5) {
			MagicDebug.warn("Invalid attribute modifier '%s' %s - too many or too few arguments.", attributeString, MagicDebug.resolveFullPath());
			return null;
		}

		Attribute attribute = AttributeUtil.getAttribute(args[0]);
		if (attribute == null) {
			MagicDebug.warn("Invalid attribute '%s' on attribute modifier '%s' %s.", args[0], attributeString, MagicDebug.resolveFullPath());
			return null;
		}

		double value;
		try {
			value = Double.parseDouble(args[1]);
		} catch (NumberFormatException e) {
			MagicDebug.warn("Invalid value '%s' on attribute modifier '%s' %s.", args[1], attributeString, MagicDebug.resolveFullPath());
			return null;
		}

		AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
		if (args.length >= 3) {
			operation = AttributeUtil.getOperation(args[2]);

			if (operation == null) {
				MagicDebug.warn("Invalid operation '%s' on attribute modifier '%s' %s.", args[2], attributeString, MagicDebug.resolveFullPath());
				return null;
			}
		}

		EquipmentSlot slot = null;
		if (args.length >= 4) {
			String slotString = args[3].toUpperCase();

			try {
				slot = EquipmentSlot.valueOf(slotString);
			} catch (IllegalArgumentException e) {
				boolean valid = true;

				slot = switch (slotString) {
					case "MAINHAND", "MAIN_HAND" -> EquipmentSlot.HAND;
					case "OFFHAND" -> EquipmentSlot.OFF_HAND;
					case "ANY", "*" -> null;
					default -> {
						valid = false;
						yield null;
					}
				};

				if (!valid) {
					MagicDebug.warn("Invalid slot '%s' on attribute modifier '%s' %s.", args[3], attributeString, MagicDebug.resolveFullPath());
					return null;
				}
			}
		}

		java.util.UUID uuid;
		if (args.length == 5) {
			try {
				uuid = java.util.UUID.fromString(args[4]);
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid UUID '%s' on attribute modifier '%s' %s.", args[3], attributeString, MagicDebug.resolveFullPath());
				return null;
			}
		} else {
			ByteArrayDataOutput output = ByteStreams.newDataOutput();
			output.writeUTF(attribute.name());
			output.writeUTF(args[0]);
			output.writeDouble(value);
			output.writeInt(operation.ordinal());
			if (slot != null) output.writeInt(slot.ordinal());

			uuid = java.util.UUID.nameUUIDFromBytes(output.toByteArray());
		}

		AttributeModifier modifier = new AttributeModifier(uuid, args[0], value, operation, slot);

		return new AttributeInfo(attribute, modifier);
	}

}
