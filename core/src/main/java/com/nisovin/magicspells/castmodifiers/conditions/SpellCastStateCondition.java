package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.IModifier;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

/**
 * Valid condition variable arguments are any of the following: {@link SpellCastState}
 */
@Name("spellcaststate")
public class SpellCastStateCondition extends Condition implements IModifier {

	private SpellCastState state;

	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;
		try {
			state = SpellCastState.valueOf(var.trim().toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			MagicDebug.warn("Invalid spell cast state '%s' %s.", var, MagicDebug.resolveFullPath());
			return false;
		}
	}

	@Override
	public boolean apply(SpellCastEvent event) {
		return event.getSpellCastState() == state;
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return false;
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		return false;
	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		return false;
	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		return false;
	}

	@Override
	public ModifierResult apply(LivingEntity caster, SpellData data) {
		return new ModifierResult(data, false);
	}

	@Override
	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		return new ModifierResult(data, false);
	}

	@Override
	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		return new ModifierResult(data, false);
	}

	@Override
	public boolean check(LivingEntity caster) {
		return false;
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return false;
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

}
