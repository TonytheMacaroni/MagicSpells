package com.nisovin.magicspells.mana;

import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.castmodifiers.ModifierSet;

public class ManaSystem extends ManaHandler {

	private Component defaultBarPrefix;
	private char defaultSymbol;
	private int defaultBarSize;
	private TextColor defaultBarColorFull;
	private TextColor defaultBarColorEmpty;

	private int defaultMaxMana;
	private int defaultStartingMana;
	private int defaultRegenAmount;
	private int defaultRegenInterval;

	private boolean showManaOnUse;
	private boolean showManaOnRegen;
	private boolean showManaOnHungerBar;
	private boolean showManaOnActionBar;
	private boolean showManaOnExperienceBar;

	private ModifierSet modifiers;

	private ManaRank defaultRank;
	private List<ManaRank> ranks;

	private Map<UUID, ManaBar> manaBars;
	private Set<Regenerator> regenerators;

	public ManaSystem(MagicConfig config) {
		String path = "mana.";
		defaultBarPrefix = Util.getMiniMessage(config.getString(path + "default-prefix", "Mana:"));
		defaultSymbol = config.getString(path + "default-symbol", "=").charAt(0);
		defaultBarSize = config.getInt(path + "default-size", 35);
		defaultBarColorFull = Util.getColor(config.getString(path + "default-color-full", null), NamedTextColor.GREEN);
		defaultBarColorEmpty = Util.getColor(config.getString(path + "default-color-empty", null), NamedTextColor.BLACK);

		defaultMaxMana = config.getInt(path + "default-max-mana", 100);
		defaultStartingMana = config.getInt(path + "default-starting-mana", defaultMaxMana);
		defaultRegenAmount = config.getInt(path + "default-regen-amount", 5);
		defaultRegenInterval = config.getInt(path + "default-regen-interval", TimeUtil.TICKS_PER_SECOND);

		showManaOnUse = config.getBoolean(path + "show-mana-on-use", false);
		showManaOnRegen = config.getBoolean(path + "show-mana-on-regen", false);
		showManaOnHungerBar = config.getBoolean(path + "show-mana-on-hunger-bar", false);
		showManaOnActionBar = config.getBoolean(path + "show-mana-on-action-bar", false);
		showManaOnExperienceBar = config.getBoolean(path + "show-mana-on-experience-bar", true);

		defaultRank = new ManaRank("default", defaultBarPrefix, defaultSymbol, defaultBarSize, defaultMaxMana, defaultStartingMana, defaultRegenAmount, defaultRegenInterval, defaultBarColorFull, defaultBarColorEmpty);

		regenerators = new HashSet<>();
		ranks = new ArrayList<>();
		manaBars = new HashMap<>();

		Set<String> rankKeys = config.getKeys("mana.ranks");
		if (rankKeys != null) {
			for (String key : rankKeys) {
				String keyPath = "mana.ranks." + key + ".";

				String prefixString = config.getString(keyPath + "prefix", null);
				Component prefix = prefixString == null ? defaultBarPrefix : Util.getMiniMessage(prefixString);

				ManaRank r = new ManaRank();
				r.setName(key);
				r.setPrefix(prefix);
				r.setSymbol(config.getString(keyPath + "symbol", defaultSymbol + "").charAt(0));
				r.setBarSize(config.getInt(keyPath + "size", defaultBarSize));
				r.setMaxMana(config.getInt(keyPath + "max-mana", defaultMaxMana));
				r.setStartingMana(config.getInt(keyPath + "starting-mana", defaultStartingMana));
				r.setRegenAmount(config.getInt(keyPath + "regen-amount", defaultRegenAmount));
				r.setRegenInterval(config.getInt(keyPath + "regen-interval", defaultRegenAmount));
				r.setColorFull(Util.getColor(config.getString(keyPath + "color-full", null), defaultBarColorFull));
				r.setColorEmpty(Util.getColor(config.getString(keyPath + "color-empty", null), defaultBarColorEmpty));

				regenerators.add(new Regenerator(r, r.getRegenInterval()));

				ranks.add(r);
			}
		}

		regenerators.add(new Regenerator(defaultRank, defaultRegenInterval));
	}

	@Override
	public void initialize() {
		modifiers = ModifierSet.fromConfig(MagicSpells.getMagicConfig().getMainConfig(), "mana.modifiers");
	}

	// DEBUG INFO: level 1, creating mana bar for player playerName with rank rankName
	private ManaBar getManaBar(Player player) {
		ManaBar bar = manaBars.get(player.getUniqueId());
		if (bar == null) {
			// Create the mana bar
			ManaRank rank = getRank(player);
			bar = new ManaBar(player, rank);
			MagicSpells.debug(1, "Creating mana bar for player " + player.getName() + " with rank " + rank.getName());
			manaBars.put(player.getUniqueId(), bar);
		}
		return bar;
	}

	// DEBUG INFO: level 1, updating mana bar for player playerName with rank rankName
	@Override
	public void createManaBar(final Player player) {
		boolean update = manaBars.containsKey(player.getUniqueId());
		ManaBar bar = getManaBar(player);
		if (update) {
			ManaRank rank = getRank(player);
			if (rank != bar.getManaRank()) {
				MagicSpells.debug(1, "Updating mana bar for player " + player.getName() + " with rank " + rank.getName());
				bar.setRank(rank);
			}
		}
		MagicSpells.scheduleDelayedTask(() -> showMana(player), 11);
	}

