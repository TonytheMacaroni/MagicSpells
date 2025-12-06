package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

import net.kyori.adventure.text.Component;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.PAGES;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.TITLE;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.AUTHOR;

public class WrittenBookHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof BookMeta bookMeta)) return true;

		if (config.isString(TITLE.getKey())) {
			Component title = Util.getMiniMessage(config.getString(TITLE.getKey()));

			bookMeta.title(title);
			data.setAttribute(TITLE, bookMeta.title());
		} else if (!invalidIfSet(config, TITLE)) return false;

		if (config.isString(AUTHOR.getKey())) {
			Component author = Util.getMiniMessage(config.getString(AUTHOR.getKey()));

			bookMeta.author(author);
			data.setAttribute(AUTHOR, bookMeta.author());
		} else if (!invalidIfSet(config, AUTHOR)) return false;

		if (config.isList(PAGES.getKey())) {
			List<Component> pages = new ArrayList<>();

			List<String> pageStrings = config.getStringList(PAGES.getKey());
			for (String pageString : pageStrings) pages.add(Util.getMiniMessage(pageString));

			if (!pages.isEmpty()) {
				bookMeta.pages(pages);
				data.setAttribute(PAGES, bookMeta.pages());
			}
		} else if (!invalidIfSet(config, PAGES)) return false;

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof BookMeta bookMeta)) return;

		if (data.hasAttribute(TITLE)) bookMeta.title(data.getAttribute(TITLE));
		if (data.hasAttribute(AUTHOR)) bookMeta.author(data.getAttribute(AUTHOR));
		if (data.hasAttribute(PAGES)) bookMeta.pages(data.getAttribute(PAGES));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof BookMeta bookMeta)) return;

		if (bookMeta.hasAuthor()) data.setAttribute(AUTHOR, bookMeta.author());
		if (bookMeta.hasTitle()) data.setAttribute(TITLE, bookMeta.title());
		if (bookMeta.hasPages()) data.setAttribute(PAGES, bookMeta.pages());
	}

	public static Component getTitle(ItemMeta meta) {
		return meta instanceof BookMeta bookMeta ? bookMeta.title() : null;
	}

	public static Component getAuthor(ItemMeta meta) {
		return meta instanceof BookMeta bookMeta ? bookMeta.author() : null;
	}

}
