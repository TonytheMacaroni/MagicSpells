package com.nisovin.magicspells.commands;

import java.util.Map;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Optional;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import org.incendo.cloud.type.Either;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.flag.FlagContext;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.EitherParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.LiteralParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.incendo.cloud.parser.standard.CharacterParser;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.DebugLevel;
import com.nisovin.magicspells.debug.DebugConfig;
import com.nisovin.magicspells.debug.DebugCategory;

@SuppressWarnings("UnstableApiUsage")
public class DebugCommand {

	private static final CommandFlag<Void> RESET_FLAG = CommandFlag.builder("reset")
		.withDescription(Description.of("Resets to the configured debug settings."))
		.build();

	private static final CommandFlag<DebugLevel> LEVEL_FLAG = CommandFlag.builder("level")
		.withComponent(EnumParser.enumParser(DebugLevel.class))
		.withDescription(Description.of("Sets the default debug level."))
		.build();

	private static final Map<DebugCategory, CommandFlag<Either<DebugLevel, String>>> CATEGORY_FLAGS = Arrays.stream(DebugCategory.values())
		.collect(Collectors.toMap(
			category -> category,
			category -> CommandFlag
				.builder(category.name().toLowerCase().replace("_", "-"))
				.withComponent(EitherParser.eitherParser(
					EnumParser.enumParser(DebugLevel.class),
					LiteralParser.literal("unset")
				))
				.withDescription(Description.of("Sets the debug level of the '" + category + "' category."))
				.build()
		));

	private static final CommandFlag<Integer> INDENT_FLAG = CommandFlag.builder("indent")
		.withComponent(
			CommandComponent.builder("indent", IntegerParser.integerParser(1))
				.suggestionProvider(SuggestionProvider.noSuggestions())
		)
		.withDescription(Description.of("Sets the indentation level."))
		.build();

	private static final CommandFlag<Character> INDENT_CHARACTER_FLAG = CommandFlag.builder("indent-character")
		.withComponent(CharacterParser.characterParser())
		.withDescription(Description.of("Sets the indentation character."))
		.build();

	public static void register(PaperCommandManager<CommandSourceStack> manager) {
		manager.command(manager.commandBuilder("ms", "magicspells")
			.literal("debug")
			.flag(RESET_FLAG)
			.flag(LEVEL_FLAG)
			.flag(INDENT_FLAG)
			.flag(INDENT_CHARACTER_FLAG)
			.apply(builder -> {
				for (CommandFlag<Either<DebugLevel, String>> flag : CATEGORY_FLAGS.values())
					builder = builder.flag(flag);

				return builder;
			})
			.commandDescription(Description.of("Display current debug settings, as well as modify them using flags."))
			.permission(Perm.COMMAND_DEBUG)
			.handler(DebugCommand::debug)
		);
	}

	private static void debug(CommandContext<CommandSourceStack> context) {
		FlagContext flagContext = context.flags();

		if (flagContext.isPresent(RESET_FLAG))
			MagicSpells.setModifiedDebugConfig(null);

		DebugConfig config = MagicSpells.getDebugConfig();

		DebugLevel defaultLevel = flagContext.getValue(LEVEL_FLAG).orElse(config.getDefaultLevel());
		int indent = flagContext.getValue(INDENT_FLAG).orElse(config.getIndent());
		String indentCharacter = flagContext.getValue(INDENT_CHARACTER_FLAG).map(String::valueOf).orElse(config.getIndentCharacter());

		EnumMap<DebugCategory, DebugLevel> overrides = new EnumMap<>(config.getOverrides());
		for (Map.Entry<DebugCategory, CommandFlag<Either<DebugLevel, String>>> entry : CATEGORY_FLAGS.entrySet()) {
			Optional<Either<DebugLevel, String>> optional = flagContext.getValue(entry.getValue());
			if (optional.isEmpty()) continue;

			Optional<DebugLevel> level = optional.get().primary();
			if (level.isPresent()) overrides.put(entry.getKey(), level.get());
			else overrides.remove(entry.getKey());
		}

		MagicSpells.setModifiedDebugConfig(new DebugConfig(defaultLevel, overrides, indent, indentCharacter));

		config = MagicSpells.getDebugConfig();
		boolean modified = MagicSpells.hasModifiedDebugConfig();

		TextComponent.Builder message = Component.text()
			.content("Debug Config:\n")
			.color(MagicSpells.getTextColor())
			.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
			.append(Component.text("Modified - "))
			.append(Component.text(modified, modified ? NamedTextColor.GREEN : NamedTextColor.RED))
			.appendNewline()
			.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
			.append(Component.text("Indent size - " + indent))
			.appendNewline()
			.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
			.append(Component.text("Indent character - '" + indentCharacter + "'"))
			.appendNewline()
			.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
			.append(Component.text("Default debug level - "))
			.append(Component.text(
				defaultLevel.name(),
				switch (defaultLevel) {
					case NONE -> NamedTextColor.GRAY;
					case ERROR -> NamedTextColor.RED;
					case WARNING -> NamedTextColor.YELLOW;
					case INFO, ALL -> NamedTextColor.WHITE;
				}
			))
			.appendNewline()
			.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
			.append(Component.text("Overrides:"));

		overrides = config.getOverrides();
		if (overrides.isEmpty()) message.append(Component.text(" NONE", NamedTextColor.GRAY));
		else {
			for (Map.Entry<DebugCategory, DebugLevel> entry : overrides.entrySet()) {
				message
					.appendNewline()
					.append(Component.text("   - ", NamedTextColor.DARK_GRAY))
					.append(Component.text(entry.getKey().name() + " - "))
					.append(Component.text(
						entry.getValue().name(),
						switch (entry.getValue()) {
							case NONE -> NamedTextColor.GRAY;
							case ERROR -> NamedTextColor.RED;
							case WARNING -> NamedTextColor.YELLOW;
							case INFO, ALL -> NamedTextColor.WHITE;
						}
					));
			}
		}

		context.sender().getSender().sendMessage(message);
	}

}
