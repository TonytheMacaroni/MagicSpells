package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("permission")
public class PermissionCondition extends Condition {

	private String perm;

	@Override
	public boolean initialize(@NotNull String var) {
		perm = var;
		return true;
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return hasPermission(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return hasPermission(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean hasPermission(LivingEntity target) {
		return target.hasPermission(perm);
	}

}
