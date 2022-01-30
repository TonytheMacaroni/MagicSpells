package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class CurrentHealthVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getHealth();
		return 0D;
	}

	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p == null) return;
		double max = Util.getMaxHealth(p);
		if (amount < 0) amount = 0;
		if (amount > max) amount = max;
		p.setHealth(amount);
	}

}
