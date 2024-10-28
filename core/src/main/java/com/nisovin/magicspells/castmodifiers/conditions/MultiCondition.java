package com.nisovin.magicspells.castmodifiers.conditions;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.castmodifiers.*;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

@Name("collection")
public class MultiCondition extends Condition implements IModifier {

	private ModifierCollection collection;

	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;

		ModifierCollectionManager manager = MagicSpells.getModifierCollectionManager();

		collection = manager.getCollection(var);
		if (collection == null) {
			MagicDebug.warn("Invalid modifier collection '%s' specified %s.", var, MagicDebug.resolveFullPath());
			return false;
		}

		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return collection.check(caster, SpellData.NULL, modifier -> modifier.check(caster));
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return collection.check(caster, SpellData.NULL, modifier -> modifier.check(caster));
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return collection.check(caster, SpellData.NULL, modifier -> modifier.check(caster));
	}

	@Override
	public boolean apply(SpellCastEvent event) {
		return collection.check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));

	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return collection.check(event.getPlayer(), SpellData.NULL, modifier -> modifier.apply(event));
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		return collection.check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));

	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		return collection.check(event.getCaster(), event.getSpellData(), modifier -> modifier.apply(event));

	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		return collection.check(event.getPlayer(), SpellData.NULL, modifier -> modifier.apply(event));
	}

	@Override
	public ModifierResult apply(LivingEntity caster, SpellData data) {
		return collection.checkResult(caster, data, modifier -> modifier.apply(caster, data));
	}

	@Override
	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		return collection.checkResult(caster, data, modifier -> modifier.apply(caster, target, data));
	}

	@Override
	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		return collection.checkResult(caster, data, modifier -> modifier.apply(caster, target, data));

	}

}
