package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.BLOCK_DATA;

public class BlockDataHandler extends ItemHandler {

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isString(BLOCK_DATA.getKey())) return invalidIfSet(config, BLOCK_DATA);

		if (!(meta instanceof BlockDataMeta blockDataMeta)) {
			MagicDebug.warn("Invalid option 'block-data' specified %s - item type '%s' cannot have block data applied.", MagicDebug.resolveFullPath(), item.getType().getKey().getKey());
			return false;
		}

		String blockDataString = config.getString(BLOCK_DATA.getKey());

		BlockData blockData;
		try {
			blockData = Bukkit.createBlockData(item.getType(), blockDataString.toLowerCase());
		} catch (IllegalArgumentException e) {
			MagicDebug.warn("Invalid block data '%s' on magic item.", blockDataString);
			return false;
		}

		blockDataMeta.setBlockData(blockData);
		data.setAttribute(BLOCK_DATA, blockData);

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof BlockDataMeta blockDataMeta) || !data.hasAttribute(BLOCK_DATA)) return;

		blockDataMeta.setBlockData(data.getAttribute(BLOCK_DATA));
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!(meta instanceof BlockDataMeta blockDataMeta) || !blockDataMeta.hasBlockData()) return;

		data.setAttribute(BLOCK_DATA, blockDataMeta.getBlockData(item.getType()));
	}

}
