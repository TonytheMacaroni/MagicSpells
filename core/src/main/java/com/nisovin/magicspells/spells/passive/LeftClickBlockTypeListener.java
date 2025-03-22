package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable accepts a comma separated list of blocks to accept
@Name("leftclickblocktype")
public class LeftClickBlockTypeListener extends PassiveListener {

	private final Set<Material> materials = EnumSet.noneOf(Material.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		ConversionUtil.convert(
			ConversionSource.split(var, ","),
			ConversionTarget.consumer(materials::add),
			Converters.MATERIAL_BLOCK
		);
	}

	@OverridePriority
	@EventHandler
	public void onLeftClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		if (!isCancelStateOk(isCancelled(event))) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		Block block = event.getClickedBlock();
		if (block == null) return;

		if (!materials.isEmpty() && !materials.contains(block.getType())) return;

		boolean casted = passiveSpell.activate(event.getPlayer(), block.getLocation().add(0.5, 0.5, 0.5));
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean isCancelled(PlayerInteractEvent event) {
		return event.useInteractedBlock() == Event.Result.DENY;
	}

}
