package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable is optional
// If not specified, it will trigger on any entity type
// If specified, it should be a comma separated list of entity types to trigger on
@Name("kill")
public class KillListener extends PassiveListener {

	private final EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var.replace(" ", ""), ","),
			Converters.registryEntryOrTag(RegistryKey.ENTITY_TYPE),
			ConversionTarget.addTo(types)
		);
	}

	@OverridePriority
	@EventHandler
	public void onDeath(EntityDeathEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!types.isEmpty() && !types.contains(event.getEntityType())) return;

		LivingEntity caster = event.getEntity().getKiller();
		if (caster == null || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster, event.getEntity());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
