package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

import static org.bukkit.event.entity.EntityTargetEvent.TargetReason;

@Name("entitytarget")
public class EntityTargetListener extends PassiveListener {

	private final Set<TargetReason> targetReasons = EnumSet.noneOf(TargetReason.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var, "\\|"),
			Converters.enumConverter(TargetReason.class),
			ConversionTarget.addTo(targetReasons)
		);
	}

	@OverridePriority
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!(event.getTarget() instanceof LivingEntity target)) return;

		if (!targetReasons.isEmpty() && !targetReasons.contains(event.getReason())) return;
		if (!canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster, target);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
