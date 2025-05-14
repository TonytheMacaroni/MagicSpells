package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import io.papermc.paper.event.entity.EntityMoveEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class CarpetSpell extends BuffSpell {

	private final Map<UUID, BlockPlatform> entities;

	private final Set<UUID> falling;

	private final ConfigData<Material> platformMaterial;

	private final ConfigData<Integer> platformSize;

	public CarpetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		platformMaterial = getConfigDataMaterial("platform-block", Material.GLASS);

		platformSize = getConfigDataInt("size", 2);

		entities = new HashMap<>();
		falling = new HashSet<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		entities.put(data.target().getUniqueId(), new BlockPlatform(
			platformMaterial.get(data),
			Material.AIR,
			data.target().getLocation().getBlock().getRelative(0, -1, 0),
			platformSize.get(data),
			true,
			"square"
		));

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());

		BlockPlatform old = entities.get(data.target().getUniqueId());
		if (old != null) old.destroyPlatform();

		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		BlockPlatform platform = entities.remove(entity.getUniqueId());
		if (platform != null) platform.destroyPlatform();
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities.keySet();
	}

	private void handleMove(LivingEntity entity, Location to) {
		BlockPlatform platform = entities.get(entity.getUniqueId());
		if (platform == null) return;

		if (isExpired(entity)) {
			turnOff(entity);
			return;
		}

		Block block = to.getBlock().getRelative(BlockFace.DOWN);
		if (falling.contains(entity.getUniqueId())) block = to.getBlock().getRelative(BlockFace.DOWN, 2);

		boolean moved = platform.isMoved(block, true);
		if (moved) {
			platform.movePlatform(block, true);
			addUseAndChargeCost(entity);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		handleMove(event.getPlayer(), event.getTo());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityMove(EntityMoveEvent event) {
		handleMove(event.getEntity(), event.getTo());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (!isActive(player)) return;

		if (player.isSneaking()) {
			falling.remove(player.getUniqueId());
			return;
		}

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN, 2);
		boolean moved = entities.get(player.getUniqueId()).movePlatform(block);

		if (moved) {
			falling.add(player.getUniqueId());
			addUseAndChargeCost(player);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (entities.isEmpty()) return;
		final Block block = event.getBlock();
		if (Util.containsValueParallel(entities, platform -> platform.blockInPlatform(block))) event.setCancelled(true);
	}

	public Map<UUID, BlockPlatform> getEntities() {
		return entities;
	}

	public Set<UUID> getFalling() {
		return falling;
	}

}
