package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class ResistSpell extends BuffSpell {

	private final Map<UUID, ResistData> entities;

	private final Set<String> spellDamageTypes;

	private final Set<DamageCause> normalDamageTypes;

	private final ConfigData<Double> flatModifier;

	private final ConfigData<Float> multiplier;

	private final ConfigData<Boolean> constantMultiplier;
	private final ConfigData<Boolean> constantFlatModifier;
	private final ConfigData<Boolean> powerAffectsMultiplier;
	private final ConfigData<Boolean> powerAffectsFlatModifier;

	public ResistSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		flatModifier = getConfigDataDouble("flat-modifier", 0D);
		multiplier = getConfigDataFloat("multiplier", 0.5F);

		constantMultiplier = getConfigDataBoolean("constant-multiplier", true);
		constantFlatModifier = getConfigDataBoolean("constant-flat-modifier", true);
		powerAffectsMultiplier = getConfigDataBoolean("power-affects-multiplier", true);
		powerAffectsFlatModifier = getConfigDataBoolean("power-affects-flat-modifier", true);

		normalDamageTypes = new HashSet<>();
		List<String> causes = getConfigStringList("normal-damage-types", null);
		if (causes != null) {
			for (String cause : causes) {
				try {
					DamageCause damageCause = DamageCause.valueOf(cause.replace(" ", "_").replace("-", "_").toUpperCase());
					normalDamageTypes.add(damageCause);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ResistSpell '" + internalName + "' has an invalid damage cause defined '" + cause + "'!");
				}
			}
		}

		spellDamageTypes = new HashSet<>();
		causes = getConfigStringList("spell-damage-types", null);
		if (causes != null) spellDamageTypes.addAll(causes);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		boolean constantMultiplier = this.constantMultiplier.get(data);

		float multiplier = 0;
		if (constantMultiplier) {
			multiplier = this.multiplier.get(data);
			if (powerAffectsMultiplier.get(data)) {
				if (Math.abs(multiplier) < 1) multiplier /= data.power();
				else if (Math.abs(multiplier) > 1) multiplier *= data.power();
			}
		}
		boolean constantFlatDamage = this.constantFlatModifier.get(data);

		double flatModifier = 0;
		if (constantFlatDamage) {
			flatModifier = this.flatModifier.get(data);
			if (powerAffectsFlatModifier.get(data)) {
				if (Math.abs(flatModifier) < 1) flatModifier /= data.power();
				else if (Math.abs(flatModifier) > 1) flatModifier *= data.power();
			}
		}

		entities.put(data.target().getUniqueId(), new ResistData(data, multiplier, constantMultiplier, flatModifier, constantFlatDamage));
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler
	public void onSpellDamage(SpellApplyDamageEvent event) {
		if (spellDamageTypes.isEmpty()) return;
		if (!isActive(event.getTarget())) return;

		String spellDamageType = event.getSpellDamageType();
		if (spellDamageType == null) return;
		if (!spellDamageTypes.contains(spellDamageType)) return;

		LivingEntity caster = event.getTarget();
		ResistData data = entities.get(caster.getUniqueId());

		float multiplier = data.multiplier;
		if (!data.constantMultiplier) {
			SpellData subData = data.spellData.target(event.getCaster());

			multiplier = this.multiplier.get(subData);
			if (powerAffectsMultiplier.get(subData)) {
				if (Math.abs(multiplier) < 1) multiplier /= subData.power();
				else if (Math.abs(multiplier) > 1) multiplier *= subData.power();
			}
		}

		double flatModifier = data.flatModifier;
		if (!data.constantFlatModifier) {
			SpellData subData = data.spellData.target(event.getCaster());

			flatModifier = this.flatModifier.get(subData);
			if (powerAffectsFlatModifier.get(subData)) {
				if (Math.abs(flatModifier) < 1) flatModifier /= subData.power();
				else if (Math.abs(flatModifier) > 1) flatModifier *= subData.power();
			}
		}

		addUseAndChargeCost(caster);
		event.applyDamageModifier(multiplier);
		event.applyFlatDamageModifier(flatModifier);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (normalDamageTypes.isEmpty()) return;
		if (!normalDamageTypes.contains(event.getCause())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity caster)) return;
		if (!isActive(caster)) return;

		LivingEntity target = null;
		if (event instanceof EntityDamageByEntityEvent e && e.getDamager() instanceof LivingEntity damager)
			target = damager;

		ResistData data = entities.get(caster.getUniqueId());

		float multiplier = data.multiplier;
		if (!data.constantMultiplier) {
			SpellData subData = data.spellData.target(target);

			multiplier = this.multiplier.get(subData);
			if (powerAffectsMultiplier.get(subData)) {
				if (Math.abs(multiplier) < 1) multiplier /= subData.power();
				else if (Math.abs(multiplier) > 1) multiplier *= subData.power();
			}
		}

		double flatModifier = data.flatModifier;
		if (!data.constantFlatModifier) {
			SpellData subData = data.spellData.target(target);

			flatModifier = this.flatModifier.get(subData);
			if (powerAffectsFlatModifier.get(subData)) {
				if (Math.abs(flatModifier) < 1) flatModifier /= subData.power();
				else if (Math.abs(flatModifier) > 1) flatModifier *= subData.power();
			}
		}

		addUseAndChargeCost(caster);

		double finalDamage = (event.getDamage() * multiplier) - flatModifier;
		if (finalDamage < 0D) finalDamage = 0D;
		event.setDamage(finalDamage);
	}

	public Map<UUID, ResistData> getEntities() {
		return entities;
	}

	public Set<String> getSpellDamageTypes() {
		return spellDamageTypes;
	}

	public Set<DamageCause> getNormalDamageTypes() {
		return normalDamageTypes;
	}

	public record ResistData(SpellData spellData, float multiplier, boolean constantMultiplier, double flatModifier, boolean constantFlatModifier) {}

}
