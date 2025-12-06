package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityMountEvent;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("mount")
public class MountListener extends PassiveListener {

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
	public void onMount(EntityMountEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!types.isEmpty() && !types.contains(event.getMount().getType())) return;
		if (!canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
