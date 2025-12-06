package com.nisovin.magicspells.spells.passive;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.enchantment.EnchantItemEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("enchant")
public class EnchantListener extends PassiveListener {

	private Set<MagicItemData> items;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		items = Conversion.convert(
			ConversionSource.split(var, MagicItemDataParser.DATA_REGEX_PATTERN),
			Converters.MAGIC_ITEM_DATA,
			ConversionTarget.set(true)
		);
	}

	@OverridePriority
	@EventHandler
	public void onEnchant(EnchantItemEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		LivingEntity caster = event.getEnchanter();
		if (!canTrigger(caster)) return;

		if (items != null && !contains(event.getItem())) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean contains(ItemStack item) {
		if (item == null) item = new ItemStack(Material.AIR);

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (itemData == null) return false;

		for (MagicItemData data : items)
			if (data.matches(itemData))
				return true;

		return false;
	}

}
