package com.nisovin.magicspells.util.itemreader;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.POWER;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.FIREWORK_EFFECTS;

public class FireworkHandler {

	private static final String FIREWORK_EFFECTS_CONFIG_NAME = FIREWORK_EFFECTS.toString();
	private static final String POWER_CONFIG_NAME = POWER.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof FireworkMeta fireworkMeta)) return;

		int power = config.getInt(POWER_CONFIG_NAME, 0);
		fireworkMeta.setPower(power);
		data.setAttribute(POWER, power);

		if (config.isList(FIREWORK_EFFECTS_CONFIG_NAME)) {
			List<String> effectStrings = config.getStringList(FIREWORK_EFFECTS_CONFIG_NAME);

			List<FireworkEffect> fireworkEffects = new ArrayList<>();

			// <type> <trail> <flicker> <colors>(,) <fadeColors>(,)
			for (String effectString : effectStrings) {
				String[] values = effectString.split(" ");
				if (values.length != 4 && values.length != 5) {
					MagicDebug.warn("Invalid firework effect '%s' on magic item - missing or too many values.", effectString);
					continue;
				}

				FireworkEffect.Type fireworkType;
				try {
					fireworkType = FireworkEffect.Type.valueOf(values[0].toUpperCase());
				} catch (IllegalArgumentException e) {
					MagicDebug.warn("Invalid firework effect type '%s' on magic item.", values[0]);
					continue;
				}
				boolean trail = Boolean.parseBoolean(values[1]);
				boolean flicker = Boolean.parseBoolean(values[2]);
				List<Color> colors = FireworkEffectHandler.getColorsFromString(values[3]);
				List<Color> fadeColors = values.length > 4 ? FireworkEffectHandler.getColorsFromString(values[4]) : List.of();

				if (colors.isEmpty()) continue;

				FireworkEffect effect = FireworkEffect.builder()
						.flicker(flicker)
						.trail(trail)
						.with(fireworkType)
						.withColor(colors)
						.withFade(fadeColors)
						.build();

				fireworkEffects.add(effect);
			}

			if (!fireworkEffects.isEmpty()) {
				fireworkMeta.addEffects(fireworkEffects);
				data.setAttribute(FIREWORK_EFFECTS, fireworkEffects);
			}
		}
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof FireworkMeta fireworkMeta)) return;

		if (data.hasAttribute(POWER)) fireworkMeta.setPower((int) data.getAttribute(POWER));
		if (data.hasAttribute(FIREWORK_EFFECTS)) fireworkMeta.addEffects((List<FireworkEffect>) data.getAttribute(FIREWORK_EFFECTS));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof FireworkMeta fireworkMeta)) return;
		data.setAttribute(POWER, fireworkMeta.getPower());
		if (!fireworkMeta.hasEffects()) return;
		List<FireworkEffect> effects = fireworkMeta.getEffects();
		if (!effects.isEmpty()) data.setAttribute(FIREWORK_EFFECTS, fireworkMeta.getEffects());
	}

}
