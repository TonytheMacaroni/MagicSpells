package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spelleffects.SpellEffect;

@Name("stopusingitem")
public class StopUsingItemEffect extends SpellEffect {

	@Override
	protected void loadFromConfig(ConfigurationSection config) {

	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if (entity instanceof LivingEntity livingEntity)
			livingEntity.clearActiveItem();

		return null;
	}

}
