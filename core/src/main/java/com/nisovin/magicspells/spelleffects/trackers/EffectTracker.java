package com.nisovin.magicspells.spelleffects.trackers;

import org.bukkit.entity.Entity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.DelayableEntity;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.effecttypes.EntityEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

public class EffectTracker implements Runnable {

	protected Entity entity;
	protected BuffSpell buffSpell;
	protected SpellEffect effect;
	protected SpellEffectActiveChecker checker;

	protected SpellData data;

	protected int effectTrackerTaskId;

	protected boolean isEntityEffect;

	protected DelayableEntity<Entity> effectEntity;

	public EffectTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		this.entity = entity;
		this.checker = checker;
		this.effect = effect;
		this.data = data;

		isEntityEffect = effect instanceof EntityEffect;

		int interval = effect.getEffectInterval().get(data);
		effectTrackerTaskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
	}

	public Entity getEntity() {
		return entity;
	}

	public BuffSpell getBuffSpell() {
		return buffSpell;
	}

	public SpellEffect getEffect() {
		return effect;
	}

	public SpellEffectActiveChecker getChecker() {
		return checker;
	}

	public int getEffectTrackerTaskId() {
		return effectTrackerTaskId;
	}

	public SpellData getData() {
		return data;
	}

	public DelayableEntity<Entity> getEffectEntity() {
		return effectEntity;
	}

	public void setBuffSpell(BuffSpell spell) {
		buffSpell = spell;
	}

	@Override
	public void run() {

	}

	public void stop() {
		MagicSpells.cancelTask(effectTrackerTaskId);
		entity = null;
		if (effectEntity != null) effectEntity.remove();
	}

	public void unregister() {
		if (buffSpell != null) buffSpell.getEffectTrackers().remove(this);
	}

}
