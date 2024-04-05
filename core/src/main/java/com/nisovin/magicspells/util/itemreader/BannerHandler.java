package com.nisovin.magicspells.util.itemreader;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.DyeColor;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.PATTERNS;

public class BannerHandler {

	private static final String CONFIG_NAME = PATTERNS.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BannerMeta bannerMeta)) return;
		if (!config.isList(CONFIG_NAME)) return;

		List<Pattern> patterns = new ArrayList<>();

		List<String> patternStrings = config.getStringList(CONFIG_NAME);
		for (String patternString : patternStrings) {
			String[] patternData = patternString.split(" ");
			if (patternData.length != 2) {
				MagicDebug.warn("Invalid banner pattern '%s' on magic item.", patternString);
				continue;
			}

			PatternType patternType = PatternType.getByIdentifier(patternData[0].toLowerCase());
			if (patternType == null) {
				try {
					patternType = PatternType.valueOf(patternData[0].toUpperCase());
				} catch (IllegalArgumentException e) {
					MagicDebug.warn("Invalid pattern type '%s' on magic item.", patternData[0]);
					continue;
				}
			}

			DyeColor dyeColor;
			try {
				dyeColor = DyeColor.valueOf(patternData[1].toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid banner color '%s' on magic item.", patternData[1]);
				continue;
			}

			Pattern pattern = new Pattern(dyeColor, patternType);
			bannerMeta.addPattern(pattern);
			patterns.add(pattern);
		}

		if (!patterns.isEmpty()) data.setAttribute(PATTERNS, patterns);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BannerMeta)) return;

		if (data.hasAttribute(PATTERNS)) ((BannerMeta) meta).setPatterns((List<Pattern>) data.getAttribute(PATTERNS));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BannerMeta)) return;

		List<Pattern> patterns = ((BannerMeta) meta).getPatterns();
		if (!patterns.isEmpty()) data.setAttribute(PATTERNS, ((BannerMeta) meta).getPatterns());
	}
	
}
