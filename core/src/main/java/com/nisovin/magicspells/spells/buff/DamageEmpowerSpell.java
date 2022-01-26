package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.util.CastData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class DamageEmpowerSpell extends BuffSpell {

	private final Map<UUID, CastData> entities;

	private SpellFilter filter;

	private ConfigData<Float> damageMultiplier;

	public DamageEmpowerSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damageMultiplier = getConfigDataFloat("damage-multiplier", 1.5F);

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);

		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new CastData(power, args));
		return true;
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpellApplyDamage(SpellApplyDamageEvent event) {
		LivingEntity caster = event.getCaster();
		if (!isActive(caster)) return;
		if (!filter.check(event.getSpell())) return;

		addUseAndChargeCost(caster);

		CastData data = entities.get(caster.getUniqueId());
		float damageMultiplier = this.damageMultiplier.get(caster, event.getTarget(), data.power(), data.args());
		event.applyDamageModifier(damageMultiplier);
	}

	public Map<UUID, CastData> getEntities() {
		return entities;
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

}
