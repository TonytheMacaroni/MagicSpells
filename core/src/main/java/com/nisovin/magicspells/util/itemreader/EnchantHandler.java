package com.nisovin.magicspells.util.itemreader;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.handlers.EnchantmentHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttributes.ENCHANTS;

public class EnchantHandler extends ItemHandler {

	private static final Converter<String, EnchantmentWithLevel> ENCHANT = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super EnchantmentWithLevel, ?> target) {
			String[] enchantData = value.split(" ");
			if (enchantData.length > 2) {
				MagicDebug.warn("Invalid enchant string '%s' %s - too many arguments.", value, MagicDebug.resolveFullPath());
				return ConverterResult.INVALID_NO_WARNING;
			}

			Enchantment enchant = EnchantmentHandler.getEnchantment(enchantData[0]);
			if (enchant == null) {
				MagicDebug.warn("Invalid enchantment '%s' %s.", enchantData[0], MagicDebug.resolveFullPath());
				return ConverterResult.INVALID_NO_WARNING;

			}

			int level = 0;
			if (enchantData.length > 1) {
				try {
					level = Integer.parseInt(enchantData[1]);
				} catch (IllegalArgumentException e) {
					MagicDebug.warn("Invalid enchant level '%s' %s.", enchantData[1], MagicDebug.resolveFullPath());
					return ConverterResult.INVALID_NO_WARNING;
				}
			}

			target.add(new EnchantmentWithLevel(enchant, level));
			return ConverterResult.VALID;
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("enchant");
		}

	};

	@Override
	public boolean process(@NotNull ConfigurationSection config, @NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!config.isList(ENCHANTS.getKey())) return invalidIfSet(config, ENCHANTS);

		ConversionResult<Void> result = Conversion
			.single(
				ConversionSource.listFromConfig(config, "enchants"),
				ENCHANT,
				ConversionTarget.consumer(enchantmentWithLevel -> {
					Enchantment enchantment = enchantmentWithLevel.enchantment;
					int level = enchantmentWithLevel.level;

					if (meta instanceof EnchantmentStorageMeta storageMeta)
						storageMeta.addStoredEnchant(enchantment, level, true);
					else
						meta.addEnchant(enchantment, level, true);
				})
			)
			.invalidateOnError()
			.convert();
		if (result.isInvalid()) return false;

		if (meta instanceof EnchantmentStorageMeta storageMeta && storageMeta.hasStoredEnchants())
			data.setAttribute(ENCHANTS, storageMeta.getStoredEnchants());
		else if (meta.hasEnchants())
			data.setAttribute(ENCHANTS, meta.getEnchants());

		return true;
	}

	@Override
	public void processItemMeta(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!data.hasAttribute(ENCHANTS)) return;

		Map<Enchantment, Integer> enchantments = data.getAttribute(ENCHANTS);
		for (Enchantment enchant : enchantments.keySet()) {
			int level = enchantments.get(enchant);

			if (meta instanceof EnchantmentStorageMeta storage) storage.addStoredEnchant(enchant, level, true);
			else meta.addEnchant(enchant, level, true);
		}
	}

	@Override
	public void processMagicItemData(@NotNull ItemStack item, @NotNull ItemMeta meta, @NotNull MagicItemData data) {
		if (!meta.hasEnchants()) return;

		data.setAttribute(ENCHANTS, meta.getEnchants());
	}

	private record EnchantmentWithLevel(Enchantment enchantment, int level) {}

}
