package com.nisovin.magicspells.commands;

import net.kyori.adventure.text.Component;

import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;

import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.MagicSpells;

@SuppressWarnings("UnstableApiUsage")
public class DebugCommand {

	private static final CloudKey<Integer> LEVEL_KEY = CloudKey.of("level", Integer.class);

	public static void register(PaperCommandManager<CommandSourceStack> manager) {
		manager.command(manager.commandBuilder("ms", "magicspells")
			.literal("debug")
			.optional(LEVEL_KEY, IntegerParser.integerParser())
			.permission(Perm.COMMAND_DEBUG)
			.handler(DebugCommand::debug)
		);
	}

	private static void debug(CommandContext<CommandSourceStack> context) {
		CommandSender sender = context.sender().getSender();

		if (MagicSpells.isDebug()) {
			MagicSpells.setDebug(false);
			MagicSpells.setDebugLevel(MagicSpells.getDebugLevelOriginal());
			sender.sendMessage(Component.text("MagicSpells debug mode disabled.", MagicSpells.getTextColor()));
			return;
		}

		int level = context.getOrDefault(LEVEL_KEY, MagicSpells.getDebugLevelOriginal());
		MagicSpells.setDebugLevel(level);
		MagicSpells.setDebug(true);
		sender.sendMessage(Component.text("MagicSpells debug mode enabled (level: " + level + ").", MagicSpells.getTextColor()));
	}


}
