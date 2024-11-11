package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.google.common.io.ByteStreams;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.collect.LinkedHashMultimap;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.AttributeUtil;
import com.nisovin.magicspells.util.ConfigReaderUtil;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.ATTRIBUTES;

public class AttributeHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		String key = ATTRIBUTES.getKey();
		if (!config.isList(key)) return invalidIfSet(config, key);

		Multimap<Attribute, AttributeModifier> modifiers = getAttributeModifiers(config.getList(key), key);
		if (modifiers == null) return false;

		if (!modifiers.isEmpty()) {
			meta.setAttributeModifiers(modifiers);
			data.setAttribute(ATTRIBUTES, modifiers);
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

	@Nullable
	public static LinkedHashMultimap<Attribute, AttributeModifier> getAttributeModifiers(@NotNull List<?> data, @NotNull String name) {
		return getAttributeModifiers(data, name, null);
	}

	@Nullable
	public static LinkedHashMultimap<Attribute, AttributeModifier> getAttributeModifiers(@NotNull List<?> modifierData, @NotNull String name, @Nullable String spellName) {
		try (var ignored = MagicDebug.section("Resolving attribute modifier list from '%s'.", name)
			.pushPath(name, DebugPath.Type.LIST)
		) {
			LinkedHashMultimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();

			for (int i = 0; i < modifierData.size(); i++) {
				try (var ignored1 = MagicDebug.pushListEntry(i)) {
					Object value = modifierData.get(i);

					switch (value) {
						case String string -> {
							try (var ignored2 = MagicDebug.section("Resolving string-based attribute modifier '%s'.", string)) {
								AttributeData data = getAttributeFromString(string, spellName, i);
								if (data == null) return null;

								modifiers.put(data.attribute, data.modifier);
							}
						}
						case Map<?, ?> map -> {
							try (var ignored2 = MagicDebug.section("Resolving section-based attribute modifier.")) {
								ConfigurationSection config = ConfigReaderUtil.mapToSection(map);

								AttributeData data = getAttributeFromSection(config);
								if (data == null) return null;

								modifiers.put(data.attribute, data.modifier);
							}
						}
						default -> {
							MagicDebug.warn("Invalid attribute modifier %s.", MagicDebug.resolveFullPath());
							return null;
						}
					}
				}
			}

			return modifiers;
		}
	}

	@Nullable
	@SuppressWarnings("UnstableApiUsage")
	private static AttributeData getAttributeFromString(@NotNull String string, @Nullable String spellName, int ordinal) {
		String[] args = string.split(" ");
		if (args.length < 2 || args.length > 5) {
			MagicDebug.warn("Invalid attribute modifier '%s' %s - too many or too few arguments.", string, MagicDebug.resolveFullPath());
			return null;
		}

		Attribute attribute = AttributeUtil.getAttribute(args[0]);
		if (attribute == null) {
			MagicDebug.warn("Invalid attribute '%s' on attribute modifier '%s' %s.", args[0], string, MagicDebug.resolveFullPath());
			return null;
		}

		double value;
		try {
			value = Double.parseDouble(args[1]);
		} catch (NumberFormatException e) {
			MagicDebug.warn("Invalid value '%s' on attribute modifier '%s' %s.", args[1], string, MagicDebug.resolveFullPath());
			return null;
		}

		AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
		if (args.length >= 3) {
			operation = AttributeUtil.getOperation(args[2]);

			if (operation == null) {
				MagicDebug.warn("Invalid operation '%s' on attribute modifier '%s' %s.", args[2], string, MagicDebug.resolveFullPath());
				return null;
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
				MagicDebug.warn("Invalid equipment slot group '%s' on attribute modifier '%s' %s.", args[3], string, MagicDebug.resolveFullPath());
				return null;
			}
		}

		NamespacedKey key;
		if (args.length == 5) {
			key = NamespacedKey.fromString(args[4], MagicSpells.getInstance());

			if (key == null) {
				MagicDebug.warn("Invalid namespaced key '%s' on attribute modifier '%s' %s.", args[4], string, MagicDebug.resolveFullPath());
				return null;
			}
		} else {
			ByteArrayDataOutput output = ByteStreams.newDataOutput();
			if (spellName != null) output.writeUTF(spellName);
			output.writeInt(ordinal);
			output.writeUTF(attribute.key().asString());
			output.writeDouble(value);
			output.writeDouble(operation.ordinal());
			output.writeUTF(group.toString());

			UUID uuid = java.util.UUID.nameUUIDFromBytes(output.toByteArray());
			key = new NamespacedKey(MagicSpells.getInstance(), uuid.toString());
		}

		return new AttributeData(attribute, new AttributeModifier(key, value, operation, group));
	}

	@Nullable
	@SuppressWarnings("UnstableApiUsage")
	private static AttributeData getAttributeFromSection(@NotNull ConfigurationSection config) {
		String attributeString = config.getString("type");
		if (attributeString == null) {
			MagicDebug.warn("No 'type' specified on attribute modifier %s.", MagicDebug.resolveFullPath());
			return null;
		}

		Attribute attribute = AttributeUtil.getAttribute(attributeString);
		if (attribute == null) {
			MagicDebug.warn("Invalid attribute '%s' specified %s.", attributeString, MagicDebug.resolveFullPath("type"));
			return null;
		}

		Object amountObj = config.get("amount");
		if (!(amountObj instanceof Number amount)) {
			if (amountObj == null) MagicDebug.warn("No 'amount' specified on attribute modifier.");
			else MagicDebug.warn("Invalid value '%s' specified %s.", amountObj, MagicDebug.resolveFullPath("amount"));

			return null;
		}

		String operationString = config.getString("operation");
		if (operationString == null) {
			MagicDebug.warn("No 'operation' specified %s.", MagicDebug.resolveFullPath());
			return null;
		}

		AttributeModifier.Operation operation = AttributeUtil.getOperation(operationString);
		if (operation == null) {
			MagicDebug.warn("Invalid operation '%s' specified %s.", operationString, MagicDebug.resolveFullPath("operation"));
			return null;
		}

		String slotString = config.getString("slot");
		if (slotString == null) {
			MagicDebug.warn("No 'slot' specified %s.", MagicDebug.resolveFullPath());
			return null;
		}

		EquipmentSlotGroup group = switch (slotString.toLowerCase()) {
			case "main_hand", "hand" -> EquipmentSlotGroup.MAINHAND;
			case "off_hand" -> EquipmentSlotGroup.OFFHAND;
			case "*" -> EquipmentSlotGroup.ANY;
			case String s -> EquipmentSlotGroup.getByName(s);
		};

		if (group == null) {
			MagicDebug.warn("Invalid equipment slot group '%s' specified %s.", slotString, MagicDebug.resolveFullPath("slot"));
			return null;
		}

		String idString = config.getString("id");
		if (idString == null) {
			MagicDebug.warn("No 'id' specified %s.", MagicDebug.resolveFullPath());
			return null;
		}

		NamespacedKey id = NamespacedKey.fromString(idString, MagicSpells.getInstance());
		if (id == null) {
			MagicDebug.warn("Invalid namespaced key '%s' specified %s.", idString, MagicDebug.resolveFullPath("id"));
			return null;
		}

		return new AttributeData(attribute, new AttributeModifier(id, amount.doubleValue(), operation, group));
	}

	private record AttributeData(Attribute attribute, AttributeModifier modifier) {

	}

}