package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("regainhealth")
public class RegainHealthListener extends PassiveListener {

	private final EnumSet<RegainReason> reasons = EnumSet.noneOf(RegainReason.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var, ","),
			Converters.enumConverter(RegainReason.class),
			ConversionTarget.addTo(reasons)
		);
	}

	@OverridePriority
	@EventHandler
	public void onRegainHealth(EntityRegainHealthEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity caster)) return;
		if (!canTrigger(caster)) return;
		if (!reasons.isEmpty() && !reasons.contains(event.getRegainReason())) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
