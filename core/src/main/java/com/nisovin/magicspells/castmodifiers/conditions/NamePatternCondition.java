package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("namepattern")
public class NamePatternCondition extends Condition {

	private Pattern compiledPattern;
	
	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;
		compiledPattern = Pattern.compile(var);
		// note, currently won't translate the & to the color code,
		// this will need to be done through regex unicode format 
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return namePattern(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return namePattern(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean namePattern(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		return compiledPattern.asMatchPredicate().test(pl.getName()) || compiledPattern.asMatchPredicate().test(Util.getLegacyFromComponent(pl.displayName()));

	}

}
