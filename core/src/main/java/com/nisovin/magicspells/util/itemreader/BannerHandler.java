package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.DyeColor;
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
			MagicDebug.warn("Invalid option 'patterns' specified %s - item type '%s' cannot have banner patterns applied.", MagicDebug.resolveFullPath(), item.getType().getKey().getKey());
			return true;
		}

		List<Pattern> patterns = new ArrayList<>();

		List<String> patternStrings = config.getStringList(PATTERNS.getKey());
		for (String patternString : patternStrings) {
			String[] patternData = patternString.split(" ");
			if (patternData.length != 2) {
				MagicDebug.warn("Invalid banner pattern '%s' %s.", patternString, MagicDebug.resolveFullPath());
				return false;
			}

			PatternType patternType = PatternType.getByIdentifier(patternData[0].toLowerCase());
			if (patternType == null) {
				try {
					patternType = PatternType.valueOf(patternData[0].toUpperCase());
				} catch (IllegalArgumentException e) {
					MagicDebug.warn("Invalid pattern type '%s' in pattern '%s' %s.", patternData[0], patternString, MagicDebug.resolveFullPath());
					return false;
				}
			}

			DyeColor dyeColor;
			try {
				dyeColor = DyeColor.valueOf(patternData[1].toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid banner color '%s' in pattern '%s' %s.", patternData[1], patternString, MagicDebug.resolveFullPath());
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

}
