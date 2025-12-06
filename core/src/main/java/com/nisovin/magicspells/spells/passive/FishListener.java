package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.EnumSet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerFishEvent;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable can optionally include a comma-separated list of fish event states and entity types
@Name("fish")
public class FishListener extends PassiveListener {

	private final Set<PlayerFishEvent.State> states = EnumSet.noneOf(PlayerFishEvent.State.class);
	private final Set<EntityType> types = EnumSet.noneOf(EntityType.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		// TODO: Test this out
		Conversion.multi(ConversionSource.split(var.replace(" ", ""), ","))
			.target(Converters.enumConverter(PlayerFishEvent.State.class, "fishing state"), ConversionTarget.addTo(states))
			.target(Converters.registryEntryOrTag(RegistryKey.ENTITY_TYPE), ConversionTarget.addTo(types))
			.convert();
	}

	@OverridePriority
	@EventHandler
	public void onFish(PlayerFishEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		if (!states.isEmpty() && !states.contains(event.getState())) return;

		Entity caught = event.getCaught();
		if (!types.isEmpty() && (caught == null || !types.contains(caught.getType()))) return;

		boolean casted = caught instanceof LivingEntity ? passiveSpell.activate(caster, (LivingEntity) caught) :
			passiveSpell.activate(caster, event.getHook().getLocation());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
