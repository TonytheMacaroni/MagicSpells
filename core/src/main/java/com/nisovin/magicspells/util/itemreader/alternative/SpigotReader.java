package com.nisovin.magicspells.util.itemreader.alternative;

import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.debug.MagicDebug;

public class SpigotReader implements ItemConfigTransformer {
	
	private static final String READER_KEY = "external::spigot";
	private static final String DATA_KEY = "data";
	
	@Override
	public ItemStack deserialize(ConfigurationSection section) {
		if (section == null) return null;

		ItemStack item = section.getItemStack(DATA_KEY);
		if (item == null) {
			MagicDebug.warn("Invalid 'data' for type 'external::spigot' %s.", MagicDebug.resolvePath());
			return null;
		}

		return item;
	}
	
	@Override
	public ConfigurationSection serialize(ItemStack itemStack) {
		YamlConfiguration configuration = new YamlConfiguration();
		configuration.set(DATA_KEY, itemStack);
		return configuration;
	}
	
	@Override
	public String getReaderKey() {
		return READER_KEY;
	}
	
}
