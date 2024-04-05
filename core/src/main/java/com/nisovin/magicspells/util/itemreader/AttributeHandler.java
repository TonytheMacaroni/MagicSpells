package com.nisovin.magicspells.util.itemreader;

import java.util.List;

import com.google.common.io.ByteStreams;
import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;
import com.google.common.io.ByteArrayDataOutput;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.AttributeUtil;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.managers.AttributeManager.AttributeInfo;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.ATTRIBUTES;

public class AttributeHandler {

	private static final String CONFIG_NAME = ATTRIBUTES.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data, Material material) {
		if (!config.isList(CONFIG_NAME)) return;

		Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();

		List<String> attributeStrings = config.getStringList(CONFIG_NAME);
		for (String attributeString : attributeStrings) {
			AttributeInfo info = getAttributeModifier(attributeString);
			if (info == null) continue;

			meta.addAttributeModifier(info.attribute(), info.attributeModifier());
			attributes.put(info.attribute(), info.attributeModifier());
		}

		if (!attributes.isEmpty()) data.setAttribute(ATTRIBUTES, attributes);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!data.hasAttribute(ATTRIBUTES)) return;
		meta.setAttributeModifiers((Multimap<Attribute, AttributeModifier>) data.getAttribute(ATTRIBUTES));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!meta.hasAttributeModifiers()) return;
		data.setAttribute(ATTRIBUTES, meta.getAttributeModifiers());
	}

	public static AttributeInfo getAttributeModifier(String attributeString) {
		String[] args = attributeString.split(" ");
		if (args.length < 2 || args.length > 5) {
			MagicDebug.warn("Invalid attribute modifier '%s' on magic item - too many or too few arguments.", attributeString);
			return null;
		}

		Attribute attribute = AttributeUtil.getAttribute(args[0]);
		if (attribute == null) {
			MagicDebug.warn("Invalid attribute '%s' on attribute modifier '%s' on magic item.", args[0], attributeString);
			return null;
		}

		double value;
		try {
			value = Double.parseDouble(args[1]);
		} catch (NumberFormatException e) {
			MagicDebug.warn("Invalid value '%s' on attribute modifier '%s' on magic item.", args[1], attributeString);
			return null;
		}

		AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
		if (args.length >= 3) {
			operation = AttributeUtil.getOperation(args[2]);

			if (operation == null) {
				MagicDebug.warn("Invalid operation '%s' on attribute modifier '%s' on magic item.", args[2], attributeString);
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
					MagicDebug.warn("Invalid slot '%s' on attribute modifier '%s' on magic item.", args[3], attributeString);
					return null;
				}
			}
		}

		java.util.UUID uuid;
		if (args.length == 5) {
			try {
				uuid = java.util.UUID.fromString(args[4]);
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid UUID '%s' on attribute modifier '%s' on magic item.", args[3], attributeString);
				return null;
			}
		} else {
			ByteArrayDataOutput output = ByteStreams.newDataOutput();
			output.writeUTF(attribute.getKey().asString());
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
