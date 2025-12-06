package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable option is optional
// If not defined, it will trigger regardless of entity type
// If specified, it should be a comma separated list of entity types to accept
@Name("rightclickentity")
public class RightClickEntityListener extends PassiveListener {

	private final Set<EntityType> entities = EnumSet.noneOf(EntityType.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var.replace(" ", ""), ","),
			Converters.enumConverter(EntityType.class),
			ConversionTarget.addTo(entities)
		);
	}

	@OverridePriority
	@EventHandler
	public void onRightClickEntity(PlayerInteractAtEntityEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Entity entity = event.getRightClicked();
		if (!entities.isEmpty() && !entities.contains(entity.getType())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		boolean casted = entity instanceof LivingEntity ? passiveSpell.activate(caster, (LivingEntity) entity)
				: passiveSpell.activate(caster, entity.getLocation());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
