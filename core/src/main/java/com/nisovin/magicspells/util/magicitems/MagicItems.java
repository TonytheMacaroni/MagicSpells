package com.nisovin.magicspells.util.magicitems;

import java.util.*;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ItemUtil;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.itemreader.*;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.handlers.EnchantmentHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute;
import com.nisovin.magicspells.util.itemreader.alternative.AlternativeReaderManager;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.*;

public class MagicItems {

	private static final Map<String, MagicItem> magicItems = new HashMap<>();
	private static final Map<ItemStack, MagicItemData> itemStackCache = new HashMap<>();
	private static final List<ItemHandler> handlers = new ArrayList<>();

	static {
		handlers.add(new AttributeHandler());
		handlers.add(new BannerHandler());
		handlers.add(new BlockDataHandler());
		handlers.add(new CustomModelDataHandler());
		handlers.add(new DurabilityHandler());
		handlers.add(new FireworkEffectHandler());
		handlers.add(new FireworkHandler());
		handlers.add(new LeatherArmorHandler());
		handlers.add(new LoreHandler());
		handlers.add(new NameHandler());
		handlers.add(new PotionHandler());
		handlers.add(new RepairableHandler());
		handlers.add(new SkullHandler());
		handlers.add(new SuspiciousStewHandler());
		handlers.add(new WrittenBookHandler());
	}

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

	public static MagicItemData getMagicItemDataFromItemStack(ItemStack item) {
		if (item == null) return null;

		MagicItemData cached = itemStackCache.get(item);
		// We can do this because itemStackCache doesn't have any null values
		if (cached != null) return cached;

		MagicItemData data = new MagicItemData();

		// type
		data.setAttribute(TYPE, item.getType());

		if (item.getType().isAir()) {
			itemStackCache.put(item, data);
			return data;
		}

		// amount
		data.setAttribute(AMOUNT, item.getAmount());

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			itemStackCache.put(item, data);
			return data;
		}

		// tooltip
		boolean tooltip = true;
		for (ItemFlag itemFlag : ItemFlag.values())
			if (!meta.getItemFlags().contains(itemFlag))
				tooltip = false;

		data.setAttribute(HIDE_TOOLTIP, tooltip);

		// enchantments
		Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());
		if (ItemUtil.hasFakeEnchantment(meta)) {
			enchants.remove(Enchantment.FROST_WALKER);

			data.setAttribute(FAKE_GLINT, true);
		}
		if (!enchants.isEmpty()) data.setAttribute(ENCHANTS, enchants);

		// Handlers
		for (ItemHandler handler : handlers)
			handler.processMagicItemData(item, meta, data);

		itemStackCache.put(item, data);
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

		MagicItemData itemData = MagicItemDataParser.parseMagicItemData(str);
		if (itemData == null) return null;

		return getMagicItemFromData(itemData);
	}

	public static MagicItem getMagicItemFromData(MagicItemData data) {
		if (data == null) return null;

		Material type = data.getAttribute(TYPE);
		if (type == null) return null;

		ItemStack item = new ItemStack(type);

		if (type.isAir()) return new MagicItem(item, data);

		if (data.hasAttribute(AMOUNT)) {
			int amount = data.getAttribute(AMOUNT);
			if (amount >= 1) item.setAmount(amount);
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return new MagicItem(item, data);

		// Enchantments
		if (data.hasAttribute(ENCHANTS)) {
			Map<Enchantment, Integer> enchantments = data.getAttribute(ENCHANTS);
			for (Enchantment enchant : enchantments.keySet()) {
				int level = enchantments.get(enchant);

				if (meta instanceof EnchantmentStorageMeta storage) storage.addStoredEnchant(enchant, level, true);
				else meta.addEnchant(enchant, level, true);
			}
		}

		if (data.hasAttribute(FAKE_GLINT)) {
			boolean fakeGlint = data.getAttribute(FAKE_GLINT);

			if (fakeGlint && !meta.hasEnchants()) {
				ItemUtil.addFakeEnchantment(meta);
			}
		}

		// Unbreakable
		if (data.hasAttribute(UNBREAKABLE))
			meta.setUnbreakable(data.getAttribute(UNBREAKABLE));

		// Hide tooltip
		if (data.hasAttribute(HIDE_TOOLTIP)) {
			if (data.getAttribute(HIDE_TOOLTIP)) meta.addItemFlags(ItemFlag.values());
			else meta.removeItemFlags(ItemFlag.values());
		}

		// Handlers
		for (ItemHandler handler : handlers)
			handler.processItemMeta(item, meta, data);

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

				getMatchSettings(section, data);

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
				data = magicItem.getMagicItemData().clone();
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null) return new MagicItem(item, data);

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

			for (ItemHandler handler : handlers)
				if (!handler.process(section, item, meta, data))
					return null;

			// Set meta
			item.setItemMeta(meta);

			getMatchSettings(section, data);

			return new MagicItem(item, data);
		}
	}

	private static void getMatchSettings(ConfigurationSection section, MagicItemData data) {
		if (section.isList("ignored-attributes")) {
			Set<MagicItemAttribute<?>> ignoredAttributes = data.getIgnoredAttributes();

			List<String> attributeStrings = section.getStringList("ignored-attributes");
			for (String attributeString : attributeStrings) {
				MagicItemAttribute<?> attribute = MagicItemAttribute.fromString(attributeString);
				if (attribute == null) {
					MagicDebug.warn("Invalid ignored attribute '%s'.", attributeString);
					continue;
				}

				ignoredAttributes.add(attribute);
			}
		}

		if (section.isList("blacklisted-attributes")) {
			Set<MagicItemAttribute<?>> blacklistedAttributes = data.getBlacklistedAttributes();

			List<String> attributeStrings = section.getStringList("blacklisted-attributes");
			for (String attributeString : attributeStrings) {
				MagicItemAttribute<?> attribute = MagicItemAttribute.fromString(attributeString);
				if (attribute == null) {
					MagicDebug.warn("Invalid blacklisted attribute '%s'.", attributeString);
					continue;
				}

				blacklistedAttributes.add(attribute);
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
	}

}
