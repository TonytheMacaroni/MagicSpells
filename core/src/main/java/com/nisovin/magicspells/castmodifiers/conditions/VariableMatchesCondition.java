package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("variablematches")
public class VariableMatchesCondition extends Condition {

	private String variable;
	
	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;
		variable = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return variableMatches(caster, null);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return variableMatches(caster, target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return variableMatches(caster, null);
	}

	private boolean variableMatches(LivingEntity caster, LivingEntity target) {
		if (!(caster instanceof Player pl)) return false;
		String name = null;
		if (target instanceof Player t) name = t.getName();
		// Check against normal (default)
		return Objects.equals(
				MagicSpells.getVariableManager().getStringValue(variable, pl),
				MagicSpells.getVariableManager().getStringValue(variable, name)
		);
	}

}
