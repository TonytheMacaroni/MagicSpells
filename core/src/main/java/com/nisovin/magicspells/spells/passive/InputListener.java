package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.EnumSet;
import java.util.function.Predicate;

import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.conversion.*;
import com.nisovin.magicspells.util.InputPredicate;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("input")
public class InputListener extends PassiveListener {

	private final Set<InputType> onPress = EnumSet.noneOf(InputType.class);
	private final Set<InputType> onRelease = EnumSet.noneOf(InputType.class);

	private InputPredicate oldInputPredicate;
	private InputPredicate newInputPredicate;

	@Override
	public void initialize(@NotNull String var) {
		MagicDebug.warn("The 'damage' trigger does not have a string format: %s", MagicDebug.resolveFullPath());
	}

	@Override
	public boolean initialize(@NotNull ConfigurationSection config) {
		if (initInputTypes(config, "on-press", onPress)) return false;
		if (initInputTypes(config, "on-release", onRelease)) return false;

		oldInputPredicate = InputPredicate.fromConfig(config, "old-input");
		if (oldInputPredicate == null && config.isSet("old-input")) return false;

		newInputPredicate = InputPredicate.fromConfig(config, "new-input");
		if (newInputPredicate == null && config.isSet("new-input")) return false;

		return true;
	}

	private static boolean initInputTypes(ConfigurationSection config, String path, Set<InputType> inputTypes) {
		ConversionResult<Void> result = Conversion.<Object, InputType, Void>single()
			.source(ConversionSource.listFromConfig(config, path))
			.converter(Converters.enumConverter(InputType.class))
			.target(ConversionTarget.addTo(inputTypes))
			.invalidateOnError()
			.convert();

		return result.isInvalid();
	}

	@OverridePriority
	@EventHandler
	public void onInput(PlayerInputEvent event) {
		Player caster = event.getPlayer();
		if (!canTrigger(caster)) return;

		Input oldInput = caster.getCurrentInput();
		if (oldInputPredicate != null && !oldInputPredicate.test(oldInput)) return;

		Input newInput = event.getInput();
		if (newInputPredicate != null && !newInputPredicate.test(newInput)) return;

		trigger_check:
		if (!onPress.isEmpty() || !onRelease.isEmpty()) {
			for (InputType type : onPress)
				if (!type.isPressed(oldInput) && type.isPressed(newInput))
					break trigger_check;

			for (InputType type : onRelease)
				if (type.isPressed(oldInput) && !type.isPressed(newInput))
					break trigger_check;

			return;
		}

		passiveSpell.activate(caster);
	}

	private enum InputType {

		FORWARD(Input::isForward),
		BACKWARD(Input::isBackward),
		LEFT(Input::isLeft),
		RIGHT(Input::isRight),
		JUMP(Input::isJump),
		SNEAK(Input::isSneak),
		SPRINT(Input::isSprint);

		private final Predicate<Input> predicate;

		InputType(Predicate<Input> predicate) {
			this.predicate = predicate;
		}

		public boolean isPressed(Input input) {
			return predicate.test(input);
		}

	}

}
