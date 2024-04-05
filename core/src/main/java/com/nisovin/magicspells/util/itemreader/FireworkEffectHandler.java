package com.nisovin.magicspells.util.itemreader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.FIREWORK_EFFECT;

public class FireworkEffectHandler {

	private static final String TYPE_CONFIG_NAME = "firework-type";
	private static final String TRAIL_CONFIG_NAME = "trail";
	private static final String FLICKER_CONFIG_NAME = "flicker";
	private static final String COLORS_CONFIG_NAME = "colors";
	private static final String FADE_COLORS_CONFIG_NAME = "fade-colors";

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof FireworkEffectMeta fireworkMeta)) return;

		String type = config.getString(TYPE_CONFIG_NAME, "BALL").trim().toUpperCase();
		boolean trail = config.getBoolean(TRAIL_CONFIG_NAME, false);
		boolean flicker = config.getBoolean(FLICKER_CONFIG_NAME, false);

		String colorString = config.getString(COLORS_CONFIG_NAME);
		List<Color> colors = getColorsFromString(colorString);
		List<Color> fadeColors = getColorsFromString(config.getString(FADE_COLORS_CONFIG_NAME));

		if (colors.isEmpty()) {
			if (colorString != null)
				MagicDebug.warn("No valid firework effect colors specified on magic item.", colorString);

			return;
		}

		FireworkEffect.Type fireworkType;
		try {
			fireworkType = FireworkEffect.Type.valueOf(type);
		} catch (IllegalArgumentException e) {
			MagicDebug.warn("Invalid firework type '%s' on magic item.", type);
			return;
		}

		FireworkEffect effect = FireworkEffect.builder()
				.flicker(flicker)
				.trail(trail)
				.with(fireworkType)
				.withColor(colors)
				.withFade(fadeColors)
				.build();

		fireworkMeta.setEffect(effect);
		data.setAttribute(FIREWORK_EFFECT, effect);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof FireworkEffectMeta fireworkMeta)) return;
		if (!data.hasAttribute(FIREWORK_EFFECT)) return;
		fireworkMeta.setEffect((FireworkEffect) data.getAttribute(FIREWORK_EFFECT));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof FireworkEffectMeta effectMeta)) return;
		if (!effectMeta.hasEffect()) return;
		data.setAttribute(FIREWORK_EFFECT, effectMeta.getEffect());
	}

	public static List<Color> getColorsFromString(String input) {
		if (input == null || input.isEmpty()) return List.of();

		List<Color> colors = new ArrayList<>();

		String[] colorStrings = input.split(",");
		for (String colorString : colorStrings) {
			try {
				int c = Integer.parseInt(colorString.trim(), 16);
				colors.add(Color.fromRGB(c));
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid firework effect color '%s' on magic item.", colorString);
			}
		}

		return colors;
	}

}
