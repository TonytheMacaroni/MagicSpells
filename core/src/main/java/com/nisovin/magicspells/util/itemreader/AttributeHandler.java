package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.google.common.io.ByteStreams;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.collect.LinkedHashMultimap;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.AttributeUtil;
import com.nisovin.magicspells.util.ConfigReaderUtil;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.ATTRIBUTES;

public class AttributeHandler {

	private static final String CONFIG_NAME = ATTRIBUTES.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!config.isList(CONFIG_NAME)) return;

		Multimap<Attribute, AttributeModifier> modifiers = getAttributeModifiers(config.getList(CONFIG_NAME));
		if (modifiers.isEmpty()) return;

		meta.setAttributeModifiers(modifiers);
		data.setAttribute(ATTRIBUTES, modifiers);
	}

	public static void processItemMeta(@NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(ATTRIBUTES)) return;

		meta.setAttributeModifiers((Multimap<Attribute, AttributeModifier>) data.getAttribute(ATTRIBUTES));
	}

	public static void processMagicItemData(@NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!meta.hasAttributeModifiers()) return;

		data.setAttribute(ATTRIBUTES, meta.getAttributeModifiers());
	}

	public static LinkedHashMultimap<Attribute, AttributeModifier> getAttributeModifiers(List<?> data) {
		return getAttributeModifiers(data, null);
	}

	@SuppressWarnings("UnstableApiUsage")
	public static LinkedHashMultimap<Attribute, AttributeModifier> getAttributeModifiers(List<?> data, String spellName) {
		LinkedHashMultimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();

		for (int i = 0; i < data.size(); i++) {
			Object object = data.get(i);

			switch (object) {
				case String string -> {
					String[] args = string.split(" ");
					if (args.length < 2 || args.length > 5) {
						MagicDebug.warn("Invalid attribute modifier '%s' on magic item - too many or too few arguments.", string);
						continue;
					}

					Attribute attribute = AttributeUtil.getAttribute(args[0]);
					if (attribute == null) {
						MagicDebug.warn("Invalid attribute '%s' on attribute modifier '%s' on magic item.", args[0], string);
						continue;
					}

					double value;
					try {
						value = Double.parseDouble(args[1]);
					} catch (NumberFormatException e) {
						MagicDebug.warn("Invalid value '%s' on attribute modifier '%s' on magic item.", args[1], string);
						continue;
					}

					AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
					if (args.length >= 3) {
						operation = AttributeUtil.getOperation(args[2]);

						if (operation == null) {
							MagicDebug.warn("Invalid operation '%s' on attribute modifier '%s' on magic item.", args[2], string);
							continue;
						}
					}

					EquipmentSlotGroup group = EquipmentSlotGroup.ANY;
					if (args.length >= 4) {
						group = switch (args[3].toLowerCase()) {
							case "main_hand", "hand" -> EquipmentSlotGroup.MAINHAND;
							case "off_hand" -> EquipmentSlotGroup.OFFHAND;
							case "*" -> EquipmentSlotGroup.ANY;
							case String s -> EquipmentSlotGroup.getByName(s);
						};

						if (group == null) {
							MagicDebug.warn("Invalid equipment slot group '%s' on attribute modifier '%s' on magic item.", args[3], string);
							continue;
						}
					}

					NamespacedKey key;
					if (args.length == 5) {
						key = NamespacedKey.fromString(args[4], MagicSpells.getInstance());

						if (key == null) {
							MagicDebug.warn("Invalid namespaced key '%s' on attribute modifier '%s' on magic item.", args[4], string);
							continue;
						}
					} else {
						ByteArrayDataOutput output = ByteStreams.newDataOutput();
						if (spellName != null) output.writeUTF(spellName);
						output.writeInt(i);
						output.writeUTF(attribute.key().asString());
						output.writeDouble(value);
						output.writeDouble(operation.ordinal());
						output.writeUTF(group.toString());

						UUID uuid = java.util.UUID.nameUUIDFromBytes(output.toByteArray());
						key = new NamespacedKey(MagicSpells.getInstance(), uuid.toString());
					}

					modifiers.put(attribute, new AttributeModifier(key, value, operation, group));
				}
				case Map<?, ?> map -> {
					ConfigurationSection config = ConfigReaderUtil.mapToSection(map);

					String attributeString = config.getString("type");
					if (attributeString == null) {
						MagicDebug.warn("No 'type' specified on attribute modifier on magic item.");
						continue;
					}

					Attribute attribute = AttributeUtil.getAttribute(attributeString);
					if (attribute == null) {
						MagicDebug.warn("Invalid attribute '%s' specified for 'type' on attribute modifier on magic item.", attributeString);
						continue;
					}

					Object amountObj = config.get("amount");
					if (!(amountObj instanceof Number amount)) {
						if (amountObj == null) MagicDebug.warn("No 'amount' specified on attribute modifier.");
						else MagicDebug.warn("Invalid value '%s' specified for 'amount' on attribute modifier on magic item.", amountObj);

						continue;
					}

					String operationString = config.getString("operation");
					if (operationString == null) {
						MagicDebug.warn("No 'operation' specified on attribute modifier on magic item.");
						continue;
					}

					AttributeModifier.Operation operation = AttributeUtil.getOperation(operationString);
					if (operation == null) {
						MagicDebug.warn("Invalid operation '%s' specified for 'operation' on attribute modifier on magic item.", operationString);
						continue;
					}

					String slotString = config.getString("slot");
					if (slotString == null) {
						MagicDebug.warn("No 'slot' specified on attribute modifier.");
						continue;
					}

					EquipmentSlotGroup group = switch (slotString.toLowerCase()) {
						case "main_hand", "hand" -> EquipmentSlotGroup.MAINHAND;
						case "off_hand" -> EquipmentSlotGroup.OFFHAND;
						case "*" -> EquipmentSlotGroup.ANY;
						case String s -> EquipmentSlotGroup.getByName(s);
					};

					if (group == null) {
						MagicDebug.warn("Invalid equipment slot group '%s' specified for 'slot' on attribute modifier on magic item.", slotString);
						continue;
					}

					String idString = config.getString("id");
					if (idString == null) {
						MagicDebug.warn("No 'id' specified on attribute modifier on magic item.");
						continue;
					}

					NamespacedKey id = NamespacedKey.fromString(idString, MagicSpells.getInstance());
					if (id == null) {
						MagicDebug.warn("Invalid namespaced key '%s' specified for 'id' on attribute modifier on magic item.", idString);
						continue;
					}

					modifiers.put(attribute, new AttributeModifier(id, amount.doubleValue(), operation, group));
				}
				default -> MagicDebug.warn("Invalid attribute modifier '%s' on magic item.", object);
			}
		}

		return modifiers;
	}

}