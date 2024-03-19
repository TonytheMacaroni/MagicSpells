package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.FIREWORK_EFFECT;

public class FireworkEffectHandler extends ItemHandler {

	private static final String TYPE_CONFIG_NAME = "firework-type";
	private static final String TRAIL_CONFIG_NAME = "trail";
	private static final String FLICKER_CONFIG_NAME = "flicker";
	private static final String COLORS_CONFIG_NAME = "colors";
	private static final String FADE_COLORS_CONFIG_NAME = "fade-colors";

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof FireworkEffectMeta fireworkMeta)) return true;

		if (!config.isString(COLORS_CONFIG_NAME)) return invalidIfSet(config, COLORS_CONFIG_NAME);

		if (!config.isString(FADE_COLORS_CONFIG_NAME) && !invalidIfSet(config, FADE_COLORS_CONFIG_NAME))
			return false;

		if (!config.isString(TYPE_CONFIG_NAME) && !invalidIfSet(config, TYPE_CONFIG_NAME))
			return false;

		if (!config.isBoolean(TRAIL_CONFIG_NAME) && !invalidIfSet(config, TRAIL_CONFIG_NAME))
			return false;

		if (!config.isBoolean(FLICKER_CONFIG_NAME) && !invalidIfSet(config, FADE_COLORS_CONFIG_NAME))
			return false;

		List<Color> colors = getColorsFromString(config.getString(COLORS_CONFIG_NAME), "'colors'", true);
		if (colors == null) return false;

		List<Color> fadeColors = getColorsFromString(config.getString(FADE_COLORS_CONFIG_NAME), "'fade-colors'", false);
		if (fadeColors == null) return false;

		String typeString = config.getString(TYPE_CONFIG_NAME, "BALL");

		FireworkEffect.Type type;
		try {
			type = FireworkEffect.Type.valueOf(typeString);
		} catch (IllegalArgumentException e) {
			MagicDebug.warn("Invalid 'firework-type' value '%s' %s.", typeString, MagicDebug.resolvePath());
			return false;
		}

		boolean trail = config.getBoolean(TRAIL_CONFIG_NAME, false);
		boolean flicker = config.getBoolean(FLICKER_CONFIG_NAME, false);

		FireworkEffect effect = FireworkEffect.builder()
			.flicker(flicker)
			.trail(trail)
			.with(type)
			.withColor(colors)
			.withFade(fadeColors)
			.build();

		fireworkMeta.setEffect(effect);
		data.setAttribute(FIREWORK_EFFECT, effect);

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(FIREWORK_EFFECT) || !(meta instanceof FireworkEffectMeta fireworkMeta)) return;

		fireworkMeta.setEffect(data.getAttribute(FIREWORK_EFFECT));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof FireworkEffectMeta effectMeta) || !effectMeta.hasEffect()) return;

		data.setAttribute(FIREWORK_EFFECT, effectMeta.getEffect());
	}

	@Nullable
	public static List<Color> getColorsFromString(String input, String name, boolean required) {
		if (input == null || input.isEmpty()) {
			if (required) {
				MagicDebug.warn("Invalid value '%s' for %s %s - firework effect color(s) not specified.", input, name, MagicDebug.resolvePath());
				return null;
			}

			return List.of();
		}

		List<Color> colors = new ArrayList<>();

		String[] colorStrings = input.split(",");
		for (String colorString : colorStrings) {
			try {
				int c = Integer.parseInt(colorString.trim(), 16);
				colors.add(Color.fromRGB(c));
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid color '%s' for %s %s.", colorString, name, MagicDebug.resolvePath());
				return null;
			}
		}

		return colors;
	}

}
