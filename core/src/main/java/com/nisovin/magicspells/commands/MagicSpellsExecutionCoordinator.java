package com.nisovin.magicspells.commands;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Entity;
import org.bukkit.command.CommandSender;

import org.incendo.cloud.CommandTree;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.Suggestions;
import org.incendo.cloud.execution.CommandResult;
import org.incendo.cloud.suggestion.SuggestionMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;

@SuppressWarnings("UnstableApiUsage")
public class MagicSpellsExecutionCoordinator implements ExecutionCoordinator<CommandSourceStack> {

	private final ExecutionCoordinator<CommandSourceStack> coordinator = ExecutionCoordinator.simpleCoordinator();

	@Override
	public @NonNull CompletableFuture<CommandResult<CommandSourceStack>> coordinateExecution(@NonNull CommandTree<CommandSourceStack> commandTree, @NonNull CommandContext<CommandSourceStack> context, @NonNull CommandInput input) {
		try (var ignored = MagicDebug.section(DebugCategory.COMMANDS, "Executing command '%s'.", input.input())) {
			CommandSourceStack stack = context.sender();

			CommandSender sender = stack.getSender();
			MagicDebug.info("Original command sender: %s", sender);

			Entity executor = stack.getExecutor();
			if (executor != null) MagicDebug.info("Current command executor: %s", executor);

			return coordinator.coordinateExecution(commandTree, context, input);
		}
	}

	@Override
	@NonNull
	public <S extends Suggestion> CompletableFuture<@NonNull Suggestions<CommandSourceStack, S>> coordinateSuggestions(@NonNull CommandTree<CommandSourceStack> commandTree, @NonNull CommandContext<CommandSourceStack> context, @NonNull CommandInput commandInput, @NonNull SuggestionMapper<S> mapper) {
		try (var ignored = MagicDebug.section(DebugCategory.COMMANDS, "Creating suggestions for input '%s'.", commandInput.input())) {
			CommandSourceStack stack = context.sender();

			CommandSender sender = stack.getSender();
			MagicDebug.info("Original command sender: %s", sender);

			Entity executor = stack.getExecutor();
			if (executor != null) MagicDebug.info("Current command executor: %s", executor);

			return coordinator.coordinateSuggestions(commandTree, context, commandInput, mapper);
		}
	}

}
