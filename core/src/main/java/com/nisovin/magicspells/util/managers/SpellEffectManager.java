package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.HashMap;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.spelleffects.*;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.spelleffects.effecttypes.*;

public class SpellEffectManager {

	private final Map<String, Class<? extends SpellEffect>> spellEffects = new HashMap<>();

	public SpellEffectManager() {
		initialize();
	}

	public Map<String, Class<? extends SpellEffect>> getSpellEffects() {
		return spellEffects;
	}

	/**
	 * @param spellEffect must be annotated with {@link Name}.
	 */
	public void addSpellEffect(Class<? extends SpellEffect> spellEffect) {
		Name name = spellEffect.getAnnotation(Name.class);
		if (name == null) throw new IllegalStateException("Missing 'Name' annotation on SpellEffect class: " + spellEffect.getName());
		spellEffects.put(name.value(), spellEffect);
	}

	/**
	 * @deprecated Use {@link SpellEffectManager#addSpellEffect(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addSpellEffect(String name, Class<? extends SpellEffect> spellEffect) {
		spellEffects.put(name.toLowerCase(), spellEffect);
	}

	/**
	 * @deprecated Use {@link SpellEffectManager#addSpellEffect(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addSpellEffect(Class<? extends SpellEffect> spellEffect, String name) {
		spellEffects.put(name.toLowerCase(), spellEffect);
	}

	public void removeSpellEffect(String name) {
		spellEffects.remove(name.toLowerCase());
	}

	public SpellEffect getSpellEffectByName(String name) {
		Class<? extends SpellEffect> clazz = spellEffects.get(name.toLowerCase());
		if (clazz == null) return null;

		DependsOn dependsOn = clazz.getAnnotation(DependsOn.class);
		if (dependsOn != null && !Util.checkPluginsEnabled(dependsOn.value())) return null;

		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}

	private void initialize() {
		addSpellEffect(ActionBarTextEffect.class);
		addSpellEffect(ArmorStandEffect.class);
		addSpellEffect(BlockBreakEffect.class);
		addSpellEffect(BossBarEffect.class);
		addSpellEffect(BroadcastEffect.class);
		addSpellEffect(CloudEffect.class);
		addSpellEffect(CompleteUsingItemEffect.class);
		addSpellEffect(DragonDeathEffect.class);
		addSpellEffect(EffectLibEffect.class);
		addSpellEffect(EffectLibEntityEffect.class);
		addSpellEffect(EffectLibLineEffect.class);
		addSpellEffect(EnderSignalEffect.class);
		addSpellEffect(EntityEffect.class);
		addSpellEffect(ExplosionEffect.class);
		addSpellEffect(FireworksEffect.class);
		addSpellEffect(GameTestAddMarkerEffect.class);
		addSpellEffect(GameTestClearMarkersEffect.class);
		addSpellEffect(ItemCooldownEffect.class);
		addSpellEffect(ItemSprayEffect.class);
		addSpellEffect(LightningEffect.class);
		addSpellEffect(MobSpawnerEffect.class);
		addSpellEffect(NovaEffect.class);
		addSpellEffect(ParticleCloudEffect.class);
		addSpellEffect(ParticlesEffect.class);
		addSpellEffect(ParticlesPersonalEffect.class);
		addSpellEffect(PotionEffect.class);
		addSpellEffect(SmokeSwirlEffect.class);
		addSpellEffect(SmokeTrailEffect.class);
		addSpellEffect(SoundEffect.class);
		addSpellEffect(SoundPersonalEffect.class);
		addSpellEffect(SplashPotionEffect.class);
		addSpellEffect(StartUsingItemEffect.class);
		addSpellEffect(StopUsingItemEffect.class);
		addSpellEffect(SwingHandEffect.class);
		addSpellEffect(TitleEffect.class);
		addSpellEffect(ToastEffect.class);
	}

}
