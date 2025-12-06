package com.nisovin.magicspells.spells.passive;

import java.util.List;
import java.util.EnumSet;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.*;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("potioneffect")
public class PotionEffectListener extends PassiveListener {

	private boolean isAnyType = false;
	private EnumSet<Cause> causes = EnumSet.allOf(Cause.class);
	private EnumSet<Action> actions = EnumSet.allOf(Action.class);
	private final List<PotionEffectType> types = new ArrayList<>();

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		String[] splits = var.toUpperCase().split(" ");
		if (splits[0].equals("*")) isAnyType = true;
		else {
			Conversion.convert(
				ConversionSource.split(splits[0], ","),
				Converters.registryEntryOrTag(RegistryKey.MOB_EFFECT),
				ConversionTarget.addTo(types)
			);
		}

		if (splits.length > 1 && !splits[1].equals("*")) {
			Conversion.convert(
				ConversionSource.split(splits[1], ","),
				Converters.enumConverter(Action.class),
				ConversionTarget.addTo(actions)
			);
		} else actions = EnumSet.allOf(Action.class);

		if (splits.length > 1 && !splits[2].equals("*")) {
			Conversion.convert(
				ConversionSource.split(splits[1], ","),
				Converters.enumConverter(Cause.class),
				ConversionTarget.addTo(causes)
			);
		} else causes = EnumSet.allOf(Cause.class);
	}

	@EventHandler
	public void onPotionEffect(EntityPotionEffectEvent event) {
		if (!(event.getEntity() instanceof LivingEntity entity)) return;
		if (!isCancelStateOk(event.isCancelled())) return;

		if (!actions.contains(event.getAction()) || !causes.contains(event.getCause())) return;
		if (!canTrigger(entity)) return;

		if (!isAnyType) {
			// The effect used by the event is referenced differently based on the action, so unfortunately this is needed.
			PotionEffectType type = switch (event.getAction()) {
				case ADDED -> {
					PotionEffect effect = event.getNewEffect();
					yield effect == null ? null : effect.getType();
				}
				case CHANGED -> event.getModifiedType();
				case REMOVED, CLEARED -> {
					PotionEffect effect = event.getOldEffect();
					yield effect == null ? null : effect.getType();
				}
			};

			if (type == null || !types.contains(type)) return;
		}

		boolean casted = passiveSpell.activate(entity);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
