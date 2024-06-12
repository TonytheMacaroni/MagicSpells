package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.List;

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

import org.bukkit.inventory.ItemStack;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.debug.MagicDebug;
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
				IntegerParser.integerParser(1, 99),
				Description.of("The amount of the magic item to give.")
			)
			.flag(DROP_LEFTOVER_FLAG)
			.commandDescription(Description.of("Give a magic item to players."))
			.permission(Perm.COMMAND_MAGIC_ITEM)
			.handler(MagicItemCommand::magicItem)
		);
	}

	private static void magicItem(CommandContext<CommandSourceStack> context) {
		String internalName = context.get(MAGIC_ITEM_KEY);

		try (var ignored = MagicDebug.section("Dropping magic item '%s'.", internalName)) {
			MagicItem magicItem = MagicItems.getMagicItemByInternalName(internalName);
			if (magicItem == null) {
				MagicDebug.info("No magic item with matching internal name '%s'.", internalName);
				throw new InvalidCommandArgumentException("No such magic item: '" + internalName + "'");
			}

			boolean dropLeftOver = context.flags().isPresent(DROP_LEFTOVER_FLAG);
			MagicDebug.info("Drop leftover? %b.", dropLeftOver);

			Integer amount = context.getOrDefault(AMOUNT_KEY, null);
			MagicDebug.info("Amount? %s.", amount == null ? "Default" : amount);

			if (amount != null && amount > magicItem.getItemStack().getMaxStackSize()) {
				MagicDebug.info("Amount '%s' exceeds the item's max stack size.", amount);
				throw new InvalidCommandArgumentException("Stack size too large: '" + amount + "'");
			}

			try (var ignored1 = MagicDebug.section("Giving items...")) {
				context.get(TARGET_PLAYERS_KEY).values().forEach(player -> {
					MagicDebug.info("Giving item to player '%s'.", player.getName());

					ItemStack item = magicItem.getItemStack().clone();
					if (amount != null) item.setAmount(amount);

					player.give(List.of(item), dropLeftOver);
				});
			}
		}
	}

}
