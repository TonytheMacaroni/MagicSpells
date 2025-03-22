package com.nisovin.magicspells.spells.passive;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;

import org.jetbrains.annotations.NotNull;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("insideblock")
public class InsideBlockListener extends PassiveListener {

	private List<BlockData> blockData;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		blockData = ConversionUtil.convert(
			ConversionSource.split(var, RegexUtil.BLOCK_DATA_SPLIT_PATTERN),
			ConversionTarget.list(),
			Converters.BLOCK_DATA
		);
	}

	@OverridePriority
	@EventHandler
	public void insideBlock(EntityInsideBlockEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!(event.getEntity() instanceof LivingEntity caster) || !canTrigger(caster)) return;

		Block block = event.getBlock();
		if (blockData != null && check(block)) return;

		boolean casted = passiveSpell.activate(caster, block.getLocation().toCenterLocation());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean check(Block block) {
		BlockData bd = block.getBlockData();

		for (BlockData data : blockData)
			if (bd.matches(data))
				return false;

		return true;
	}

}
