package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class PotionEffectSpell extends TargetedSpell implements TargetedEntitySpell {

	private final List<ConfigData<PotionEffect>> potionEffects;

	private final ConfigData<PotionEffectType> type;

	private final ConfigData<Integer> duration;
	private final ConfigData<Integer> strength;

	private final ConfigData<Boolean> icon;
	private final ConfigData<Boolean> hidden;
	private final ConfigData<Boolean> ambient;
	private final ConfigData<Boolean> override;
	private final ConfigData<Boolean> targeted;
	private final boolean spellPowerAffectsDuration;
	private final boolean spellPowerAffectsStrength;

	public PotionEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		type = ConfigDataUtil.getPotionEffectType(config.getMainConfig(), internalKey + "type", PotionEffectType.SPEED);

		duration = getConfigDataInt("duration", 0);
		strength = getConfigDataInt("strength", 0);

		icon = getConfigDataBoolean("icon", true);
		hidden = getConfigDataBoolean("hidden", false);
		ambient = getConfigDataBoolean("ambient", false);
		targeted = getConfigDataBoolean("targeted", false);
		override = getConfigDataBoolean("override", false);
		spellPowerAffectsDuration = getConfigBoolean("spell-power-affects-duration", true);
		spellPowerAffectsStrength = getConfigBoolean("spell-power-affects-strength", true);

		potionEffects = Util.getPotionEffects(getConfigList("potion-effects", null), spellPowerAffectsDuration, spellPowerAffectsStrength);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (targeted.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		} else {
			SpellTargetEvent targetEvent = new SpellTargetEvent(this, data, data.caster());
			if (!targetEvent.callEvent()) return noTarget(targetEvent);
			data = targetEvent.getSpellData();
		}

		return castAtEntity(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (potionEffects == null) {
			PotionEffectType type = this.type.get(data);

			int duration = this.duration.get(data);
			if (spellPowerAffectsDuration) duration = Math.round(duration * data.power());

			int strength = this.strength.get(data);
			if (spellPowerAffectsStrength) strength = Math.round(strength * data.power());

			boolean ambient = this.ambient.get(data);
			boolean particles = !this.hidden.get(data);
			boolean icon = this.icon.get(data);

			PotionEffect effect = new PotionEffect(type, duration, strength, ambient, particles, icon);

			callDamageEvent(data.caster(), data.target(), effect);

			if (override.get(data) && data.target().hasPotionEffect(type)) data.target().removePotionEffect(type);
			data.target().addPotionEffect(effect);

			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		for (ConfigData<PotionEffect> effectData : potionEffects) {
			PotionEffect effect = effectData.get(data);

			callDamageEvent(data.caster(), data.target(), effect);

			if (override.get(data) && data.target().hasPotionEffect(effect.getType())) data.target().removePotionEffect(effect.getType());
			data.target().addPotionEffect(effect);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void callDamageEvent(LivingEntity caster, LivingEntity target, PotionEffect effect) {
		DamageCause cause = null;
		if (effect.getType() == PotionEffectType.POISON) cause = DamageCause.POISON;
		else if (effect.getType() == PotionEffectType.WITHER) cause = DamageCause.WITHER;

		if (cause != null) new SpellApplyDamageEvent(this, caster, target, effect.getAmplifier(), cause, "").callEvent();
	}

}
