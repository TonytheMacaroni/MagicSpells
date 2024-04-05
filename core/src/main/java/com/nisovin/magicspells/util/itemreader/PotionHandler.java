package com.nisovin.magicspells.util.itemreader;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.util.BooleanUtils;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.*;

public class PotionHandler {

	public static final String POTION_EFFECT_CONFIG_NAME = POTION_EFFECTS.toString();
	public static final String POTION_TYPE_CONFIG_NAME = POTION_TYPE.toString();
	public static final String COLOR_CONFIG_NAME = COLOR.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		if (config.isList(POTION_EFFECT_CONFIG_NAME)) {
			potionMeta.clearCustomEffects();
			List<String> potionEffectStrings = config.getStringList(POTION_EFFECT_CONFIG_NAME);
			List<PotionEffect> potionEffects = new ArrayList<>();

			for (String potionEffect : potionEffectStrings) {
				PotionEffect eff = buildPotionEffect(potionEffect);
				if (eff == null) {
					MagicDebug.warn("Invalid potion effect string on magic item: '%s'.", potionEffect);
					continue;
				}

				potionMeta.addCustomEffect(eff, true);
				potionEffects.add(eff);
			}

			if (!potionEffects.isEmpty()) data.setAttribute(POTION_EFFECTS, potionEffects);
		}

		color:
		if (config.isString(COLOR_CONFIG_NAME) || config.isString("potion-color")) {
			String colorString = config.getString(COLOR_CONFIG_NAME, config.getString("potion-color", ""));

			Color color;
			try {
				int c = Integer.parseInt(colorString.replace("#", ""), 16);
				color = Color.fromRGB(c);
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid potion color on magic item: '%s'.", colorString);
				break color;
			}

			potionMeta.setColor(color);
			data.setAttribute(COLOR, color);
		}

		if (config.isString(POTION_TYPE_CONFIG_NAME) || config.isString("potion-data")) {
			String potionTypeString = config.getString(POTION_TYPE_CONFIG_NAME, config.getString("potion-data", ""));

			PotionType potionType = getPotionType(potionTypeString);
			if (potionType == null) {
				MagicDebug.warn("Invalid potion type on magic item: '%s'.", potionTypeString);
				return;
			}

			potionMeta.setBasePotionType(potionType);
			data.setAttribute(POTION_TYPE, potionType);
		}
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		if (data.hasAttribute(POTION_EFFECTS)) {
			potionMeta.clearCustomEffects();
			((List<PotionEffect>) data.getAttribute(POTION_EFFECTS)).forEach(potionEffect -> potionMeta.addCustomEffect(potionEffect, true));
		}
		if (data.hasAttribute(COLOR)) potionMeta.setColor((Color) data.getAttribute(COLOR));
		if (data.hasAttribute(POTION_TYPE)) potionMeta.setBasePotionType((PotionType) data.getAttribute(POTION_TYPE));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		data.setAttribute(POTION_TYPE, potionMeta.getBasePotionType());
		if (potionMeta.hasCustomEffects()) {
			List<PotionEffect> effects = potionMeta.getCustomEffects();
			if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);
		}
		if (potionMeta.hasColor()) data.setAttribute(COLOR, potionMeta.getColor());
	}

	public static PotionType getPotionType(ItemMeta meta) {
		return meta instanceof PotionMeta potionMeta ? potionMeta.getBasePotionType() : null;
	}

	public static PotionType getPotionType(String potionTypeString) {
		try {
			return PotionType.valueOf(potionTypeString.toUpperCase());
		} catch (IllegalArgumentException ignored) {
		}

		NamespacedKey key = NamespacedKey.fromString(potionTypeString.toLowerCase());
		if (key != null) {
			PotionType type = Registry.POTION.get(key);
			if (type != null) return type;
		}

		// Legacy support for potion data format

		String[] potionData = potionTypeString.split(" ", 2);
		if (potionData.length != 2) return null;

		String prefix;
		if (potionData[1].equalsIgnoreCase("extended")) prefix = "LONG_";
		else if (potionData[1].equalsIgnoreCase("upgraded")) prefix = "STRONG_";
		else return null;

		try {
			return PotionType.valueOf(prefix + potionData[0].toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	// - <potionEffectType> (level) (duration) (ambient)
	public static PotionEffect buildPotionEffect(String effectString) {
		String[] data = effectString.split(" ");

		PotionEffectType t = PotionEffectHandler.getPotionEffectType(data[0]);
		if (t == null) {
			MagicDebug.warn("Invalid potion effect type '%s' for potion effect '%s'.", data[0], effectString);
			return null;
		}

		int level = 0;
		if (data.length > 1) {
			try {
				level = Integer.parseInt(data[1]);
			} catch (NumberFormatException ex) {
				MagicDebug.warn("Invalid level '%s' for potion effect '%s'.", data[1], effectString);
				return null;
			}
		}

		int duration = 600;
		if (data.length > 2) {
			try {
				duration = Integer.parseInt(data[2]);
			} catch (NumberFormatException ex) {
				MagicDebug.warn("Invalid duration '%s' for potion effect '%s'.", data[2], effectString);
				return null;
			}
		}

		boolean ambient = data.length > 3 && (BooleanUtils.isYes(data[3]) || data[3].equalsIgnoreCase("ambient"));

		boolean particles = data.length > 4 && (BooleanUtils.isYes(data[4]) || data[4].equalsIgnoreCase("particles"));

		boolean icon = data.length > 5 && (BooleanUtils.isYes(data[5]) || data[5].equalsIgnoreCase("icon"));

		return new PotionEffect(t, duration, level, ambient, particles, icon);
	}

}
