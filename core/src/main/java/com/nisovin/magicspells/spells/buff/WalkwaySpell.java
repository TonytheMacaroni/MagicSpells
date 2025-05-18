package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import io.papermc.paper.event.entity.EntityMoveEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class WalkwaySpell extends BuffSpell {

	private final Map<UUID, Platform> entities;

	private final ConfigData<BlockData> stairType;
	private final ConfigData<BlockData> platformType;

	private final ConfigData<Integer> size;

	private WalkwayListener listener;

	public WalkwaySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		stairType = getConfigDataBlockData("stair-type", null);
		platformType = getConfigDataBlockData("platform-type", Material.OAK_PLANKS.createBlockData());

		size = getConfigDataInt("size", 6);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		entities.put(data.target().getUniqueId(), new Platform(data));
		registerListener();
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());

		Platform platform = entities.remove(data.target().getUniqueId());
		platform.remove();

		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		Platform platform = entities.remove(entity.getUniqueId());
		if (platform == null) return;

		platform.remove();
		unregisterListener();
	}

	@Override
	protected void turnOff() {
		super.turnOff();

		unregisterListener();
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities.keySet();
	}

	private void registerListener() {
		if (listener != null) return;
		listener = new WalkwayListener();
		registerEvents(listener);
	}

	private void unregisterListener() {
		if (listener == null || !entities.isEmpty()) return;
		unregisterEvents(listener);
		listener = null;
	}

	public class WalkwayListener implements Listener {

		private void handleMove(LivingEntity entity) {
			Platform carpet = entities.get(entity.getUniqueId());
			if (carpet == null) return;

			carpet.move();
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerMove(PlayerMoveEvent event) {
			handleMove(event.getPlayer());
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityMove(EntityMoveEvent event) {
			handleMove(event.getEntity());
		}

		@EventHandler(ignoreCancelled = true)
		public void onBlockBreak(BlockBreakEvent event) {
			Block block = event.getBlock();
			for (Platform platform : entities.values()) {
				if (!platform.blockInPlatform(block)) continue;

				event.setCancelled(true);
				return;
			}
		}

	}

	private class Platform {

		private static final double THRESHOLD = Math.nextDown(Math.sqrt(0.5));

		private final List<Block> platform;
		private final LivingEntity entity;

		private final int size;
		private final BlockData stairType;
		private final BlockData platformType;

		private int prevX;
		private int prevZ;
		private int prevModX;
		private int prevModY;
		private int prevModZ;

		private Platform(SpellData data) {
			this.entity = data.target();

			size = WalkwaySpell.this.size.get(data);

			platformType = WalkwaySpell.this.platformType.get(data);

			BlockData stairType = WalkwaySpell.this.stairType.get(data);
			if (stairType == null) {
				if (platformType.getMaterial() == Material.OAK_PLANKS)
					this.stairType = Material.OAK_STAIRS.createBlockData();
				else if (platformType.getMaterial() == Material.COBBLESTONE)
					this.stairType = Material.COBBLESTONE_STAIRS.createBlockData();
				else this.stairType = platformType;
			} else this.stairType = stairType;

			platform = new ArrayList<>();

			move();
			addUseAndChargeCost(data.target());
		}

		private void move() {
			Location location = entity.getLocation();
			Block origin = location.getBlock().getRelative(BlockFace.DOWN);

			float pitch = location.getPitch();
			float yaw = location.getYaw();

			int x = location.getBlockX();
			int z = location.getBlockZ();

			double dirX = -Math.sin(Math.toRadians(yaw));
			double dirZ = Math.cos(Math.toRadians(yaw));

			int modX, modY, modZ;

			if (dirX > THRESHOLD) modX = 1;
			else if (dirX < -THRESHOLD) modX = -1;
			else modX = 0;

			if (dirZ > THRESHOLD) modZ = 1;
			else if (dirZ < -THRESHOLD) modZ = -1;
			else modZ = 0;

			if (prevModY == 0) {
				if (pitch < -40) modY = 1;
				else if (pitch > 40) modY = -1;
				else modY = prevModY;
			} else if (prevModY == 1 && pitch > -10) modY = 0;
			else if (prevModY == -1 && pitch < 10) modY = 0;
			else modY = prevModY;

			if (x == prevX && z == prevZ && modX == prevModX && modY == prevModY && modZ == prevModZ) return;

			if (origin.getType().isAir()) {
				// Check for weird stair positioning
				Block up = origin.getRelative(0, 1, 0);
				if (up.getType() == stairType.getMaterial()) origin = up;
				else {
					// Allow down movement when stepping out over an edge
					Block down = origin.getRelative(0, -1, 0);
					if (!down.getType().isAir()) origin = down;
				}
			}

			drawCarpet(origin, modX, modY, modZ);
			addUseAndChargeCost(entity);

			prevX = x;
			prevZ = z;
			prevModX = modX;
			prevModY = modY;
			prevModZ = modZ;
		}

		private boolean blockInPlatform(Block block) {
			return platform.contains(block);
		}

		public void remove() {
			platform.forEach(b -> b.setType(Material.AIR));
		}

		private void drawCarpet(Block origin, int modX, int modY, int modZ) {
			BlockData blockData = platformType;
			if (modY != 0) {
				blockData = stairType;

				if (blockData instanceof Stairs stairs) {
					BlockFace facing;

					if (modX == 1) facing = BlockFace.WEST;
					else if (modX == -1) facing = BlockFace.EAST;
					else if (modZ == 1) facing = BlockFace.NORTH;
					else facing = BlockFace.SOUTH;
					if (modY == 1) facing = facing.getOppositeFace();

					stairs.setShape(Stairs.Shape.STRAIGHT);
					stairs.setFacing(facing);
				}
			}

			// Get platform blocks
			List<Block> blocks = new ArrayList<>();
			blocks.add(origin); // Add standing block
			for (int i = 1; i < size; i++) { // Add blocks ahead
				Block b = origin.getRelative(modX * i, modY * i, modZ * i);
				blocks.add(b);
			}

			// Remove old blocks
			Iterator<Block> iter = platform.iterator();
			while (iter.hasNext()) {
				Block b = iter.next();
				if (!blocks.contains(b)) {
					b.setType(Material.AIR);
					iter.remove();
				}
			}

			// Set new blocks
			for (Block b : blocks) {
				if (platform.contains(b) || b.getType().isAir()) {
					b.setBlockData(blockData);
					platform.add(b);
				}
			}
		}

	}

}
