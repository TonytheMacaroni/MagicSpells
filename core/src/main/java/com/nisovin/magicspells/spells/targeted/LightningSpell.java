package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class LightningSpell extends TargetedSpell implements TargetedLocationSpell {

	private final ConfigData<Double> additionalDamage;

	private final ConfigData<Boolean> zapPigs;
	private final ConfigData<Boolean> noDamage;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> chargeCreepers;
	private final ConfigData<Boolean> requireEntityTarget;
	private final ConfigData<Boolean> powerAffectsAdditionalDamage;

	public LightningSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		additionalDamage = getConfigDataDouble("additional-damage", 0F);

		zapPigs = getConfigDataBoolean("zap-pigs", true);
		noDamage = getConfigDataBoolean("no-damage", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		chargeCreepers = getConfigDataBoolean("charge-creepers", true);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", false);
		powerAffectsAdditionalDamage = getConfigDataBoolean("power-affects-additional-damage", true);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (requireEntityTarget.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();

			if (noDamage.get(data)) {
				data.target().getWorld().strikeLightningEffect(data.target().getLocation());
				playSpellEffects(data);
				return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
			}

			double additionalDamage = this.additionalDamage.get(data);
			if (powerAffectsAdditionalDamage.get(data)) additionalDamage *= data.power();

			if (checkPlugins.get(data) && checkFakeDamageEvent(data.caster(), data.target()))
				return noTarget(data);

			ChargeOption option = new ChargeOption(additionalDamage, chargeCreepers.get(data), zapPigs.get(data));

			LightningStrike strike = data.target().getWorld().strikeLightning(data.target().getLocation());
			strike.setMetadata("MS" + internalName, new FixedMetadataValue(MagicSpells.plugin, option));

			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		TargetInfo<Location> info = getTargetedBlockLocation(data);
		if (info.noTarget()) return noTarget(info);

		return castAtLocation(info.spellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location target = data.location();

		if (noDamage.get(data)) target.getWorld().strikeLightningEffect(target);
		else {
			double additionalDamage = this.additionalDamage.get(data);
			if (powerAffectsAdditionalDamage.get(data)) additionalDamage *= data.power();
			ChargeOption option = new ChargeOption(additionalDamage, chargeCreepers.get(data), zapPigs.get(data));

			LightningStrike strike = target.getWorld().strikeLightning(target);
			strike.setMetadata("MS" + internalName, new FixedMetadataValue(MagicSpells.plugin, option));
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler
	public void onLightningDamage(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.LIGHTNING || !(event.getDamager() instanceof LightningStrike strike)) return;

		List<MetadataValue> data = strike.getMetadata("MS" + internalName);
		if (data.isEmpty()) return;

		for (MetadataValue val : data) {
			ChargeOption option = (ChargeOption) val.value();
			if (option != null && option.additionalDamage > 0)
				event.setDamage(event.getDamage() + option.additionalDamage);
			return;
		}
	}

	@EventHandler
	public void onCreeperCharge(CreeperPowerEvent event) {
		LightningStrike strike = event.getLightning();
		if (strike == null) return;
		List<MetadataValue> data = strike.getMetadata("MS" + internalName);
		if (data.isEmpty()) return;
		for (MetadataValue val : data) {
			ChargeOption option = (ChargeOption) val.value();
			if (option == null) continue;
			if (!option.chargeCreeper) event.setCancelled(true);
			break;
		}
	}

	@EventHandler
	public void onPigZap(PigZapEvent event) {
		LightningStrike strike = event.getLightning();
		List<MetadataValue> data = strike.getMetadata("MS" + internalName);
		if (data.isEmpty()) return;
		for (MetadataValue val : data) {
			ChargeOption option = (ChargeOption) val.value();
			if (option == null) continue;
			if (!option.changePig) event.setCancelled(true);
		}
	}

	private record ChargeOption(double additionalDamage, boolean chargeCreeper, boolean changePig) { }

}
