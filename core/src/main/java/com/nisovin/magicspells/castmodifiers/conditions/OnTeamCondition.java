package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("onteam")
public class OnTeamCondition extends Condition {

	private String teamName;
	
	@Override
	public boolean initialize(@NotNull String var) {
		teamName = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return onTeam(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return onTeam(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean onTeam(LivingEntity target) {
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntityTeam(target);
		return team != null && team.getName().equals(teamName);
	}

}
