package com.nisovin.magicspells.util.itemreader.alternative;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.debug.MagicDebug;

public class VanillaReader implements ItemConfigTransformer {

	@Override
	public ItemStack deserialize(ConfigurationSection section) {
		if (section == null) return null;

		String data = section.getString("data");
		if (data == null) return null;

		ItemStack item;
		try {
			item = Bukkit.getItemFactory().createItemStack(data);
		} catch (IllegalArgumentException e) {
			MagicDebug.warn(e, "Invalid 'data' value '%s' for type 'external::vanilla' %s.", data, MagicDebug.resolvePath());
			return null;
		}

		item.setAmount(section.getInt("amount", 1));

		return item;
	}

	@Override
	public ConfigurationSection serialize(ItemStack itemStack) {
		YamlConfiguration configuration = new YamlConfiguration();

		String data = itemStack.getType().getKey().toString();
		if (itemStack.hasItemMeta()) data += itemStack.getItemMeta().getAsComponentString();

		configuration.set("data", data);
		configuration.set("amount", itemStack.getAmount());

		return configuration;
	}

	@Override
	public String getReaderKey() {
		return "external::vanilla";
	}

}
