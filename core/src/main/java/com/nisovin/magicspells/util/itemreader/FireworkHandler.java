package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.POWER;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.FIREWORK_EFFECTS;

public class FireworkHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof FireworkMeta fireworkMeta)) return true;

		if (!config.isInt(POWER.getKey()) && !invalidIfSet(config, POWER))
			return false;

		int power = config.getInt(POWER.getKey(), 0);
		fireworkMeta.setPower(power);
		data.setAttribute(POWER, power);

		if (!config.isList(FIREWORK_EFFECTS.getKey())) return invalidIfSet(config, FIREWORK_EFFECTS);

		List<FireworkEffect> fireworkEffects = new ArrayList<>();
		fireworkMeta.clearEffects();

		// <type> <trail> <flicker> <colors>(,) <fadeColors>(,)
		List<String> effectStrings = config.getStringList(FIREWORK_EFFECTS.getKey());
		for (String effectString : effectStrings) {
			String[] values = effectString.split(" ");
			if (values.length != 4 && values.length != 5) {
				MagicDebug.warn("Invalid firework effect '%s' %s - missing or too many values.", effectString, MagicDebug.resolvePath());
				return false;
			}

			FireworkEffect.Type type;
			try {
				type = FireworkEffect.Type.valueOf(values[0].toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid firework effect type '%s' %s.", values[0], MagicDebug.resolvePath());
				return false;
			}

			boolean trail = Boolean.parseBoolean(values[1]);
			boolean flicker = Boolean.parseBoolean(values[2]);

			List<Color> colors = FireworkEffectHandler.getColorsFromString(values[3], "colors", true);
			if (colors == null) return false;

			List<Color> fadeColors = values.length > 4 ? FireworkEffectHandler.getColorsFromString(values[4], "fade colors", true) : List.of();
			if (fadeColors == null) return false;

			FireworkEffect effect = FireworkEffect.builder()
				.flicker(flicker)
				.trail(trail)
				.with(type)
				.withColor(colors)
				.withFade(fadeColors)
				.build();

			fireworkEffects.add(effect);
		}

		if (!fireworkEffects.isEmpty()) {
			fireworkMeta.addEffects(fireworkEffects);
			data.setAttribute(FIREWORK_EFFECTS, fireworkEffects);
		}

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof FireworkMeta fireworkMeta)) return;

		if (data.hasAttribute(POWER)) fireworkMeta.setPower(data.getAttribute(POWER));

		if (data.hasAttribute(FIREWORK_EFFECTS)) {
			fireworkMeta.clearEffects();
			fireworkMeta.addEffects(data.getAttribute(FIREWORK_EFFECTS));
		}
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof FireworkMeta fireworkMeta)) return;

		data.setAttribute(POWER, fireworkMeta.getPower());

		List<FireworkEffect> effects = fireworkMeta.getEffects();
		if (!effects.isEmpty()) data.setAttribute(FIREWORK_EFFECTS, effects);
	}

}
