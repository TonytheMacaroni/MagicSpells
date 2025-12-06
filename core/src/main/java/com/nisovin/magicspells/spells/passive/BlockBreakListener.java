package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of a comma separated list of blocks to accept
@Name("blockbreak")
public class BlockBreakListener extends PassiveListener {

	private final EnumSet<Material> materials = EnumSet.noneOf(Material.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.single(
			ConversionSource.split(var, ","),
			Converters.MATERIAL_BLOCK,
			ConversionTarget.addTo(materials)
		);
	}

	@OverridePriority
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player player = event.getPlayer();
		if (!canTrigger(player)) return;

		Block block = event.getBlock();
		if (!materials.isEmpty() && !materials.contains(block.getType())) return;

		boolean casted = passiveSpell.activate(player, block.getLocation().add(0.5, 0.5, 0.5));
		if (cancelDefaultAction(casted)) event.setCancelled(true);

	}

}
