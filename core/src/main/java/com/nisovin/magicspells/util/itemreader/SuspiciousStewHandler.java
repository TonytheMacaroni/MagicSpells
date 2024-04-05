package com.nisovin.magicspells.util.itemreader;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.POTION_EFFECTS;

public class SuspiciousStewHandler {

	private static final String CONFIG_NAME = POTION_EFFECTS.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return;
		if (!config.isList(CONFIG_NAME)) return;

		List<String> effects = config.getStringList(CONFIG_NAME);
		List<PotionEffect> potionEffects = new ArrayList<>();
		for (String str : effects) {
			PotionEffect potionEffect = buildSuspiciousStewPotionEffect(str);
			if (potionEffect == null) continue;

			stewMeta.addCustomEffect(potionEffect, true);
			potionEffects.add(potionEffect);
		}

		if (!potionEffects.isEmpty()) data.setAttribute(POTION_EFFECTS, potionEffects);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return;
		if (!data.hasAttribute(POTION_EFFECTS)) return;
		stewMeta.clearCustomEffects();
		((List<PotionEffect>) data.getAttribute(POTION_EFFECTS)).forEach(potionEffect -> stewMeta.addCustomEffect(potionEffect, true));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return;
		if (!stewMeta.hasCustomEffects()) return;
		List<PotionEffect> effects = stewMeta.getCustomEffects();
		if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);
	}

	// - <potionEffectType> (duration)
	public static PotionEffect buildSuspiciousStewPotionEffect(String effectString) {
		String[] data = effectString.split(" ");
		if (data.length > 2) {
			MagicDebug.warn("Invalid suspicious stew potion effect '%s' - too many arguments.", effectString);
			return null;
		}

		PotionEffectType type = PotionEffectHandler.getPotionEffectType(data[0]);
		if (type == null) {
			MagicDebug.warn("Invalid potion type '%s' on suspicious stew potion effect on magic item.", data[0]);
			return null;
		}

		int duration = 600;
		if (data.length > 1) {
			try {
				duration = Integer.parseInt(data[1]);
			} catch (NumberFormatException ex) {
				MagicDebug.warn("Invalid duration '%s' on suspicious stew potion effect on magic item.", data[1]);
				return null;
			}
		}
		return new PotionEffect(type, duration, 0, true);
	}

}
