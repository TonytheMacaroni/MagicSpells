package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.POTION_EFFECTS;

public class SuspiciousStewHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return true;

		if (!config.isList(POTION_EFFECTS.getKey())) return invalidIfSet(config, POTION_EFFECTS);

		List<PotionEffect> effects = new ArrayList<>();
		stewMeta.clearCustomEffects();

		List<String> effectStrings = config.getStringList(POTION_EFFECTS.getKey());
		for (String effectString : effectStrings) {
			PotionEffect effect = buildSuspiciousStewPotionEffect(effectString);
			if (effect == null) return false;

			stewMeta.addCustomEffect(effect, true);
			effects.add(effect);
		}

		if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta) || !data.hasAttribute(POTION_EFFECTS)) return;

		stewMeta.clearCustomEffects();
		data.getAttribute(POTION_EFFECTS).forEach(potionEffect -> stewMeta.addCustomEffect(potionEffect, true));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta) || !stewMeta.hasCustomEffects()) return;

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
			MagicDebug.warn("Invalid potion type '%s' for potion effect '%s' %s.", data[0], effectString, MagicDebug.resolveFullPath());
			return null;
		}

		int duration = 600;
		if (data.length > 1) {
			try {
				duration = Integer.parseInt(data[1]);
			} catch (NumberFormatException ex) {
				MagicDebug.warn("Invalid duration '%s' for potion effect '%s' %s.", data[1], effectString, MagicDebug.resolveFullPath());
				return null;
			}
		}

		return new PotionEffect(type, duration, 0, true);
	}

}
