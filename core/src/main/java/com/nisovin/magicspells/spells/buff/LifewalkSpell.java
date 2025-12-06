package com.nisovin.magicspells.spells.buff;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.conversion.*;

public class LifewalkSpell extends BuffSpell {
	
	private final Set<UUID> entities;

	private final Map<Material, Integer> blocks;

	private Grower grower;

	private int tickInterval;
	
	public LifewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tickInterval = getConfigInt("tick-interval", 15);

		blocks = new HashMap<>();

		entities = new HashSet<>();

		if (!isConfigList("blocks")) {
			blocks.put(Material.TALL_GRASS, 25);
			blocks.put(Material.FERN, 20);
			blocks.put(Material.POPPY, 15);
			blocks.put(Material.DANDELION, 10);
			blocks.put(Material.OAK_SAPLING, 5);
			return;
		}

		record MaterialWithChance(Material material, int chance) {

			MaterialWithChance material(Material material) {
				return new MaterialWithChance(material, this.chance);
			}

			MaterialWithChance chance(int chance) {
				return new MaterialWithChance(this.material, chance);
			}

		}

		Conversion.convert(
			getListSource("blocks"),
			Converters.<MaterialWithChance>sequential2("block entry")
				.supplier(new MaterialWithChance(null, 0))
				.step(Converters.MATERIAL_BLOCK, (block, value) -> value.material(block))
				.step(Converters.rangedInteger(0, "chance"), (chance, value) -> value.chance(chance)),
			ConversionTarget.consumer(entry -> blocks.put(entry.material, entry.chance))
		);
	}

	@Override
	public boolean castBuff(SpellData data) {
		entities.add(data.target().getUniqueId());
		if (grower == null) grower = new Grower();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		if (!entities.isEmpty()) return;

		if (grower == null) return;
		grower.stop();
		grower = null;
	}
	
	@Override
	protected void turnOff() {
		super.turnOff();

		if (grower == null) return;
		grower.stop();
		grower = null;
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities;
	}

	public Set<UUID> getEntities() {
		return entities;
	}

	public Map<Material, Integer> getBlocks() {
		return blocks;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	private class Grower implements Runnable {
		
		private final int taskId;

		private Grower() {
			taskId = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval);
		}
		
		public void stop() {
			MagicSpells.cancelTask(taskId);
		}
		
		@Override
		public void run() {
			for (UUID id : entities) {
				Entity entity = Bukkit.getEntity(id);
				if (entity == null) continue;
				if (!entity.isValid()) continue;
				if (!(entity instanceof LivingEntity livingEntity)) continue;
				if (isExpired(livingEntity)) {
					turnOff(livingEntity);
					continue;
				}

				Block feet = livingEntity.getLocation().getBlock();
				Block ground = feet.getRelative(BlockFace.DOWN);

				if (!feet.getType().isAir()) continue;
				if (ground.getType() != Material.DIRT && ground.getType() != Material.GRASS_BLOCK) continue;
				if (ground.getType() == Material.DIRT) ground.setType(Material.GRASS_BLOCK);

				int rand = random.nextInt(100);

				for (Material m : blocks.keySet()) {
					int chance = blocks.get(m);

					if (rand > chance) continue;

					feet.setType(m);
					addUseAndChargeCost(livingEntity);

					rand = random.nextInt(100);
				}
			}
		}
	}

}
