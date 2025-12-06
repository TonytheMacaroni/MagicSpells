package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDismountEvent;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("dismount")
public class DismountListener extends PassiveListener {

	private final EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var.replace(" ", ""), ","),
			Converters.enumConverter(EntityType.class),
			ConversionTarget.addTo(types)
		);
	}

	@OverridePriority
	@EventHandler
	public void onDismount(EntityDismountEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!types.isEmpty() && !types.contains(event.getDismounted().getType())) return;
		if (!canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
