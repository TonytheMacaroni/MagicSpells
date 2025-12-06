package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerShearEntityEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable that can either be set to a dye color to accept or "all"
@Name("sheepshear")
public class SheepShearListener extends PassiveListener {

	private final EnumSet<DyeColor> dyeColors = EnumSet.noneOf(DyeColor.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var, ","),
			Converters.enumConverter(DyeColor.class),
			ConversionTarget.addTo(dyeColors)
		);
	}

	@OverridePriority
	@EventHandler
	public void onSheepShear(PlayerShearEntityEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof Sheep)) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		Sheep target = (Sheep) event.getEntity();
		if (!dyeColors.isEmpty() && !dyeColors.contains(target.getColor())) return;

		boolean casted = passiveSpell.activate(caster, target);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
