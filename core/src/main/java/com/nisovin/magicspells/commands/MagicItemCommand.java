package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

@SuppressWarnings("UnstableApiUsage")
public class MagicItemCommand {

	private static final CloudKey<MultiplePlayerSelector> TARGET_PLAYERS_KEY = CloudKey.of("player", MultiplePlayerSelector.class);
	private static final CloudKey<String> MAGIC_ITEM_KEY = CloudKey.of("magic item", String.class);
	private static final CloudKey<Integer> AMOUNT_KEY = CloudKey.of("amount", Integer.class);

	private static final CommandFlag<Void> DROP_LEFTOVER_FLAG = CommandFlag.builder("drop-leftover")
		.withDescription(Description.of("If used, leftover items will be dropped on the ground."))
		.withAliases("d")
		.build();

	static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		manager.command(manager.commandBuilder("ms", "magicspells")
			.literal("magicitem")
			.required(
				TARGET_PLAYERS_KEY,
				MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false),
				Description.of("Selector for the player(s) to give the magic item to.")
			)
			.required(
				MAGIC_ITEM_KEY,
				StringParser.stringParser(),
				Description.of("The magic item to give."),
				SuggestionProvider.suggestingStrings(MagicItems.getMagicItemKeys())
			)
			.optional(
				AMOUNT_KEY,
				IntegerParser.integerParser(),
				Description.of("The amount of the magic item to give.")
			)
			.flag(DROP_LEFTOVER_FLAG)
			.commandDescription(Description.of("Give a magic item to players."))
			.permission(Perm.COMMAND_MAGIC_ITEM)
			.handler(MagicItemCommand::magicItem)
		);
	}

	private static void magicItem(CommandContext<CommandSourceStack> context) {
		String magicItemString = context.get(MAGIC_ITEM_KEY);

		MagicItem magicItem = MagicItems.getMagicItemByInternalName(magicItemString);
		if (magicItem == null)
			throw new InvalidCommandArgumentException("No matching magic item: '" + magicItemString + "'");

		boolean dropLeftOver = context.flags().isPresent(DROP_LEFTOVER_FLAG);
		Integer amount = context.getOrDefault(AMOUNT_KEY, null);

		MultiplePlayerSelector players = context.get(TARGET_PLAYERS_KEY);
		players.values().forEach(player -> {
			ItemStack item = magicItem.getItemStack().clone();
			if (amount != null) item.setAmount(amount);

			Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
			if (dropLeftOver && !leftover.isEmpty()) {
				Location location = player.getLocation();
				World world = location.getWorld();

				leftover.values().forEach(i -> world.dropItem(location, i));
			}
		});
	}

}
