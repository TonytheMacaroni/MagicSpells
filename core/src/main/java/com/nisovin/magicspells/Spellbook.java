package com.nisovin.magicspells;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.handlers.MagicXpHandler;
import com.nisovin.magicspells.events.SpellSelectionChangeEvent;
import com.nisovin.magicspells.events.SpellSelectionChangedEvent;

public class Spellbook {

	private Player player;

	private final Set<Spell> spells = new HashSet<>();
	private final Set<String> cantLearn = new HashSet<>();
	private final Map<CastItem, ItemBindings> itemBindings = new HashMap<>();
	private final Multimap<String, Spell> temporarySpells = HashMultimap.create();

	public Spellbook(Player player) {
		this.player = player;

		load();
	}

	public void destroy() {
		removeAllSpells();

		player = null;
	}

	public void load() {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Loading spellbook for player '%s'.", player.getName())) {
			MagicSpells.getStorageHandler().load(this);
			addGrantedSpells();
			loadDefaultBindings();
		}
	}

	public void save() {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Saving spellbook for player '%s'.", player.getName())) {
			MagicSpells.getStorageHandler().save(this);
		}
	}

	public void reload() {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Reloading spellbook for player '%s'.", player.getName())) {
			removeAllSpells();
			load();
			save();
		}
	}

	public void addGrantedSpells() {
		boolean grantAllByDefault = MagicSpells.ignoreGrantPerms() && MagicSpells.ignoreGrantPermsFakeValue();
		if (grantAllByDefault || player.isOp() && MagicSpells.grantOpsAllSpells()) {
			try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Granting all spells - %s.", grantAllByDefault ? "all spells are granted by default" : "player is opped")) {
				boolean added = false;

				for (Spell spell : MagicSpells.getSpellsOrdered()) {
					try (var ignored1 = MagicDebug.section(spell, "Attempting to grant spell '%s'.", spell.getInternalName())) {
						if (spell.isHelperSpell()) {
							MagicDebug.info("Skipping spell '%s' - helper spells cannot be granted.", spell.getInternalName());
							continue;
						}

						if (spells.contains(spell)) {
							MagicDebug.info("Skipping spell '%s' - player already has the spell.", spell.getInternalName());
							continue;
						}

						addSpell(spell);
						added = true;
					}
				}

				if (added) save();

				return;
			}
		}

		if (MagicSpells.ignoreGrantPerms()) return;

		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Adding granted spells.")) {
			boolean added = false;

			for (Spell spell : MagicSpells.getSpellsOrdered()) {
				try (var ignored1 = MagicDebug.section(spell, "Attempting to grant spell '%s'.", spell.getInternalName())) {
					if (!spell.isAlwaysGranted() && !Perm.GRANT.has(player, spell)) {
						MagicDebug.info("Skipping spell '%s' - player does not have the required grant permission.", spell.getInternalName());
						continue;
					}

					if (spell.isHelperSpell()) {
						MagicDebug.info("Skipping spell '%s' - helper spells cannot be granted.", spell.getInternalName());
						continue;
					}

					if (hasSpell(spell, false)) {
						MagicDebug.info("Skipping spell '%s' - player already has the spell.", spell.getInternalName());
						continue;
					}

					addSpell(spell);
					added = true;
				}
			}

			if (added) save();
		}
	}

	public boolean canLearn(Spell spell) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Checking if player '%s' can learn spell '%s'.", player.getName(), spell.getInternalName())) {
			if (spell.isHelperSpell()) {
				MagicDebug.info("Spell is a helper spell and cannot be learned - check failed.");
				return false;
			}

			if (cantLearn.contains(spell.getInternalName())) {
				MagicDebug.info("Spell cannot be learned as another spell prevents it - check failed.");
				return false;
			}

			List<String> prerequisites = spell.getPrerequisites();
			if (prerequisites != null && !prerequisites.isEmpty()) {
				for (String spellName : prerequisites) {
					Spell prerequisite = MagicSpells.getSpellByInternalName(spellName);
					if (prerequisite == null) {
						MagicDebug.info("Cannot learn as invalid prerequisite spell '%s' is listed - check failed.", spellName);
						return false;
					}

					if (!hasSpell(prerequisite)) {
						MagicDebug.info("Cannot learn as player does not have prerequisite spell '%s' - check failed.", spellName);
						return false;
					}
				}
			}

			MagicXpHandler handler = MagicSpells.getMagicXpHandler();
			if (handler != null) {
				Map<String, Integer> xpRequired = spell.getXpRequired();

				if (xpRequired != null && !xpRequired.isEmpty()) {
					for (String school : xpRequired.keySet()) {
						int xp = handler.getXp(player, school);
						int requiredXp = xpRequired.get(school);

						if (xp < requiredXp) {
							MagicDebug.info("Cannot learn as player does not have enough magic xp in the '%s' school ('%d' < '%d') - check failed.", school, xp, requiredXp);
							return false;
						}
					}
				}
			}

			if (!Perm.LEARN.has(player, spell)) {
				MagicDebug.info("Player does not have the required learn permission '%s' - check failed.", Perm.LEARN.getNode(spell));
				return false;
			}

			MagicDebug.info("Player has the required learn permission '%s' - check passed.", Perm.LEARN.getNode(spell));
			return true;
		}
	}

	public boolean canCast(Spell spell) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Checking if '%s' can cast spell '%s'.", player.getName(), spell.getInternalName())) {
			if (MagicSpells.hasCastPermsIgnored()) {
				MagicDebug.info("Cast perms are being ignored - check passed.");
				return true;
			}

			if (spell.isHelperSpell()) {
				MagicDebug.info("Spell is a helper spell - check passed.");
				return true;
			}

			if (Perm.CAST.has(player, spell)) {
				MagicDebug.info("Player has the required cast permission '%s' - check passed.", Perm.CAST.getNode(spell));
				return true;
			}

			MagicDebug.info("Player does not have the required cast permission '%s' - check failed.", Perm.CAST.getNode(spell));
			return false;
		}
	}

	public boolean canTeach(Spell spell) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Checking if '%s' can teach spell '%s'.", player.getName(), spell.getInternalName())) {
			if (spell.isHelperSpell()) {
				MagicDebug.info("Spell is a helper spell - check failed.");
				return false;
			}

			if (!Perm.TEACH.has(player, spell)) {
				MagicDebug.info("Player does not have the required teach permission '%s' - check failed.", Perm.TEACH.getNode(spell));
				return false;
			}

			MagicDebug.info("Player has the required teach permission '%s' - check passed.", Perm.TEACH.getNode(spell));
			return true;
		}
	}

	public boolean hasAdvancedPerm(String spell) {
		String permission = Perm.ADVANCED.getNode() + spell;

		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Checking if '%s' has the advanced permission '%s'.", player.getName(), permission)) {
			if (player.hasPermission(permission)) {
				MagicDebug.info("Player has the required advanced permission '%s' - check passed.", permission);
				return true;
			}

			MagicDebug.info("Player does not have the required advanced permission '%s' - check failed.", permission);
			return false;
		}
	}

	public void addSpell(@NotNull Spell spell) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Adding spell '%s' to spellbook for player '%s'.", spell.getInternalName(), player.getName())) {
			spells.add(spell);

			spell.initializePlayerEffectTracker(player);

			// Remove any spells that this spell replaces
			List<String> replaces = spell.getReplaces();
			if (replaces != null) {
				for (String spellName : replaces) {
					Spell toReplace = MagicSpells.getSpellByInternalName(spellName);
					if (toReplace == null) {
						MagicDebug.info("Skipping invalid spell to replace '%s'.", spellName);
						continue;
					}

					MagicDebug.info("Replacing spell '%s'.", spellName);
					removeSpell(toReplace);
				}
			}

			// Prevent learning of spells this spell precludes
			List<String> precludes = spell.getPrecludes();
			if (precludes != null) {
				for (String preclude : precludes) {
					MagicDebug.info("Precluding spell '%s'.", preclude);
					cantLearn.add(preclude.toLowerCase());
				}
			}
		}
	}

	public void addCustomBinding(@NotNull CastItem item, @NotNull Spell spell) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Adding a custom binding for spell '%s' to cast item '%s'.", spell.getInternalName(), item)) {
			ItemBindings bindings = itemBindings.computeIfAbsent(item, i -> new ItemBindings());
			bindings.addCustomBinding(spell);
		}
	}

	public boolean removeCustomBinding(@NotNull CastItem item, @NotNull Spell spell) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Removing a custom binding for spell '%s' from cast item '%s'.", spell.getInternalName(), item)) {
			ItemBindings bindings = itemBindings.get(item);
			if (bindings != null && bindings.removeCustomBinding(spell)) {
				if (bindings.isEmpty()) itemBindings.remove(item);

				MagicDebug.info("Binding removed.");
				return true;
			}

			MagicDebug.info("Spell is not bound to the item.");
			return false;
		}
	}

	public void removeCustomBindings(@NotNull CastItem item) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Removing custom bindings from cast item '%s'.", item)) {
			ItemBindings bindings = itemBindings.get(item);
			if (bindings == null || !bindings.hasCustomBindings()) {
				MagicDebug.info("No custom bindings found.");
				return;
			}

			bindings.removeCustomBindings();
			if (bindings.isEmpty()) itemBindings.remove(item);
		}
	}

	@ApiStatus.Internal
	public void setCustomBindings(@NotNull CastItem item, @NotNull Collection<@NotNull Spell> spells) {
		if (spells.isEmpty()) return;

		try (var ignored = MagicDebug.section(
			DebugCategory.SPELLBOOK,
			"Setting custom bindings for cast item '%s' to spells %s.",
			item,
			(Supplier<String>) () -> spells.stream()
				.map(Spell::getInternalName)
				.collect(Collectors.joining("['", "', '", "']"))
		)) {
			ItemBindings bindings = itemBindings.compute(item, (i, b) -> new ItemBindings());
			bindings.customBindings.addAll(spells);
			bindings.computeBindings();
		}
	}

	@ApiStatus.Internal
	public void sortCustomBindings() {
		if (MagicSpells.isSortingCustomBindings()) return;

		itemBindings.values().forEach(bindings -> {
			Collections.sort(bindings.customBindings);
			bindings.computeBindings();
		});
	}

	private void loadDefaultBindings() {
		if (MagicSpells.ignoreDefaultBindings()) return;

		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Adding default bindings to spellbook.")) {
			Multimap<CastItem, Spell> defaultBindings = HashMultimap.create();
			for (Spell spell : spells) {
				try (var ignored1 = MagicDebug.section(spell, "Checking cast items for spell '%s'.", spell.getInternalName())) {
					for (CastItem item : spell.getCastItems()) {
						if (item == null) {
							MagicDebug.info("No cast items found.");
							continue;
						}

						MagicDebug.info("Adding default bound spell '%s' to item '%s'.", spell.getInternalName(), item);
						defaultBindings.put(item, spell);
					}
				}
			}

			for (Map.Entry<CastItem, Collection<Spell>> entry : defaultBindings.asMap().entrySet()) {
				Collection<Spell> boundSpells = entry.getValue();
				CastItem item = entry.getKey();

				ItemBindings bindings = itemBindings.computeIfAbsent(item, i -> new ItemBindings());
				bindings.defaultBindings.addAll(boundSpells);
				bindings.computeBindings();
			}
		}
	}

	public void removeSpell(Spell spell) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Removing spell '%s' from spellbook of player '%s'.", spell.getInternalName(), player.getName())) {
			spells.remove(spell);

			spell.unloadPlayerEffectTracker(player);
			if (spell instanceof BuffSpell buffSpell) buffSpell.turnOff(player);

			itemBindings.values().removeIf(bindings -> bindings.removeSpell(spell) && bindings.isEmpty());
		}
	}

	public boolean hasSpell(Spell spell) {
		return hasSpell(spell, true);
	}

	public boolean hasSpell(Spell spell, boolean checkGranted) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, spell, "Checking if player '%s' has spell '%s'.", player.getName(), spell.getInternalName())) {
			if (MagicSpells.ignoreGrantPerms() && MagicSpells.ignoreGrantPermsFakeValue()) {
				MagicDebug.info("Ignoring grant permissions, and using the default value of 'true' - check passed.");
				return true;
			}

			if (spells.contains(spell)) {
				MagicDebug.info("Spell is contained within the spellbook - check passed.");
				return true;
			}

			if (checkGranted && !MagicSpells.ignoreGrantPerms() && Perm.GRANT.has(player, spell)) {
				MagicDebug.info("Player has the appropriate grant permission - check passed.");
				addSpell(spell);
				return true;
			}

			if (MagicSpells.areTempGrantPermsEnabled() && Perm.TEMPGRANT.has(player, spell)) {
				MagicDebug.info("Player has the appropriate temporary grant permission - check passed.");
				return true;
			}

			MagicDebug.info("Player does not have the spell - check failed.");
			return false;
		}
	}

	public Spell nextSpell(@Nullable ItemStack item) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Retrieving the next selected spell for player '%s'.", player.getName())) {
			CastItem castItem = item == null || item.isEmpty() ? CastItem.AIR : new CastItem(item);
			MagicDebug.info("Checking bindings for cast item '%s'.", castItem);

			ItemBindings binds = itemBindings.get(castItem);
			if (binds == null) {
				MagicDebug.info("No bindings found.");
				return null;
			}

			List<Spell> spells = binds.bindings;
			int selected = binds.selectedSlot;
			if (spells.size() <= 1 && selected != -1 && !MagicSpells.canCycleToNoSpell()) {
				MagicDebug.info("Item only has a single binding, and cannot cycle to no spell.");
				return null;
			}

			int count = 0;
			while (count++ < spells.size()) {
				selected++;

				if (selected >= spells.size()) {
					if (MagicSpells.canCycleToNoSpell()) {
						MagicDebug.info("Attempting to cycle to no spell.");

						SpellSelectionChangeEvent event = new SpellSelectionChangeEvent(null, player, castItem, this);
						if (!event.callEvent()) {
							MagicDebug.info("Cycle cancelled.");
							return null;
						}

						binds.selectedSlot = -1;
						new SpellSelectionChangedEvent(null, player, castItem, this).callEvent();
						MagicSpells.sendMessage(player, MagicSpells.getSpellChangeEmptyMessage());

						MagicDebug.info("Cycled to no spell.");
						return null;
					} else selected = 0;
				}

				Spell spell = spells.get(selected);
				MagicDebug.info("Attempting to cycle to spell '%s'.", spell.getInternalName());

				if (!MagicSpells.cycleToCastableSpells() || canCast(spell)) {
					SpellSelectionChangeEvent event = new SpellSelectionChangeEvent(null, player, castItem, this);
					if (!event.callEvent()) {
						MagicDebug.info("Cycle cancelled.");
						return null;
					}

					binds.selectedSlot = selected;
					new SpellSelectionChangedEvent(spell, player, castItem, this).callEvent();

					MagicDebug.info("Cycled to spell '%s'.", spell.getInternalName());
					return spell;
				}

				MagicDebug.info("Cannot cast spell '%s'. Skipping.", spell.getInternalName());
			}

			MagicDebug.info("Could not find a spell to cycle to.");
			return null;
		}
	}

	public Spell prevSpell(@Nullable ItemStack item) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Retrieving the previous selected spell for player '%s'.", player.getName())) {
			CastItem castItem = item == null || item.isEmpty() ? CastItem.AIR : new CastItem(item);
			MagicDebug.info("Checking bindings for cast item '%s'.", castItem);

			ItemBindings binds = itemBindings.get(castItem);
			if (binds == null) {
				MagicDebug.info("No bindings found.");
				return null;
			}

			List<Spell> spells = binds.bindings;
			int selected = binds.selectedSlot;
			if (spells.size() <= 1 && selected != -1 && !MagicSpells.canCycleToNoSpell()) {
				MagicDebug.info("Item only has a single binding, and cannot cycle to no spell.");
				return null;
			}

			int count = 0;
			while (count++ < spells.size()) {
				selected--;

				if (selected < 0) {
					if (MagicSpells.canCycleToNoSpell() && selected == -1) {
						MagicDebug.info("Attempting to cycle to no spell.");

						SpellSelectionChangeEvent event = new SpellSelectionChangeEvent(null, player, castItem, this);
						if (!event.callEvent()) {
							MagicDebug.info("Cycle cancelled.");
							return null;
						}

						binds.selectedSlot = -1;
						new SpellSelectionChangedEvent(null, player, castItem, this).callEvent();
						MagicSpells.sendMessage(player, MagicSpells.getSpellChangeEmptyMessage());

						MagicDebug.info("Cycled to no spell.");
						return null;
					} else selected = spells.size() - 1;
				}

				Spell spell = spells.get(selected);
				MagicDebug.info("Attempting to cycle to spell '%s'.", spell.getInternalName());

				if (!MagicSpells.cycleToCastableSpells() || canCast(spell)) {
					SpellSelectionChangeEvent event = new SpellSelectionChangeEvent(null, player, castItem, this);
					if (!event.callEvent()) {
						MagicDebug.info("Cycle cancelled.");
						return null;
					}

					binds.selectedSlot = selected;
					new SpellSelectionChangedEvent(spell, player, castItem, this).callEvent();

					MagicDebug.info("Cycled to spell '%s'.", spell.getInternalName());
					return spell;
				}

				MagicDebug.info("Cannot cast spell '%s'. Skipping.", spell.getInternalName());
			}

			MagicDebug.info("Could not find a spell to cycle to.");
			return null;
		}
	}

	public Spell getActiveSpell(@Nullable ItemStack item) {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Retrieving currently selected spell for player '%s'.", player.getName())) {
			CastItem castItem = item == null || item.isEmpty() ? CastItem.AIR : new CastItem(item);
			MagicDebug.info("Checking bindings for cast item '%s'.", item);

			ItemBindings binds = itemBindings.get(castItem);
			if (binds == null) {
				MagicDebug.info("No bindings found.");
				return null;
			}

			if (binds.selectedSlot == -1) {
				MagicDebug.info("No spell currently selected.");
				return null;
			}

			Spell spell = binds.bindings.get(binds.selectedSlot);
			MagicDebug.info("Spell '%s' is currently selected.", spell.getInternalName());

			return spell;
		}
	}

	@Nullable
	public ItemBindings getBindings(@Nullable ItemStack item) {
		CastItem castItem = item == null || item.isEmpty() ? CastItem.AIR : new CastItem(item);
		return getBindings(castItem);
	}

	@Nullable
	public ItemBindings getBindings(@NotNull CastItem item) {
		return itemBindings.get(item);
	}

	public void addTemporarySpell(Spell spell, Plugin plugin) {
		if (hasSpell(spell)) return;

		addSpell(spell);
		temporarySpells.put(plugin.getName(), spell);
	}

	public void removeTemporarySpells(Plugin plugin) {
		Collection<Spell> spells = temporarySpells.removeAll(plugin.getName());
		for (Spell spell : spells) removeSpell(spell);
	}

	public boolean isTemporary(Spell spell) {
		return temporarySpells.containsValue(spell);
	}

	public void removeAllSpells() {
		try (var ignored = MagicDebug.section(DebugCategory.SPELLBOOK, "Removing all spells from spellbook of player '%s'.", player.getName())) {
			for (Spell spell : spells) {
				spell.unloadPlayerEffectTracker(player);
				if (spell instanceof BuffSpell buffSpell) buffSpell.turnOff(player);
			}

			spells.clear();
			cantLearn.clear();
			itemBindings.clear();
			temporarySpells.clear();
		}
	}

	public Player getPlayer() {
		return player;
	}

	public Collection<Spell> getSpells() {
		return Collections.unmodifiableCollection(spells);
	}

	public Collection<String> getCantLearn() {
		return Collections.unmodifiableCollection(cantLearn);
	}

	public Map<CastItem, ItemBindings> getItemBindings() {
		return Collections.unmodifiableMap(itemBindings);
	}

	@Override
	public String toString() {
		return "Spellbook{" +
			"player=" + player +
			", spells=" + spells +
			", cantLearn=" + cantLearn +
			", bindings=" + itemBindings +
			", temporarySpells=" + temporarySpells +
			'}';
	}

	public static class ItemBindings {

		private final List<Spell> bindings;
		private final List<Spell> customBindings;
		private final List<Spell> defaultBindings;

		private int selectedSlot;

		private ItemBindings() {
			bindings = new ArrayList<>();
			customBindings = new ArrayList<>();
			defaultBindings = new ArrayList<>();

			selectedSlot = MagicSpells.canCycleToNoSpell() ? -1 : 0;
		}

		public int getSelectedSlot() {
			return selectedSlot;
		}

		@Nullable
		public Spell getSelectedSpell() {
			return selectedSlot == -1 ? null : bindings.get(selectedSlot);
		}

		@NotNull
		public List<@NotNull Spell> getBindings() {
			return Collections.unmodifiableList(bindings);
		}

		@NotNull
		public List<@NotNull Spell> getDefaultBindings() {
			return Collections.unmodifiableList(defaultBindings);
		}

		@NotNull
		public List<@NotNull Spell> getCustomBindings() {
			return Collections.unmodifiableList(customBindings);
		}

		public boolean hasDefaultBindings() {
			return !defaultBindings.isEmpty();
		}

		public boolean hasCustomBindings() {
			return !customBindings.isEmpty();
		}

		public boolean isEmpty() {
			return bindings.isEmpty();
		}

		private void addCustomBinding(@NotNull Spell spell) {
			customBindings.add(spell);

			if (!MagicSpells.isSortingCustomBindings()) {
				bindings.add(spell);
				return;
			}

			int index = Collections.binarySearch(bindings, spell);
			if (index < 0) {
				index = -index - 1;
				bindings.add(index, spell);
				if (index <= selectedSlot) selectedSlot++;
				return;
			}

			while (++index < bindings.size() && spell.equals(bindings.get(index)));

			bindings.add(index, spell);
			if (index <= selectedSlot) selectedSlot++;
		}

		private boolean removeCustomBinding(@NotNull Spell spell) {
			int index = customBindings.indexOf(spell);
			if (index == -1) return false;

			customBindings.remove(index);

			if (!MagicSpells.isSortingCustomBindings()) {
				bindings.remove(index + defaultBindings.size());
				return true;
			}

			index = bindings.indexOf(spell);
			index += Collections.frequency(defaultBindings, spell);

			bindings.remove(index);

			if (index == selectedSlot) selectedSlot = MagicSpells.canCycleToNoSpell() ? -1 : 0;
			else if (index < selectedSlot) selectedSlot--;

			return true;
		}

		private void removeCustomBindings() {
			customBindings.clear();

			// Trivial case - no selected spell.
			if (selectedSlot == -1) {
				bindings.clear();
				bindings.addAll(defaultBindings);
				return;
			}

			// Reset selected spell if index indicates it was a custom binding.
			if (!MagicSpells.isSortingCustomBindings()) {
				bindings.subList(defaultBindings.size(), bindings.size()).clear();
				if (selectedSlot >= bindings.size()) selectedSlot = MagicSpells.canCycleToNoSpell() ? -1 : 0;
				return;
			}

			Spell selectedSpell = bindings.get(selectedSlot);

			// If present not present in default bindings, reset selected spell. Else, if it is present but the selected
			// spell is at index 0, the current selected spell must be from the default bindings.
			int index = defaultBindings.indexOf(selectedSpell);
			if (index == -1 || selectedSlot == 0) {
				bindings.clear();
				bindings.addAll(defaultBindings);
				selectedSlot = index == -1 && !MagicSpells.canCycleToNoSpell() ? 0 : index;
				return;
			}

			// If there is a duplicate prior to the currently selected spell, reset, as the current selected spell must
			// be from the custom bindings.
			if (selectedSpell.equals(bindings.get(selectedSlot - 1))) {
				bindings.clear();
				bindings.addAll(defaultBindings);
				selectedSlot = MagicSpells.canCycleToNoSpell() ? -1 : 0;
				return;
			}

			// Otherwise, the currently selected spell is either does not have a custom binding duplicate, or the
			// currently selected spell is from a default binding.
			bindings.clear();
			bindings.addAll(defaultBindings);
			selectedSlot = index;
		}

		private boolean removeSpell(@NotNull Spell spell) {
			if (!defaultBindings.removeIf(spell::equals) && !customBindings.removeIf(spell::equals)) return false;

			Iterator<Spell> it = bindings.iterator();
			int index = 0;
			while (it.hasNext()) {
				if (!spell.equals(it.next())) {
					index++;
					continue;
				}

				it.remove();

				if (selectedSlot != -1) {
					if (index < selectedSlot) selectedSlot--;
					else if (index == selectedSlot) selectedSlot = -1;
				}
			}

			return true;
		}

		private void computeBindings() {
			defaultBindings.sort(null);

			bindings.clear();
			bindings.addAll(defaultBindings);
			bindings.addAll(customBindings);

			if (MagicSpells.isSortingCustomBindings()) bindings.sort(null);
		}

	}

}
