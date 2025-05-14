package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

public class LightwalkSpell extends BuffSpell {

	private final Map<UUID, LightWalkData> players;

	private final Set<Material> allowedTypes;

	private final ConfigData<Double> yOffset;

	private final ConfigData<BlockData> blockType;

	public LightwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigDataDouble("y-offset", 0);
		blockType = getConfigDataBlockData("block-type", Material.LIGHT.createBlockData());

		players = new HashMap<>();

		allowedTypes = new HashSet<>();

		List<String> blockList = getConfigStringList("allowed-types", Collections.singletonList("air"));
		if (blockList == null) return;
		for (String str : blockList) {
			Material material = Util.getMaterial(str);
			if (material == null)
				MagicSpells.error("LightwalkSpell " + internalName + " has an invalid block defined: " + str);
			else allowedTypes.add(material);
		}
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;

		BlockData blockType = this.blockType.get(data);
		double yOffset = this.yOffset.get(data) + 0.5;

		Block block = target.getLocation().add(0, yOffset, 0).getBlock();
		if (allowedTypes.contains(block.getType())) target.sendBlockChange(block.getLocation(), blockType);
		else block = null;

		LightWalkData walkData = new LightWalkData(blockType, yOffset, block);
		players.put(data.target().getUniqueId(), walkData);

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		turnOffBuff(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (!(entity instanceof Player player)) return;

		LightWalkData data = players.remove(player.getUniqueId());
		if (data.current != null) player.sendBlockChange(data.current.getLocation(), data.current.getBlockData());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return players.keySet();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (players.isEmpty() || !event.hasChangedBlock()) return;

		Player player = event.getPlayer();

		LightWalkData data = players.get(player.getUniqueId());
		if (data == null) return;

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		Block newBlock = event.getTo().clone().add(0, data.yOffset, 0).getBlock();
		if (newBlock.equals(data.current)) return;

		if (data.current != null) {
			player.sendBlockChange(data.current.getLocation(), data.current.getBlockData());
			data.current = null;
		}

		if (!allowedTypes.contains(newBlock.getType())) return;

		player.sendBlockChange(newBlock.getLocation(), data.blockType);
		addUseAndChargeCost(player);
		data.current = newBlock;
	}

	public Set<Material> getAllowedTypes() {
		return allowedTypes;
	}

	private static final class LightWalkData {

		private final BlockData blockType;
		private final double yOffset;

		private Block current;

		private LightWalkData(BlockData blockType, double yOffset, Block current) {
			this.blockType = blockType;
			this.yOffset = yOffset;
			this.current = current;
		}

	}

}
