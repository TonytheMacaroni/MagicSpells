package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import org.bukkit.Input;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.grammars.*;

@SuppressWarnings("UnstableApiUsage")
public class InputPredicate implements Predicate<Input> {

	private final Predicate<Input> predicate;

	public InputPredicate(Predicate<Input> predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean test(Input input) {
		return predicate.test(input);
	}

	@Nullable
	public static InputPredicate fromConfig(@NotNull ConfigurationSection config, @NotNull String path) {
		String inputString = config.getString(path, null);
		if (inputString == null) {
			MagicDebug.info("No input predicate at '%s'.", MagicDebug.resolveShortPath(config, path));
			return null;
		}

		try (var _ = MagicDebug.section("Resolving input predicate from string '%s'.", inputString)
			.pushPaths(config, path, DebugPath.Type.SCALAR)
		) {
			return fromString(inputString, false);
		}
	}

	public static InputPredicate fromString(@Nullable String inputString) {
		return fromString(inputString, true);
	}

	public static InputPredicate fromString(@Nullable String inputString, boolean ignoreEmpty) {
		if (inputString == null || inputString.isEmpty()) {
			if (!ignoreEmpty)
				MagicDebug.warn("No input predicate defined %s.", MagicDebug.resolveFullPath());

			return null;
		}

		try {
			InputPredicateLexer lexer = new InputPredicateLexer(CharStreams.fromString(inputString));
			lexer.removeErrorListeners();
			lexer.addErrorListener(GrammarUtils.LEXER_LISTENER);

			InputPredicateParser parser = new InputPredicateParser(new CommonTokenStream(lexer));
			parser.removeErrorListeners();
			parser.addErrorListener(GrammarUtils.PARSER_LISTENER);

			InputPredicateVisitorImpl visitor = new InputPredicateVisitorImpl();
			Predicate<Input> predicate = visitor.visit(parser.parse());

			return new InputPredicate(predicate);
		} catch (Exception e) {
			MagicDebug.warn(e, "Encountered an error while parsing input predicate '%s' %s.", inputString, MagicDebug.resolveFullPath());
			return null;
		}
	}

}
