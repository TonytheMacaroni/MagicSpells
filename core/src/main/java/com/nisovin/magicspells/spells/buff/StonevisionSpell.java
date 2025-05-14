package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class StonevisionSpell extends BuffSpell {

	private final Map<UUID, TransparentBlockSet> players;

	private final Set<Material> transparentTypes;

	private final ConfigData<Integer> radius;

	private final ConfigData<Boolean> unobfuscate;

	private final ConfigData<BlockData> material;

	public StonevisionSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataInt("radius", 4);
		unobfuscate = getConfigDataBoolean("unobfuscate", false);

		transparentTypes = new HashSet<>();

		material = getConfigDataBlockData("material", Material.BARRIER.createBlockData());

		List<String> types = getConfigStringList("transparent-types", null);
		if (types != null) {
			for (String str : types) {
				Material material = Util.getMaterial(str);
				if (material != null && material.isBlock()) transparentTypes.add(material);
			}
		}

		if (transparentTypes.isEmpty())
			MagicSpells.error("StonevisionSpell '" + internalName + "' does not define any transparent types");

		players = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;

		players.put(target.getUniqueId(), new TransparentBlockSet(target, transparentTypes, data));
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());

		TransparentBlockSet blockSet = players.remove(data.target().getUniqueId());
		blockSet.removeTransparency();

		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		TransparentBlockSet set = players.remove(entity.getUniqueId());
		if (set != null) set.removeTransparency();
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return players.keySet();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player pl = event.getPlayer();
		if (!isActive(pl)) return;
		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		boolean moved = players.get(pl.getUniqueId()).moveTransparency();
		if (!moved) return;
		addUseAndChargeCost(pl);
	}

	public Set<Material> getTransparentTypes() {
		return transparentTypes;
	}

	private class TransparentBlockSet {

		private final Set<Material> types;
		private final Player player;

		private List<Block> blocks;
		private Block center;

		private final boolean unobfuscate;
		private final BlockData material;
		private final int radius;

		private TransparentBlockSet(Player player, Set<Material> types, SpellData data) {
			this.player = player;
			this.types = types;

			unobfuscate = StonevisionSpell.this.unobfuscate.get(data);
			material = StonevisionSpell.this.material.get(data);
			radius = StonevisionSpell.this.radius.get(data);

			blocks = new ArrayList<>();
			center = player.getLocation().getBlock();

			setTransparency();
		}

		private void setTransparency() {
			List<Block> newBlocks = new ArrayList<>();

			// Get blocks to set to transparent
			int px = center.getX();
			int py = center.getY();
			int pz = center.getZ();
			Block block;
			if (!unobfuscate) {
				// Handle normally
				for (int x = px - radius; x <= px + radius; x++) {
					for (int y = py - radius; y <= py + radius; y++) {
						for (int z = pz - radius; z <= pz + radius; z++) {
							block = center.getWorld().getBlockAt(x, y, z);
							if (types.contains(block.getType())) {
								player.sendBlockChange(block.getLocation(), material);
								newBlocks.add(block);
							}
						}
					}
				}
			} else {
				// Unobfuscate everything
				int dx;
				int dy;
				int dz;
				for (int x = px - radius - 1; x <= px + radius + 1; x++) {
					for (int y = py - radius - 1; y <= py + radius + 1; y++) {
						for (int z = pz - radius - 1; z <= pz + radius + 1; z++) {
							dx = Math.abs(x - px);
							dy = Math.abs(y - py);
							dz = Math.abs(z - pz);
							block = center.getWorld().getBlockAt(x, y, z);
							if (types.contains(block.getType()) && dx <= radius && dy <= radius && dz <= radius) {
								player.sendBlockChange(block.getLocation(), material);
								newBlocks.add(block);
							} else if (!block.getType().isAir()) {
								player.sendBlockChange(block.getLocation(), block.getType().createBlockData());
							}
						}
					}
				}
			}

			// Remove old transparent blocks
			blocks.stream().filter(b -> !newBlocks.contains(b)).forEachOrdered(b -> player.sendBlockChange(b.getLocation(), b.getType().createBlockData()));
			// Update block set
			blocks = newBlocks;
		}

		private boolean moveTransparency() {
			Location loc = this.player.getLocation();
			if (!center.getWorld().equals(loc.getWorld()) || center.getX() != loc.getBlockX() || center.getY() != loc.getBlockY() || center.getZ() != loc.getBlockZ()) {
				// Moved
				center = loc.getBlock();
				setTransparency();
				return true;
			}
			return false;
		}

		private void removeTransparency() {
			blocks.forEach(b -> player.sendBlockChange(b.getLocation(), b.getType().createBlockData()));
			blocks = null;
		}

	}

}
