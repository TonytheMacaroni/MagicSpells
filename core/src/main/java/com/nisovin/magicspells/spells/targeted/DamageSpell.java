package com.nisovin.magicspells.spells.targeted;

import org.bukkit.damage.DamageType;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

@SuppressWarnings("UnstableApiUsage")
public class DamageSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final ConfigData<String> spellDamageType;
	private final ConfigData<DamageType> damageType;
	private final ConfigData<Boolean> creditCaster;
	private final ConfigData<Double> damage;

	public DamageSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellDamageType = getConfigDataString("spell-damage-type", "");
		creditCaster = getConfigDataBoolean("credit-caster", true);
		damageType = getConfigDataRegistryEntry("damage-type", RegistryKey.DAMAGE_TYPE, DamageType.GENERIC);
		damage = getConfigDataDouble("damage", 4);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		DamageType damageType = this.damageType.get(data);
		double damage = this.damage.get(data);

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, data.caster(), data.target(), damage, damageType, spellDamageType.get(data));
		event.callEvent();
		damage = event.getFinalDamage();

		DamageSource.Builder builder = DamageSource.builder(damageType);
		if (data.hasCaster() && creditCaster.get(data)) builder.withDirectEntity(data.caster());

		data.target().damage(damage, builder.build());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		DamageType damageType = this.damageType.get(data);
		double damage = this.damage.get(data);

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, data.caster(), data.target(), damage, damageType, spellDamageType.get(data));
		event.callEvent();
		damage = event.getFinalDamage();

		DamageSource.Builder builder = DamageSource.builder(damageType).withDamageLocation(data.location());
		if (data.hasCaster() && creditCaster.get(data)) builder.withDirectEntity(data.caster());

		data.target().damage(damage, builder.build());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
