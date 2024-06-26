package com.nisovin.magicspells.variables.meta;

import org.bukkit.Bukkit;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class VelocityYVariable extends MetaVariable {
	
	@Override
	public double getValue(String player) {
		Player p = Bukkit.getPlayerExact(player);
		if (p != null) return p.getVelocity().getY();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = Bukkit.getPlayerExact(player);
		if (p == null) return;

		Vector velocity = p.getVelocity();
		velocity.setY(amount);
		p.setVelocity(velocity);
	}

}
