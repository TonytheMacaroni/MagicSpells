package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.BooleanUtils;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.*;

public class PotionHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return true;

		if (config.isList(POTION_EFFECTS.getKey())) {
			List<PotionEffect> effects = new ArrayList<>();
			potionMeta.clearCustomEffects();

			List<String> effectStrings = config.getStringList(POTION_EFFECTS.getKey());
			for (String effectString : effectStrings) {
				PotionEffect effect = buildPotionEffect(effectString);
				if (effect == null) return false;

				potionMeta.addCustomEffect(effect, true);
				effects.add(effect);
			}

			if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);
		} else if (!invalidIfSet(config, POTION_EFFECTS.getKey())) return false;

		if (config.isString(COLOR.getKey()) || config.isString("potion-color")) {
			String colorString = config.getString(COLOR.getKey(), config.getString("potion-color", ""));

			Color color = ColorUtil.getColorFromHexString(colorString, false);
			if (color == null) {
				MagicDebug.warn("Invalid 'color' value '%s' %s.", colorString, MagicDebug.resolveFullPath());
				return false;
			}

			potionMeta.setColor(color);
			data.setAttribute(COLOR, color);
		} else if (!invalidIfSet(config, COLOR.getKey(), "potion-color")) return false;

		if (config.isString(POTION_TYPE.getKey()) || config.isString("potion-data")) {
			String potionTypeString = config.getString(POTION_TYPE.getKey(), config.getString("potion-data", ""));

			PotionType potionType = getPotionType(potionTypeString);
			if (potionType == null) {
				MagicDebug.warn("Invalid 'potion-type' value '%s' %s.", potionTypeString, MagicDebug.resolveFullPath());
				return false;
			}

			potionMeta.setBasePotionType(potionType);
			data.setAttribute(POTION_TYPE, potionType);
		} else if (!invalidIfSet(config, POTION_TYPE.getKey(), "potion-data")) return false;

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		if (data.hasAttribute(POTION_TYPE)) potionMeta.setBasePotionType(data.getAttribute(POTION_TYPE));
		if (data.hasAttribute(COLOR)) potionMeta.setColor(data.getAttribute(COLOR));

		if (data.hasAttribute(POTION_EFFECTS)) {
			potionMeta.clearCustomEffects();
			data.getAttribute(POTION_EFFECTS).forEach(potionEffect -> potionMeta.addCustomEffect(potionEffect, true));
		}
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof PotionMeta potionMeta)) return;

		if (potionMeta.hasBasePotionType()) data.setAttribute(POTION_TYPE, potionMeta.getBasePotionType());
		if (potionMeta.hasColor()) data.setAttribute(COLOR, potionMeta.getColor());

		if (potionMeta.hasCustomEffects()) {
			List<PotionEffect> effects = potionMeta.getCustomEffects();
			if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);
		}
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
			MagicDebug.warn("Invalid potion effect type '%s' for potion effect '%s' %s.", data[0], effectString, MagicDebug.resolveFullPath());
			return null;
		}

		int level = 0;
		if (data.length > 1) {
			try {
				level = Integer.parseInt(data[1]);
			} catch (NumberFormatException ex) {
				MagicDebug.warn("Invalid level '%s' for potion effect '%s' %s.", data[1], effectString, MagicDebug.resolveFullPath());
				return null;
			}
		}

		int duration = 600;
		if (data.length > 2) {
			try {
				duration = Integer.parseInt(data[2]);
			} catch (NumberFormatException ex) {
				MagicDebug.warn("Invalid duration '%s' for potion effect '%s' %s.", data[2], effectString, MagicDebug.resolveFullPath());
				return null;
			}
		}

		boolean ambient = data.length > 3 && (BooleanUtils.isYes(data[3]) || data[3].equalsIgnoreCase("ambient"));

		boolean particles = data.length > 4 && (BooleanUtils.isYes(data[4]) || data[4].equalsIgnoreCase("particles"));

		boolean icon = data.length > 5 && (BooleanUtils.isYes(data[5]) || data[5].equalsIgnoreCase("icon"));

		return new PotionEffect(t, duration, level, ambient, particles, icon);
	}

}
