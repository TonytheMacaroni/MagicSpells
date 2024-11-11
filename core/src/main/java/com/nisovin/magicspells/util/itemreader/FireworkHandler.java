package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.DebugPath;
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

		String key = FIREWORK_EFFECTS.getKey();
		if (!config.isList(key)) return invalidIfSet(config, key);

		fireworkMeta.clearEffects();

		List<FireworkEffect> fireworkEffects = getFireworkEffects(config.getList(key), key);
		if (fireworkEffects == null) return false;

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

	@Nullable
	public static List<FireworkEffect> getFireworkEffects(@NotNull List<?> effectStrings, @NotNull String name) {
		try (var ignored = MagicDebug.section("Resolving firework effects from '%s'.", name)
			.pushPath(name, DebugPath.Type.LIST)
		) {
			List<FireworkEffect> fireworkEffects = new ArrayList<>();

			for (int i = 0; i < effectStrings.size(); i++) {
				try (var ignored1 = MagicDebug.pushListEntry(i)) {
					Object object = effectStrings.get(i);
					if (!(object instanceof String effectString)) {
						MagicDebug.warn("Invalid firework effect '%s' %s.", object, MagicDebug.resolveFullPath());
						return null;
					}

					FireworkEffect effect = getFireworkEffect(effectString);
					if (effect == null) return null;

					fireworkEffects.add(effect);
				}
			}

			return fireworkEffects;
		}
	}

	public static FireworkEffect getFireworkEffect(@NotNull String effectString, @NotNull String name) {
		try (var ignored = MagicDebug.pushPath(name, DebugPath.Type.SCALAR)) {
			return getFireworkEffect(effectString);
		}
	}

	@Nullable
	public static FireworkEffect getFireworkEffect(@NotNull String effectString) {
		try (var ignored = MagicDebug.section("Resolving firework effect '%s'.", effectString)) {
			String[] values = effectString.split(" ");
			if (values.length != 4 && values.length != 5) {
				MagicDebug.warn("Invalid firework effect '%s' %s - missing or too many values.", effectString, MagicDebug.resolveFullPath());
				return null;
			}

			FireworkEffect.Type type;
			try {
				type = FireworkEffect.Type.valueOf(values[0].toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid firework effect type '%s' %s.", values[0], MagicDebug.resolveFullPath());
				return null;
			}

			boolean trail = Boolean.parseBoolean(values[1]);
			boolean flicker = Boolean.parseBoolean(values[2]);

			List<Color> colors = FireworkEffectHandler.getColorsFromString(values[3], true);
			if (colors == null) return null;

			List<Color> fadeColors = values.length > 4 ? FireworkEffectHandler.getColorsFromString(values[4], true) : List.of();
			if (fadeColors == null) return null;

			return FireworkEffect.builder()
				.flicker(flicker)
				.trail(trail)
				.with(type)
				.withColor(colors)
				.withFade(fadeColors)
				.build();
		}
	}

}
