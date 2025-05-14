package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;

import io.papermc.paper.event.entity.EntityMoveEvent;

public class LilywalkSpell extends BuffSpell {

	private final Map<UUID, Lilies> entities;

	public LilywalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		Lilies lilies = new Lilies();
		lilies.move(data.target().getLocation().getBlock());

		entities.put(data.target().getUniqueId(), lilies);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		Lilies lilies = entities.remove(entity.getUniqueId());
		if (lilies != null) lilies.remove();
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities.keySet();
	}

	private void handleMove(LivingEntity entity, Block block) {
		Lilies lilies = entities.get(entity.getUniqueId());
		if (lilies == null) return;
		if (isExpired(entity)) {
			turnOff(entity);
			return;
		}
		boolean moved = lilies.isMoved(block);
		if (!moved) return;
		lilies.move(block);
		addUseAndChargeCost(entity);
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		handleMove(event.getPlayer(), event.getTo().getBlock());
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityMove(EntityMoveEvent event) {
		handleMove(event.getEntity(), event.getTo().getBlock());
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (entities.isEmpty()) return;
		final Block block = event.getBlock();
		if (block.getType() != Material.LILY_PAD) return;
		if (Util.containsValueParallel(entities, lilies -> lilies.contains(block))) event.setCancelled(true);
	}

	private static class Lilies {

		private final Set<Block> blocks = new HashSet<>();

		private Block center = null;

		private void move(Block center) {
			this.center = center;
			
			Iterator<Block> iterator = blocks.iterator();
			while (iterator.hasNext()) {
				Block b = iterator.next();
				if (b.equals(center)) continue;
				b.setType(Material.AIR);
				iterator.remove();
			}
			
			setToLily(center);
			setToLily(center.getRelative(BlockFace.NORTH));
			setToLily(center.getRelative(BlockFace.SOUTH));
			setToLily(center.getRelative(BlockFace.EAST));
			setToLily(center.getRelative(BlockFace.WEST));
			setToLily(center.getRelative(BlockFace.NORTH_WEST));
			setToLily(center.getRelative(BlockFace.NORTH_EAST));
			setToLily(center.getRelative(BlockFace.SOUTH_WEST));
			setToLily(center.getRelative(BlockFace.SOUTH_EAST));
		}
		
		private void setToLily(Block block) {
			if (!block.getType().isAir()) return;

			BlockData data = block.getRelative(BlockFace.DOWN).getBlockData();
			if (data.getMaterial() != Material.WATER || ((Levelled) data).getLevel() != 0) return;
			block.setType(Material.LILY_PAD);
			blocks.add(block);
		}
		
		private boolean isMoved(Block center) {
			return !Objects.equals(this.center, center);
		}
		
		private boolean contains(Block block) {
			return blocks.contains(block);
		}
		
		private void remove() {
			blocks.forEach(block -> block.setType(Material.AIR));
			blocks.clear();
		}
		
	}

}
