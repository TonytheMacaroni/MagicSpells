package com.nisovin.magicspells.castmodifiers.conditions;

import org.jetbrains.annotations.NotNull;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.castmodifiers.conditions.util.AbstractWorldGuardCondition;

@DependsOn("WorldGuard")
@Name("worldguardmembership")
public class WorldGuardRegionMembershipCondition extends AbstractWorldGuardCondition {

	private Type type = Type.MEMBER;

	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return true;
		try {
			type = Type.valueOf(var.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			MagicDebug.warn("Invalid type '%s' %s.", var, MagicDebug.resolveFullPath());
//			MagicSpells.error("Invalid type defined on 'worldguardmembership': " + var);
			return false;
		}
		return true;
	}

	@Override
	protected boolean check(ProtectedRegion region, LocalPlayer player) {
		return switch (type) {
			case OWNER -> region.isOwner(player);
			case MEMBER -> region.isMember(player);
			case MEMBER_ONLY -> region.isMemberOnly(player);
		};
	}

	private enum Type {
		OWNER,
		MEMBER,
		MEMBER_ONLY
	}
	
}
