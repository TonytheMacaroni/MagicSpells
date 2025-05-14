package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;

public class DummySpell extends BuffSpell {

	private final Set<UUID> entities;
	
	public DummySpell(MagicConfig config, String spellName) {
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

	public Set<UUID> getEntities() {
		return entities;
	}

}
