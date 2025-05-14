package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class StealthSpell extends BuffSpell {
	
	private final Set<UUID> entities;
	
	public StealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		entities = new HashSet<>();
	}
	
	@Override
	public boolean castBuff(SpellData data) {
		entities.add(data.target().getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities;
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if (!(event.getTarget() instanceof LivingEntity target) || !isActive(target)) return;

		if (isExpired(target)) {
			turnOff(target);
			return;
		}

		addUseAndChargeCost(target);
		event.setCancelled(true);
	}

	public Set<UUID> getEntities() {
		return entities;
	}
	
}
