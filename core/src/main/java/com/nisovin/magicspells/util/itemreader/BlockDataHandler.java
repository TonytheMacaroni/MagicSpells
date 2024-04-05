package com.nisovin.magicspells.util.itemreader;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.BLOCK_DATA;

public class BlockDataHandler {

	private static final String CONFIG_NAME = BLOCK_DATA.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data, Material material) {
		if (!(meta instanceof BlockDataMeta blockDataMeta)) return;

		String blockDataString = config.getString(CONFIG_NAME);
		if (blockDataString == null) return;

		BlockData blockData;
		try {
			blockData = Bukkit.createBlockData(material, blockDataString.toLowerCase());
		} catch (IllegalArgumentException e) {
			MagicDebug.warn("Invalid block data '%s' on magic item.", blockDataString);
			return;
		}

		blockDataMeta.setBlockData(blockData);
		data.setAttribute(BLOCK_DATA, blockData);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BlockDataMeta blockDataMeta) || !data.hasAttribute(BLOCK_DATA)) return;
		blockDataMeta.setBlockData((BlockData) data.getAttribute(BLOCK_DATA));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data, Material material) {
		if (!(meta instanceof BlockDataMeta blockDataMeta) || !blockDataMeta.hasBlockData()) return;
		data.setAttribute(BLOCK_DATA, blockDataMeta.getBlockData(material));
	}

}
