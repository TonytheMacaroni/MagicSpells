package com.nisovin.magicspells.util.conversion;

import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public final class Converters {

	public static final StringConverter<BlockData> BLOCK_DATA = value -> {
		try (var ignored = MagicDebug.section("Resolving block data from string '%s'", value)) {
			return ConversionResult.valid(Bukkit.createBlockData(value.toLowerCase()));
		} catch (IllegalArgumentException e) {
			return ConversionResult.invalid();
		}
	};

	public static final StringConverter<MagicItemData> MAGIC_ITEM_DATA = value -> {
		MagicItemData data = MagicItems.getMagicItemDataFromString(value);
		return data != null ? ConversionResult.valid(data) : ConversionResult.invalid(false);
	};

	public static final StringConverter<Material> MATERIAL = value -> {
		try (var ignored = MagicDebug.section("Resolving material from string '%s'.", value)) {
			Material material = Material.matchMaterial(value.toLowerCase());
			return material != null ? ConversionResult.valid(material) : ConversionResult.invalid();
		}
	};

	public static final StringConverter<Material> MATERIAL_BLOCK = value -> {
		try (var ignored = MagicDebug.section("Resolving block type from string '%s'.", value)) {
			Material material = Material.matchMaterial(value.toLowerCase());
			if (material == null) return ConversionResult.invalid();

			if (!material.isBlock()) {
				MagicDebug.warn("Invalid non-block material '%s' specified %s.", value, MagicDebug.resolveFullPath());
				return ConversionResult.invalid(false);
			}

			return ConversionResult.valid(material);
		}
	};

	public static final StringConverter<Material> MATERIAL_ITEM = value -> {
		try (var ignored = MagicDebug.section("Resolving item type from string '%s'.", value)) {
			Material material = Material.matchMaterial(value.toLowerCase());
			if (material == null) return ConversionResult.invalid();

			if (!material.isItem()) {
				MagicDebug.warn("Invalid non-item material '%s' specified %s.", value, MagicDebug.resolveFullPath());
				return ConversionResult.invalid(false);
			}

			return ConversionResult.valid(material);
		}
	};

	public static final StringConverter<World> WORLD_BY_NAME = value -> {
		try (var ignored = MagicDebug.section("Resolving world from string '%s'.", value)) {
			World world = Bukkit.getWorld(value);
			if (world == null) {
				MagicDebug.warn("Invalid world name '%s' %s.", value, MagicDebug.resolveFullPath());
				return ConversionResult.invalid(false);
			}

			return ConversionResult.valid(world);
		}
	};

	public static <T extends Enum<T>> StringConverter<T> enumConverter(Class<T> type, boolean skippable) {
		return value -> {
			try {
				return ConversionResult.valid(Enum.valueOf(type, value.toUpperCase()));
			} catch (IllegalArgumentException e) {
				return skippable ? ConversionResult.skip() : ConversionResult.invalid();
			}
		};
	}

	public static <T> StringConverter<T> stringFunction(Function<String, T> function, boolean skippable) {
		return value -> {
			T converted = function.apply(value);
			if (converted != null) return ConversionResult.valid(converted);

			return skippable ? ConversionResult.skip() : ConversionResult.invalid();
		};
	}

}