	@Override
	public boolean updateManaRankIfNecessary(Player player) {
		if (manaBars.containsKey(player.getUniqueId())) {
			ManaBar bar = getManaBar(player);
			ManaRank rank = getRank(player);
			if (bar.getManaRank() != rank) {
				bar.setRank(rank);
				return true;
			}
		} else getManaBar(player);

		return false;
	}

	// DEBUG INFO: level 3, fetching mana rank for playerName
	// DEBUG INFO: level 3, checking rank rankName
	// DEBUG INFO: level 3, rank found
	// DEBUG INFO: level 3, no rank found
	private ManaRank getRank(Player player) {
		MagicSpells.debug(3, "Fetching mana rank for player " + player.getName() + "...");
		for (ManaRank rank : ranks) {
			MagicSpells.debug(3, "    checking rank " + rank.getName());
			if (player.hasPermission("magicspells.rank." + rank.getName())) {
				MagicSpells.debug(3, "    rank found");
				return rank;
			}
		}
		MagicSpells.debug(3, "    no rank found");
		return defaultRank;
	}

	@Override
	public int getMaxMana(Player player) {
		ManaBar bar = getManaBar(player);
		return bar.getMaxMana();
	}

	@Override
	public void setMaxMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		bar.setMaxMana(amount);
	}

	@Override
	public int getRegenAmount(Player player) {
		ManaBar bar = getManaBar(player);
		return bar.getRegenAmount();
	}

	@Override
	public void setRegenAmount(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		bar.setRegenAmount(amount);
	}

	@Override
	public int getMana(Player player) {
		ManaBar bar = getManaBar(player);
		return bar.getMana();
	}

	@Override
	public boolean hasMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		return bar.has(amount);
	}

	@Override
	public boolean addMana(Player player, int amount, ManaChangeReason reason) {
		ManaBar bar = getManaBar(player);
		boolean r = bar.changeMana(amount, reason);
		if (r) showMana(player, showManaOnUse);
		return r;
	}

	@Override
	public boolean removeMana(Player player, int amount, ManaChangeReason reason) {
		return addMana(player, -amount, reason);
	}

	@Override
	public boolean setMana(Player player, int amount, ManaChangeReason reason) {
		ManaBar bar = getManaBar(player);
		boolean r = bar.setMana(amount, reason);
		if (r) showMana(player, showManaOnUse);
		return r;
	}

	@Override
	public void showMana(Player player, boolean showInChat) {
		ManaBar bar = getManaBar(player);
		if (showInChat) showManaInChat(player, bar);
		if (showManaOnHungerBar) showManaOnHungerBar(player, bar);
		if (showManaOnActionBar) showManaOnActionBar(player, bar);
		if (showManaOnExperienceBar) showManaOnExperienceBar(player, bar);
	}

	@Override
	public ModifierSet getModifiers() {
		return modifiers;
	}

	private Component getManaMessage(ManaBar bar) {
		ManaRank rank = bar.getManaRank();

		int mana = bar.getMana();
		int maxMana = bar.getMaxMana();
		int barSize = rank.getBarSize();
		String symbol = String.valueOf(rank.getSymbol());

		double progress = (double) mana / maxMana;
		int segments = (int) (progress * barSize);

		return Component.text()
			.color(MagicSpells.getTextColor())
			.append(
				bar.getPrefix(),
				Component.text(" {"),
				Component.text(symbol.repeat(segments), bar.getColorFull()),
				Component.text(symbol.repeat(barSize - segments), bar.getColorEmpty()),
				Component.text("} ["),
				Component.text(mana),
				Component.text("/"),
				Component.text(maxMana),
				Component.text("]")
			)
			.build();
	}

	private void showManaInChat(Player player, ManaBar bar) {
		player.sendMessage(getManaMessage(bar));
	}

	private void showManaOnHungerBar(Player player, ManaBar bar) {
		int food = Math.round(((float) bar.getMana() / (float) bar.getMaxMana()) * 20);

		double health = player.isHealthScaled() ?
				player.getHealth() / Util.getMaxHealth(player) * player.getHealthScale() :
				player.getHealth();

		player.sendHealthUpdate(health, food, player.getSaturation());
	}

	private void showManaOnActionBar(Player player, ManaBar bar) {
		player.sendActionBar(getManaMessage(bar));
	}

	private void showManaOnExperienceBar(Player player, ManaBar bar) {
		MagicSpells.getExpBarManager().update(player, bar.getMana(), (float) bar.getMana() / (float) bar.getMaxMana());
	}

	public boolean usingHungerBar() {
		return showManaOnHungerBar;
	}

	public boolean usingActionBar() {
		return showManaOnActionBar;
	}

	public boolean usingExperienceBar() {
		return showManaOnExperienceBar;
	}

	@Override
	public void disable() {
		ranks.clear();
		manaBars.clear();

		for (Regenerator regenerator : regenerators) {
			MagicSpells.cancelTask(regenerator.taskId);
		}
		regenerators.clear();
	}

	private class Regenerator implements Runnable {

		private final ManaRank rank;

		private final int taskId;

		private Regenerator(ManaRank rank, int regenInterval) {
			this.rank = rank;
			taskId = MagicSpells.scheduleRepeatingTask(this, regenInterval, regenInterval);
		}

		@Override
		public void run() {
			Iterator<ManaBar> manaBarIterator = manaBars.values().iterator();
			ManaBar manaBar;
			Player player;
			while (manaBarIterator.hasNext()) {
				manaBar = manaBarIterator.next();
				if (!manaBar.getManaRank().equals(rank)) continue;

				if (!manaBar.regenerate()) continue;

				player = manaBar.getPlayer();
				if (player == null) continue;

				showMana(player, showManaOnRegen);
			}
		}

	}

}
