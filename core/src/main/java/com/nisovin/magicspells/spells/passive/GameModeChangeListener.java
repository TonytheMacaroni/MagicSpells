package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("gamemodechange")
public class GameModeChangeListener extends PassiveListener {

	private final EnumSet<GameMode> gameModes = EnumSet.noneOf(GameMode.class);

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;

		Conversion.convert(
			ConversionSource.split(var, ","),
			Converters.enumConverter(GameMode.class),
			ConversionTarget.addTo(gameModes)
		);
	}

	@OverridePriority
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		if (!gameModes.isEmpty() && !gameModes.contains(event.getNewGameMode())) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
