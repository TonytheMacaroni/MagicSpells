package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Map;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.DependsOn;
import com.nisovin.magicspells.castmodifiers.conditions.util.AbstractWorldGuardCondition;

@DependsOn("WorldGuard")
@Name("worldguardstateflag")
public class WorldGuardStateFlagCondition extends AbstractWorldGuardCondition {

	private static final Map<String, StateFlag> flags = new HashMap<>();
	static {
		for (Flag<?> flag : WorldGuard.getInstance().getFlagRegistry().getAll()) {
			if (!(flag instanceof StateFlag stateFlag)) continue;
			flags.put(flag.getName(), stateFlag);
		}
	}

	private StateFlag flag = null;

	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;
		flag = flags.get(var.toLowerCase());
		return flag != null;
	}

	@Override
	protected boolean check(ProtectedRegion region, LocalPlayer player) {
		return region.getFlag(flag) == StateFlag.State.ALLOW;
	}

}
