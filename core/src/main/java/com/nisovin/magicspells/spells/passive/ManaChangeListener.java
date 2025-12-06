package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("manachange")
public class ManaChangeListener extends PassiveListener {

	private final EnumSet<ManaChangeReason> reasons = EnumSet.noneOf(ManaChangeReason.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var, ","),
			Converters.enumConverter(ManaChangeReason.class),
			ConversionTarget.addTo(reasons)
		);
	}

	@OverridePriority
	@EventHandler
	public void onManaChange(ManaChangeEvent event) {
		if (!reasons.isEmpty() && !reasons.contains(event.getReason())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setNewAmount(event.getOldAmount());
	}

}
