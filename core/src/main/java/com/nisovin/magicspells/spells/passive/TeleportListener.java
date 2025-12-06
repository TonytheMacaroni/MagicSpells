package com.nisovin.magicspells.spells.passive;

import java.util.Map;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.conversion.Conversion;
import com.nisovin.magicspells.util.conversion.Converters;
import com.nisovin.magicspells.util.conversion.ConversionSource;
import com.nisovin.magicspells.util.conversion.ConversionTarget;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of teleport causes to accept
@Name("teleport")
public class TeleportListener extends PassiveListener {

	private static final Map<String, TeleportCause> LEGACY_ALIASES = Arrays.stream(TeleportCause.values())
		.collect(Collectors.toMap(
			cause -> cause.name().replace("_", ""),
			cause -> cause
		));

	private final EnumSet<TeleportCause> teleportCauses = EnumSet.noneOf(TeleportCause.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var, ","),
			Converters.enumConverter(TeleportCause.class, LEGACY_ALIASES),
			ConversionTarget.addTo(teleportCauses)
		);
	}

	@OverridePriority
	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		if (!teleportCauses.isEmpty() && !teleportCauses.contains(event.getCause())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
