package com.nisovin.magicspells.handlers;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.text.NumberFormat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.IntMap;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

public class MagicXpHandler implements Listener {

	private MagicSpells plugin;

	private Map<String, String> schools = new HashMap<>();
	private Map<String, IntMap<String>> xp = new HashMap<>();
	private Map<String, String> currentWorld = new HashMap<>();
	private Map<String, List<Spell>> spellSchoolRequirements = new HashMap<>();

	private Set<String> dirty = new HashSet<>();

	private boolean autoLearn;
	private String strXpHeader;
	private String strNoXp;
	
	public MagicXpHandler(MagicSpells plugin, MagicConfig config) {
		this.plugin = plugin;
		
		Set<String> keys = config.getKeys("general.magic-schools");
		if (keys != null) {
			for (String school : keys) {
				String name = config.getString("general.magic-schools." + school, null);
				if (name != null) schools.put(school.toLowerCase(), name);
			}
		}
		autoLearn = config.getBoolean("general.magic-xp-auto-learn", false);
		strXpHeader = config.getString("general.str-xp-header", null);
		strNoXp = config.getString("general.str-no-xp", null);
		
		for (Spell spell : MagicSpells.spells()) {
			Map<String, Integer> xpRequired = spell.getXpRequired();
			if (xpRequired == null) continue;
			for (String school : xpRequired.keySet()) {
				List<Spell> list = spellSchoolRequirements.computeIfAbsent(school.toLowerCase(), s -> new ArrayList<>());
				list.add(spell);
			}
		}
		Util.forEachPlayerOnline(this::load);
		MagicSpells.scheduleRepeatingTask(this::saveAll, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
		MagicSpells.registerEvents(this);
	}
	
	public void showXpInfo(Player player) {
		MagicSpells.sendMessage(strXpHeader, player, MagicSpells.NULL_ARGS);
		IntMap<String> playerXp = xp.get(player.getName());
		if (playerXp == null || playerXp.isEmpty()) {
			MagicSpells.sendMessage(strNoXp, player, MagicSpells.NULL_ARGS);
			return;
		}
		for (String school : playerXp.keySet()) {
			String schoolName = schools.get(school);
			if (schoolName == null) continue;
			String amt = NumberFormat.getInstance().format(playerXp.get(school));
			MagicSpells.sendMessage(schoolName + ": " + amt, player, MagicSpells.NULL_ARGS);
		}
	}
	
	public int getXp(Player player, String school) {
		IntMap<String> playerXp = xp.get(player.getName());
		if (playerXp != null) return playerXp.get(school.toLowerCase());
		return 0;
	}
	
	@EventHandler
	public void onCast(SpellCastedEvent event) {
		if (event.getPostCastAction() == PostCastAction.ALREADY_HANDLED) return;
		if (event.getSpellCastState() != SpellCastState.NORMAL) return;
		
		final Map<String, Integer> xpGranted = event.getSpell().getXpGranted();
		if (xpGranted == null) return;

		// Get player xp
		IntMap<String> playerXp = xp.computeIfAbsent(event.getCaster().getName(), s -> new IntMap<>());
		
		// Grant xp
		// FIXME use entry set here
		for (String school : xpGranted.keySet()) {
			playerXp.increment(school.toLowerCase(), xpGranted.get(school));
		}

		dirty.add(event.getCaster().getName());

		if (!autoLearn) return;
		final LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player player)) return;

		final Spell castedSpell = event.getSpell();
		MagicSpells.scheduleDelayedTask(() -> {

			// Get spells to check if learned
			Set<Spell> toCheck = new HashSet<>();
			for (String school : xpGranted.keySet()) {
				List<Spell> list = spellSchoolRequirements.get(school.toLowerCase());
				if (list != null) toCheck.addAll(list);
			}

			// Check for new learned spells
			if (toCheck.isEmpty()) return;
			boolean learned = false;
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (Spell spell : toCheck) {
				if (!spellbook.hasSpell(spell, false) && spellbook.canLearn(spell)) {
					SpellLearnEvent evt = new SpellLearnEvent(spell, player, LearnSource.MAGIC_XP, castedSpell);
					EventUtil.call(evt);
					if (!evt.isCancelled()) {
						spellbook.addSpell(spell);
						MagicSpells.sendMessage(spell.getStrXpLearned(), player, MagicSpells.NULL_ARGS);
						learned = true;
					}
				}
			}
			if (learned) spellbook.save();

		}, 1);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		currentWorld.put(event.getPlayer().getName(), event.getPlayer().getWorld().getName());
		dirty.remove(event.getPlayer().getName());
		load(event.getPlayer());
	}

	@EventHandler
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		if (!MagicSpells.arePlayerSpellsSeparatedPerWorld()) return;
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (dirty.contains(playerName)) save(player);
		currentWorld.put(playerName, player.getWorld().getName());
		load(player);
		dirty.remove(playerName);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (dirty.contains(playerName)) save(player);
		xp.remove(playerName);
		dirty.remove(playerName);
		currentWorld.remove(playerName);
	}
	
	public void load(Player player) {
		File folder = new File(plugin.getDataFolder(), "xp");
		if (!folder.exists()) folder.mkdirs();
		if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
			String world = currentWorld.get(player.getName());
			if (world == null) world = player.getWorld().getName();
			folder = new File(folder, world);
			if (!folder.exists()) folder.mkdirs();
		}
		String uuid = Util.getUniqueId(player);
		File file = new File(folder, uuid + ".txt");
		if (!file.exists()) {
			File file2 = new File(folder, player.getName().toLowerCase());
			if (file2.exists()) file2.renameTo(file);
		}
		if (!file.exists()) return;
		YamlConfiguration conf = new YamlConfiguration();
		try {
			conf.load(file);
			IntMap<String> playerXp = new IntMap<>();
			for (String school : conf.getKeys(false)) {
				playerXp.put(school.toLowerCase(), conf.getInt(school, 0));
			}
			xp.put(player.getName(), playerXp);
		} catch (Exception e) {
			MagicDebug.warn(e, "Error while loading player XP for player '%s'.", player);
		}
	}
	
	public void saveAll() {
		for (String playerName : dirty) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) save(player);
		}
		dirty.clear();
	}
	
	public void save(Player player) {
		String world = currentWorld.get(player.getName());
		if (world == null) world = player.getWorld().getName();
		File folder = new File(plugin.getDataFolder(), "xp");
		if (!folder.exists()) folder.mkdirs();
		if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
			folder = new File(folder, world);
			if (!folder.exists()) folder.mkdirs();
		}
		File file = new File(folder, Util.getUniqueId(player) + ".txt");
		if (file.exists()) file.delete();
		
		YamlConfiguration conf = new YamlConfiguration();
		IntMap<String> playerXp = xp.get(player.getName());
		if (playerXp != null) {
			for (String school : playerXp.keySet()) {
				conf.set(school.toLowerCase(), playerXp.get(school));
			}
		}

		try {
			conf.save(file);
		} catch (Exception e) {
			MagicDebug.warn("Error while saving player XP for player '%s'.", player);
		}
	}
	
}
