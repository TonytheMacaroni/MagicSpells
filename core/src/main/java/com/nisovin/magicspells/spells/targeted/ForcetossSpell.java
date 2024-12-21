package com.nisovin.magicspells.spells.targeted;

import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ForcetossSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Double> damage;

	private ConfigData<Double> vForce;
	private ConfigData<Double> hForce;
	private ConfigData<Double> rotation;

	private ConfigData<Boolean> checkPlugins;
	private ConfigData<Boolean> powerAffectsForce;
	private ConfigData<Boolean> powerAffectsDamage;
	private ConfigData<Boolean> addVelocityInstead;
	private ConfigData<Boolean> avoidDamageModification;

	public ForcetossSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damage = getConfigDataDouble("damage", 0);

		vForce = getConfigDataDouble("vertical-force", 10);
		hForce = getConfigDataDouble("horizontal-force", 20);
		rotation = getConfigDataDouble("rotation", 0);

		checkPlugins = getConfigDataBoolean("check-plugins", true);
		powerAffectsForce = getConfigDataBoolean("power-affects-force", true);
		powerAffectsDamage = getConfigDataBoolean("power-affects-damage", true);
		addVelocityInstead = getConfigDataBoolean("add-velocity-instead", false);
		avoidDamageModification = getConfigDataBoolean("avoid-damage-modification", true);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		if (!data.caster().getWorld().equals(data.target().getWorld())) return noTarget(data);

		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		double damage = this.damage.get(data);
		if (powerAffectsDamage.get(data)) damage *= data.power();

		if (damage > 0) target.damage(damage, caster);

		Vector v;
		if (caster.equals(target)) v = caster.getLocation().getDirection();
		else v = target.getLocation().toVector().subtract(caster.getLocation().toVector());

		double hForce = this.hForce.get(data) / 10;
		double vForce = this.vForce.get(data) / 10;
		if (powerAffectsForce.get(data)) {
			hForce *= data.power();
			vForce *= data.power();
		}
		v.setY(0).normalize().multiply(hForce).setY(vForce);

		double rotation = this.rotation.get(data);
		if (rotation != 0) Util.rotateVector(v, rotation);

		v = Util.makeFinite(v);

		if (addVelocityInstead.get(data)) target.setVelocity(target.getVelocity().add(v));
		else target.setVelocity(v);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
