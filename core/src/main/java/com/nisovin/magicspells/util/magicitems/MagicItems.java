package com.nisovin.magicspells.util.magicitems;

import java.util.*;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.itemreader.*;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.handlers.EnchantmentHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute;
import com.nisovin.magicspells.util.itemreader.alternative.AlternativeReaderManager;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.*;

public class MagicItems {

	private static final Map<String, MagicItem> magicItems = new HashMap<>();
	private static final List<ItemHandler> handlers = new ArrayList<>();

	private static final Cache<ItemStack, MagicItemData> itemStackCache = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofMinutes(5))
		.maximumSize(10000)
		.build();

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

		MagicItemData data = itemStackCache.getIfPresent(item);
		if (data != null) return data;

		data = getMagicItemDataFromItemStackInternal(item);
		itemStackCache.put(item.clone(), data);

		return data;
	}

	private static MagicItemData getMagicItemDataFromItemStackInternal(ItemStack item) {
		MagicItemData data = new MagicItemData();

		// type
		data.setAttribute(TYPE, item.getType());
		if (item.getType().isAir()) return data;

		// amount
		data.setAttribute(AMOUNT, item.getAmount());

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return data;

		// tooltip
		boolean tooltip = true;
		for (ItemFlag itemFlag : ItemFlag.values())
			if (!meta.getItemFlags().contains(itemFlag))
				tooltip = false;

		data.setAttribute(HIDE_TOOLTIP, tooltip);

		// Handlers
		for (ItemHandler handler : handlers)
			handler.processMagicItemData(item, meta, data);

		return data;
	}

	// TODO: Replace usages with some method in ConfigReaderUtil that properly supplies debug path.
	public static MagicItemData getMagicItemDataFromString(String str) {
		if (str == null) return null;
		if (magicItems.containsKey(str)) return magicItems.get(str).getMagicItemData();

		return MagicItemDataParser.parseMagicItemData(str);
	}

	// TODO: Replace usages with some method in ConfigReaderUtil that properly supplies debug path.
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
		try (var _ = MagicDebug.section(DebugCategory.MAGIC_ITEMS, "Parsing section-based magic item.")) {
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
