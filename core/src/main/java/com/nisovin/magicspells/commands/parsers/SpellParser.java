package com.nisovin.magicspells.commands.parsers;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.function.Predicate;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.commands.exceptions.GenericCommandException;

public class SpellParser<C> implements ArgumentParser<C, Spell>, BlockingSuggestionProvider.Strings<C> {

	public static <C> ParserDescriptor<C, Spell> spellParser() {
		return ParserDescriptor.of(new SpellParser<>(), Spell.class);
	}

	private final StringParser<C> stringParser = new StringParser<>(StringParser.StringMode.QUOTED);

	@Override
	public @NonNull ArgumentParseResult<@NonNull Spell> parse(@NonNull CommandContext<@NonNull C> context, @NonNull CommandInput input) {
		ArgumentParseResult<String> result = stringParser.parse(context, input);

		return result.parsedValue()
			.map(MagicSpells::getSpellByName)
			.map(ArgumentParseResult::success)
			.orElseGet(() -> ArgumentParseResult.failure(new GenericCommandException(MagicSpells.getUnknownSpellMessage())));
	}

	@Override
	public @NonNull Iterable<@NonNull String> stringSuggestions(@NonNull CommandContext<C> commandContext, @NonNull CommandInput input) {
		return suggest();
	}

	public static <C> @NonNull List<@NonNull String> suggest() {
		return MagicSpells.spells().stream()
			.filter(Spell::canCastByCommand)
			.map(spell -> Util.getPlainString(Util.getMiniMessage(spell.getName())))
			.map(SpellParser::escapeIfRequired)
			.toList();
	}

	public static <C> @NonNull List<@NonNull String> suggest(@NonNull Predicate<Spell> predicate) {
		return MagicSpells.spells().stream()
			.filter(Spell::canCastByCommand)
			.filter(predicate)
			.map(spell -> Util.getPlainString(Util.getMiniMessage(spell.getName())))
			.map(SpellParser::escapeIfRequired)
			.toList();
	}

	public static String escapeIfRequired(@NonNull String string) {
		for (char c : string.toCharArray())
			if (shouldQuote(c))
				return '"' + string.replace("\"", "\\\"") + '"';

		return string;
	}

	private static boolean shouldQuote(char c) {
		return switch (c) {
			case ' ',  '"',  '\'',  '[',  ']',  '{',  '}' -> true;
			default -> false;
		};
	}

}
