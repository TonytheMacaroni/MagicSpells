package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.RegistryAccess;

import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.PATTERNS;

public class BannerHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isList(PATTERNS.getKey())) return invalidIfSet(config, PATTERNS);

		if (!(meta instanceof BannerMeta bannerMeta)) {
			MagicDebug.warn("Invalid option 'patterns' specified %s - item type '%s' cannot have banner patterns applied.", MagicDebug.resolvePath(), item.getType().getKey().getKey());
			return false;
		}

		List<Pattern> patterns = new ArrayList<>();

		List<String> patternStrings = config.getStringList(PATTERNS.getKey());
		for (String patternString : patternStrings) {
			String[] patternData = patternString.split(" ");
			if (patternData.length != 2) {
				MagicDebug.warn("Invalid banner pattern '%s' %s.", patternString, MagicDebug.resolvePath());
				return false;
			}

			String patternTypeString = patternData[0].toLowerCase();

			PatternType patternType = fromLegacyIdentifier(patternTypeString);
			if (patternType == null) {
				NamespacedKey key = NamespacedKey.fromString(patternTypeString);
				if (key != null) patternType = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).get(key);

				if (patternType == null) {
					MagicDebug.warn("Invalid pattern type '%s' in pattern '%s' %s.", patternData[0], patternString, MagicDebug.resolvePath());
					return false;
				}
			}

			DyeColor dyeColor;
			try {
				dyeColor = DyeColor.valueOf(patternData[1].toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid banner color '%s' in pattern '%s' %s.", patternData[1], patternString, MagicDebug.resolvePath());
				return false;
			}

			patterns.add(new Pattern(dyeColor, patternType));
		}

		if (!patterns.isEmpty()) {
			bannerMeta.setPatterns(patterns);
			data.setAttribute(PATTERNS, patterns);
		}

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof BannerMeta bannerMeta)) return;

		if (data.hasAttribute(PATTERNS)) bannerMeta.setPatterns(data.getAttribute(PATTERNS));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof BannerMeta bannerMeta)) return;

		List<Pattern> patterns = bannerMeta.getPatterns();
		if (!patterns.isEmpty()) data.setAttribute(PATTERNS, patterns);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BannerMeta bannerMeta)) return;

		if (data.hasAttribute(PATTERNS)) bannerMeta.setPatterns(data.getAttribute(PATTERNS));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BannerMeta bannerMeta)) return;

		List<Pattern> patterns = ((BannerMeta) meta).getPatterns();
		if (!patterns.isEmpty()) data.setAttribute(PATTERNS, bannerMeta.getPatterns());
	}

	private static PatternType fromLegacyIdentifier(String identifier) {
		return switch (identifier) {
			case "b" -> PatternType.BASE;
			case "bl" -> PatternType.SQUARE_BOTTOM_LEFT;
			case "br" -> PatternType.SQUARE_BOTTOM_RIGHT;
			case "tl" -> PatternType.SQUARE_TOP_LEFT;
			case "tr" -> PatternType.SQUARE_TOP_RIGHT;
			case "bs" -> PatternType.STRIPE_BOTTOM;
			case "ts" -> PatternType.STRIPE_TOP;
			case "ls" -> PatternType.STRIPE_LEFT;
			case "rs" -> PatternType.STRIPE_RIGHT;
			case "cs" -> PatternType.STRIPE_CENTER;
			case "ms" -> PatternType.STRIPE_MIDDLE;
			case "drs" -> PatternType.STRIPE_DOWNRIGHT;
			case "dls" -> PatternType.STRIPE_DOWNLEFT;
			case "ss" -> PatternType.SMALL_STRIPES;
			case "cr" -> PatternType.CROSS;
			case "sc" -> PatternType.STRAIGHT_CROSS;
			case "bt" -> PatternType.TRIANGLE_BOTTOM;
			case "tt" -> PatternType.TRIANGLE_TOP;
			case "bts" -> PatternType.TRIANGLES_BOTTOM;
			case "tts" -> PatternType.TRIANGLES_TOP;
			case "ld" -> PatternType.DIAGONAL_LEFT;
			case "rd" -> PatternType.DIAGONAL_UP_RIGHT;
			case "lud" -> PatternType.DIAGONAL_UP_LEFT;
			case "rud" -> PatternType.DIAGONAL_RIGHT;
			case "mc" -> PatternType.CIRCLE;
			case "mr" -> PatternType.RHOMBUS;
			case "vh" -> PatternType.HALF_VERTICAL;
			case "hh" -> PatternType.HALF_HORIZONTAL;
			case "vhr" -> PatternType.HALF_VERTICAL_RIGHT;
			case "hhb" -> PatternType.HALF_HORIZONTAL_BOTTOM;
			case "bo" -> PatternType.BORDER;
			case "cbo" -> PatternType.CURLY_BORDER;
			case "cre" -> PatternType.CREEPER;
			case "gra" -> PatternType.GRADIENT;
			case "gru" -> PatternType.GRADIENT_UP;
			case "bri" -> PatternType.BRICKS;
			case "sku" -> PatternType.SKULL;
			case "flo" -> PatternType.FLOWER;
			case "moj" -> PatternType.MOJANG;
			case "glb" -> PatternType.GLOBE;
			case "pig" -> PatternType.PIGLIN;
			default -> null;
		};
	}

}
