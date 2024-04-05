package com.nisovin.magicspells.util.magicitems;

import java.util.*;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ItemUtil;
import com.nisovin.magicspells.util.itemreader.*;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.handlers.EnchantmentHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute;
import com.nisovin.magicspells.util.itemreader.alternative.AlternativeReaderManager;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.*;

public class MagicItems {

	private static final Map<String, MagicItem> magicItems = new HashMap<>();
	private static final Map<ItemStack, MagicItemData> itemStackCache = new HashMap<>();

	public static Map<String, MagicItem> getMagicItems() {
		return magicItems;
	}

	public static Collection<String> getMagicItemKeys() {
		return magicItems.keySet();
	}

	public static Collection<MagicItem> getMagicItemValues() {
		return magicItems.values();
	}

	public static MagicItem getMagicItemByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		return magicItems.get(internalName);
	}

	public static ItemStack getItemByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		if (magicItems.get(internalName).getItemStack() == null) return null;
		return magicItems.get(internalName).getItemStack().clone();
	}

	public static MagicItemData getMagicItemDataByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		return magicItems.get(internalName).getMagicItemData();
	}

	public static MagicItemData getMagicItemDataFromItemStack(ItemStack itemStack) {
		if (itemStack == null) return null;

		MagicItemData cached = itemStackCache.get(itemStack);
		// We can do this because itemStackCache doesn't have any null values
		if (cached != null) return cached;

		MagicItemData data = new MagicItemData();

		// type
		data.setAttribute(TYPE, itemStack.getType());

		if (itemStack.getType().isAir()) {
			itemStackCache.put(itemStack, data);
			return data;
		}

		// amount
		data.setAttribute(AMOUNT, itemStack.getAmount());

		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) {
			itemStackCache.put(itemStack, data);
			return data;
		}

		// name
		NameHandler.processMagicItemData(meta, data);

		// durability
		if (itemStack.getType().getMaxDurability() > 0) DurabilityHandler.processMagicItemData(meta, data);

		// repairCost
		RepairableHandler.processMagicItemData(meta, data);

		// customModelData
		CustomModelDataHandler.processMagicItemData(meta, data);

		// power, fireworkEffects
		FireworkHandler.processMagicItemData(meta, data);

		// unbreakable
		data.setAttribute(UNBREAKABLE, meta.isUnbreakable());

		// tooltip
		boolean tooltip = true;
		for (ItemFlag itemFlag : ItemFlag.values()) {
			if (!meta.getItemFlags().contains(itemFlag)) tooltip = false;
		}
		data.setAttribute(HIDE_TOOLTIP, tooltip);

		// color
		LeatherArmorHandler.processMagicItemData(meta, data);

		// potion, potionEffects, potionColor
		PotionHandler.processMagicItemData(meta, data);

		// suspiciousStew
		SuspiciousStewHandler.processMagicItemData(meta, data);

		// fireworkEffect
		FireworkEffectHandler.processMagicItemData(meta, data);

		// skullOwner
		SkullHandler.processMagicItemData(meta, data);

		// author, title, pages
		WrittenBookHandler.processMagicItemData(meta, data);

		// enchantments
		Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());
		if (ItemUtil.hasFakeEnchantment(meta)) {
			enchants.remove(Enchantment.FROST_WALKER);

			data.setAttribute(FAKE_GLINT, true);
		}
		if (!enchants.isEmpty()) data.setAttribute(ENCHANTS, enchants);

		// attributes
		AttributeHandler.processMagicItemData(meta, data);

		// lore
		if (meta.hasLore()) {
			List<Component> lore = meta.lore();
			if (lore != null && !lore.isEmpty()) data.setAttribute(LORE, lore);
		}

		// patterns
		BannerHandler.processMagicItemData(meta, data);

		// block data
		BlockDataHandler.processMagicItemData(meta, data, itemStack.getType());

		itemStackCache.put(itemStack, data);
		return data;
	}

	public static MagicItemData getMagicItemDataFromString(String str) {
		if (str == null) return null;
		if (magicItems.containsKey(str)) return magicItems.get(str).getMagicItemData();

		return MagicItemDataParser.parseMagicItemData(str);
	}

	public static MagicItem getMagicItemFromString(String str) {
		if (str == null) return null;
		if (magicItems.containsKey(str)) return magicItems.get(str);

		MagicItem magicItem;
		MagicItemData itemData = MagicItemDataParser.parseMagicItemData(str);
		if (itemData == null) return null;

		magicItem = getMagicItemFromData(itemData);
		return magicItem;
	}

	public static MagicItem getMagicItemFromData(MagicItemData data) {
		if (data == null) return null;

		Material type = (Material) data.getAttribute(TYPE);
		if (type == null) return null;

		ItemStack item = new ItemStack(type);

		if (type.isAir()) return new MagicItem(item, data);

		if (data.hasAttribute(AMOUNT)) {
			int amount = (int) data.getAttribute(AMOUNT);
			if (amount >= 1) item.setAmount(amount);
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return new MagicItem(item, data);

		// Name
		NameHandler.processItemMeta(meta, data);

		// Lore
		LoreHandler.processItemMeta(meta, data);

		// Custom Model Data
		CustomModelDataHandler.processItemMeta(meta, data);

		// Enchantments
		if (data.hasAttribute(ENCHANTS)) {
			Map<Enchantment, Integer> enchantments = (Map<Enchantment, Integer>) data.getAttribute(ENCHANTS);
			for (Enchantment enchant : enchantments.keySet()) {
				int level = enchantments.get(enchant);

				if (meta instanceof EnchantmentStorageMeta)
					((EnchantmentStorageMeta) meta).addStoredEnchant(enchant, level, true);
				else meta.addEnchant(enchant, level, true);
			}
		}

		if (data.hasAttribute(FAKE_GLINT)) {
			boolean fakeGlint = (boolean) data.getAttribute(FAKE_GLINT);

			if (fakeGlint && !meta.hasEnchants()) {
				ItemUtil.addFakeEnchantment(meta);
			}
		}

		// Armor color
		LeatherArmorHandler.processItemMeta(meta, data);

		// Potion effects and potion color
		PotionHandler.processItemMeta(meta, data);

		// Skull owner
		SkullHandler.processItemMeta(meta, data);

		// Durability
		if (type.getMaxDurability() > 0) DurabilityHandler.processItemMeta(meta, data);

		// Repair cost
		RepairableHandler.processItemMeta(meta, data);

		// Written book
		WrittenBookHandler.processItemMeta(meta, data);

		// Banner
		BannerHandler.processItemMeta(meta, data);

		// Firework Star
		FireworkEffectHandler.processItemMeta(meta, data);

		// Firework
		FireworkHandler.processItemMeta(meta, data);

		// Suspicious Stew
		SuspiciousStewHandler.processItemMeta(meta, data);

		// Block Data
		BlockDataHandler.processItemMeta(meta, data);

		// Attributes
		AttributeHandler.processItemMeta(meta, data);

		// Unbreakable
		if (data.hasAttribute(UNBREAKABLE))
			meta.setUnbreakable((boolean) data.getAttribute(UNBREAKABLE));

		// Hide tooltip
		if (data.hasAttribute(HIDE_TOOLTIP) && (boolean) data.getAttribute(HIDE_TOOLTIP))
			meta.addItemFlags(ItemFlag.values());

		// Set meta
		item.setItemMeta(meta);

		return new MagicItem(item, data);
	}

	public static MagicItem getMagicItemFromSection(ConfigurationSection section) {
		try (var context = MagicDebug.section(DebugCategory.MAGIC_ITEMS, "Parsing section-based magic item.")) {
			if (!section.contains("type")) {
				MagicDebug.warn("Invalid magic item - no 'type' key present.");
				return null;
			}

			ItemStack item = AlternativeReaderManager.deserialize(section);
			if (item != null) {
				MagicItemData data = getMagicItemDataFromItemStack(item);
				MagicItem magicItem = new MagicItem(item, data);

				if (section.isList("ignored-attributes")) {
					Set<MagicItemAttribute> ignoredAttributes = data.getIgnoredAttributes();
					List<String> ignoredAttributeStrings = section.getStringList("ignored-attributes");

					for (String attr : ignoredAttributeStrings) {
						String attrValue = attr.toUpperCase().replace("-", "_");

						try {
							ignoredAttributes.add(MagicItemAttribute.valueOf(attrValue));
						} catch (IllegalArgumentException e) {
							switch (attrValue) {
								case "ENCHANTMENTS" -> ignoredAttributes.add(ENCHANTS);
								case "POTION_DATA" -> ignoredAttributes.add(POTION_TYPE);
								default -> MagicDebug.warn("Invalid ignored attribute '%s'.", attr);
							}
						}
					}
				}

				if (section.isList("blacklisted-attributes")) {
					Set<MagicItemAttribute> blacklistedAttributes = data.getBlacklistedAttributes();
					List<String> blacklistedAttributeStrings = section.getStringList("blacklisted-attributes");

					for (String attr : blacklistedAttributeStrings) {
						String attrValue = attr.toUpperCase().replace("-", "_");

						try {
							blacklistedAttributes.add(MagicItemAttribute.valueOf(attrValue));
						} catch (IllegalArgumentException e) {
							switch (attrValue) {
								case "ENCHANTMENTS" -> blacklistedAttributes.add(ENCHANTS);
								case "POTION_DATA" -> blacklistedAttributes.add(POTION_TYPE);
								default -> MagicDebug.warn("Invalid blacklisted attribute '%s'.", attr);
							}
						}
					}
				}

				if (section.isBoolean("strict-enchants"))
					data.setStrictEnchants(section.getBoolean("strict-enchants"));

				if (section.isBoolean("strict-block-data"))
					data.setStrictBlockData(section.getBoolean("strict-block-data"));

				if (section.isBoolean("strict-durability"))
					data.setStrictDurability(section.getBoolean("strict-durability"));

				if (section.isBoolean("strict-enchant-level"))
					data.setStrictEnchantLevel(section.getBoolean("strict-enchant-level"));

				return magicItem;
			}

			MagicItemData data;

			String typeString = section.getString("type");
			Material type = Util.getMaterial(typeString);
			if (type != null) {
				if (!type.isItem()) {
					MagicDebug.warn("Invalid type for magic item - '%s' is not an item type.", typeString);
					return null;
				}

				item = new ItemStack(type);
				data = new MagicItemData();
				data.setAttribute(TYPE, type);
			} else {
				MagicItem magicItem = MagicItems.getMagicItems().get(typeString);
				if (magicItem == null) {
					MagicDebug.warn("Invalid magic item 'type' - '%s' is not a valid item type, magic item or serializer key.", typeString);
					return null;
				}

				item = magicItem.getItemStack().clone();
				type = item.getType();

				data = magicItem.getMagicItemData().clone();
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null) return new MagicItem(item, data);

			NameHandler.process(section, meta, data);

			// Lore
			LoreHandler.process(section, meta, data);

			// CustomModelData
			CustomModelDataHandler.process(section, meta, data);

			// Enchants
			// <enchantmentName> <level>
			if (section.isList("enchants")) {
				List<String> enchantStrings = section.getStringList("enchants");
				for (String enchantString : enchantStrings) {
					String[] enchantData = enchantString.split(" ");
					if (enchantData.length > 2) {
						MagicDebug.warn("Invalid enchantment string '%s' - too many arguments.", enchantString);
						return null;
					}

					Enchantment enchant = EnchantmentHandler.getEnchantment(enchantData[0]);
					if (enchant == null) {
						MagicDebug.warn("Invalid enchantment '%s'.", enchantData[0]);
						return null;
					}

					int level = 0;
					if (enchantData.length > 1) {
						try {
							level = Integer.parseInt(enchantData[1]);
						} catch (IllegalArgumentException e) {
							MagicDebug.warn("Invalid enchantment level '%s'.", enchantData[1]);
							return null;
						}
					}

					if (meta instanceof EnchantmentStorageMeta storageMeta)
						storageMeta.addStoredEnchant(enchant, level, true);
					else
						meta.addEnchant(enchant, level, true);
				}

				if (meta instanceof EnchantmentStorageMeta storageMeta) {
					if (storageMeta.hasStoredEnchants()) {
						Map<Enchantment, Integer> enchantments = storageMeta.getStoredEnchants();
						if (!enchantments.isEmpty()) data.setAttribute(ENCHANTS, enchantments);
					}
				} else if (meta.hasEnchants()) {
					Map<Enchantment, Integer> enchantments = meta.getEnchants();
					if (!enchantments.isEmpty()) data.setAttribute(ENCHANTS, enchantments);
				}
			}

			if (section.isBoolean("fake-glint")) {
				boolean fakeGlint = section.getBoolean("fake-glint");

				if (fakeGlint && !meta.hasEnchants()) {
					ItemUtil.addFakeEnchantment(meta);
					data.setAttribute(FAKE_GLINT, true);
				}
			}

			// Armor color
			LeatherArmorHandler.process(section, meta, data);

			// Potion effects, color, type
			PotionHandler.process(section, meta, data);

			// Skull owner
			SkullHandler.process(section, meta, data);

			// Durability
			if (type.getMaxDurability() > 0) DurabilityHandler.process(section, meta, data);

			// Repair cost
			RepairableHandler.process(section, meta, data);

			// Written book
			WrittenBookHandler.process(section, meta, data);

			// Banner
			BannerHandler.process(section, meta, data);

			// Firework Star
			FireworkEffectHandler.process(section, meta, data);

			// Firework
			FireworkHandler.process(section, meta, data);

			// Suspicious Stew
			SuspiciousStewHandler.process(section, meta, data);

			// Block Data
			BlockDataHandler.process(section, meta, data, type);

			// Attributes
			AttributeHandler.process(section, meta, data);

			// Unbreakable
			if (section.isBoolean("unbreakable")) {
				boolean unbreakable = section.getBoolean("unbreakable");

				meta.setUnbreakable(unbreakable);
				data.setAttribute(UNBREAKABLE, unbreakable);
			}

			if (MagicSpells.hideMagicItemTooltips()) {
				meta.addItemFlags(ItemFlag.values());
				data.setAttribute(HIDE_TOOLTIP, true);
			} else if (section.isBoolean("hide-tooltip")) {
				boolean hideTooltip = section.getBoolean("hide-tooltip");

				if (hideTooltip) meta.addItemFlags(ItemFlag.values());
				data.setAttribute(HIDE_TOOLTIP, hideTooltip);
			}

			// Attributes
			AttributeHandler.process(section, meta, data);

			// Set meta
			item.setItemMeta(meta);

			if (section.isList("ignored-attributes")) {
				List<String> ignoredAttributeStrings = section.getStringList("ignored-attributes");
				Set<MagicItemAttribute> ignoredAttributes = data.getIgnoredAttributes();

				for (String attr : ignoredAttributeStrings) {
					String attrValue = attr.toUpperCase().replace("-", "_");

					try {
						ignoredAttributes.add(MagicItemAttribute.valueOf(attrValue));
					} catch (IllegalArgumentException e) {
						switch (attrValue) {
							case "ENCHANTMENTS" -> ignoredAttributes.add(ENCHANTS);
							case "POTION_DATA" -> ignoredAttributes.add(POTION_TYPE);
							default -> DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
						}
					}
				}
			}

			if (section.isList("blacklisted-attributes")) {
				List<String> blacklistedAttributeStrings = section.getStringList("blacklisted-attributes");
				Set<MagicItemAttribute> blacklistedAttributes = data.getBlacklistedAttributes();

				for (String attr : blacklistedAttributeStrings) {
					String attrValue = attr.toUpperCase().replace("-", "_");

					try {
						blacklistedAttributes.add(MagicItemAttribute.valueOf(attrValue));
					} catch (IllegalArgumentException e) {
						switch (attrValue) {
							case "ENCHANTMENTS" -> blacklistedAttributes.add(ENCHANTS);
							case "POTION_DATA" -> blacklistedAttributes.add(POTION_TYPE);
							default -> DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
						}
					}
				}
			}

			if (section.isBoolean("strict-enchants"))
				data.setStrictEnchants(section.getBoolean("strict-enchants"));

			if (section.isBoolean("strict-block-data"))
				data.setStrictBlockData(section.getBoolean("strict-block-data"));

			if (section.isBoolean("strict-durability"))
				data.setStrictDurability(section.getBoolean("strict-durability"));

			if (section.isBoolean("strict-enchant-level"))
				data.setStrictEnchantLevel(section.getBoolean("strict-enchant-level"));

			return new MagicItem(item, data);
		}
	}

}
