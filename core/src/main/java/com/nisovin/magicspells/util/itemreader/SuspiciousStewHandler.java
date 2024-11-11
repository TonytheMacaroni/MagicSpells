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
import io.papermc.paper.potion.SuspiciousEffectEntry;

import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.STEW_EFFECTS;

public class SuspiciousStewHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return true;

		String key = STEW_EFFECTS.getKey();
		if (!config.isList(key) && !config.isList("potion-effects"))
			return invalidIfSet(config, key);

		stewMeta.clearCustomEffects();

		List<?> effectStrings = config.getList(key);
		if (effectStrings == null) {
			effectStrings = config.getList("potion-effects");
			key = "potion-effects";
		}

		List<SuspiciousEffectEntry> effects = getSuspiciousStewEffects(effectStrings, key);
		if (effects == null) return false;

		if (!effects.isEmpty()) {
			for (SuspiciousEffectEntry effect : effects)
				stewMeta.addCustomEffect(effect, false);

			data.setAttribute(STEW_EFFECTS, effects);
		}

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta) || !data.hasAttribute(STEW_EFFECTS)) return;

		stewMeta.clearCustomEffects();
		data.getAttribute(STEW_EFFECTS).forEach(potionEffect -> stewMeta.addCustomEffect(potionEffect, false));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta) || !stewMeta.hasCustomEffects()) return;

		List<PotionEffect> effects = stewMeta.getCustomEffects();
		if (!effects.isEmpty()) {
			List<SuspiciousEffectEntry> entries = effects.stream().map(e -> SuspiciousEffectEntry.create(e.getType(), e.getDuration())).toList();
			data.setAttribute(STEW_EFFECTS, entries);
		}
	}

	public static List<SuspiciousEffectEntry> getSuspiciousStewEffects(@NotNull List<?> data, @NotNull String name) {
		try (var ignored = MagicDebug.section("Resolving suspicious stew effects from '%s'.", name)
			.pushPath(name, DebugPath.Type.LIST)
		) {
			List<SuspiciousEffectEntry> stewEffects = new ArrayList<>();

			for (int i = 0; i < data.size(); i++) {
				try (var ignored1 = MagicDebug.pushListEntry(i)) {
					Object object = data.get(i);
					if (!(object instanceof String effectString)) {
						MagicDebug.warn("Invalid suspicious stew effect '%s' %s.", object, MagicDebug.resolveFullPath());
						return null;
					}

					SuspiciousEffectEntry entry = getSuspiciousStewEffect(effectString);
					if (entry == null) return null;

					stewEffects.add(entry);
				}
			}

			return stewEffects;
		}
	}

	public static SuspiciousEffectEntry getSuspiciousStewEffect(String effectString) {
		try (var ignored = MagicDebug.section("Resolving suspicious stew effect '%s'.", effectString)) {
			String[] data = effectString.split(" ");
			if (data.length > 2) {
				MagicDebug.warn("Invalid suspicious stew effect '%s' - too many arguments.", effectString);
				return null;
			}

			PotionEffectType type = PotionEffectHandler.getPotionEffectType(data[0]);
			if (type == null) {
				MagicDebug.warn("Invalid effect type '%s' for suspicious stew effect '%s' %s.", data[0], effectString, MagicDebug.resolveFullPath());
				return null;
			}

			int duration = 600;
			if (data.length > 1) {
				try {
					duration = Integer.parseInt(data[1]);
				} catch (NumberFormatException ex) {
					MagicDebug.warn("Invalid duration '%s' for suspicious stew effect '%s' %s.", data[1], effectString, MagicDebug.resolveFullPath());
					return null;
				}
			}

			return SuspiciousEffectEntry.create(type, duration);
		}
	}

}
