package com.nisovin.magicspells.spells;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PassiveSpell extends Spell {

	private final List<PassiveListener> passiveListeners;
	private final List<String> spellNames;
	private List<Subspell> spells;

	private final ValidTargetList triggerList;

	private final ConfigData<Integer> delay;

	private final ConfigData<Float> chance;

	private boolean disabled = false;
	private final boolean ignoreCancelled;
	private final boolean castWithoutTarget;
	private final boolean sendFailureMessages;
	private final boolean cancelDefaultAction;
	private final boolean requireCancelledEvent;
	private final boolean cancelDefaultActionWhenCastFails;

	public PassiveSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		passiveListeners = new ArrayList<>();

		spellNames = getConfigStringList("spells", null);

		if (config.isList(internalKey + "can-trigger")) {
			List<String> defaultTargets = getConfigStringList("can-trigger", null);
			if (defaultTargets.isEmpty()) defaultTargets.add("players");
			triggerList = new ValidTargetList(this, defaultTargets);
		} else triggerList = new ValidTargetList(this, getConfigString("can-trigger", "players"));

		delay = getConfigDataInt("delay", -1);
		castTime = data -> 0;

		chance = getConfigDataFloat("chance", 100F);

		ignoreCancelled = getConfigBoolean("ignore-cancelled", true);
		castWithoutTarget = getConfigBoolean("cast-without-target", false);
		sendFailureMessages = getConfigBoolean("send-failure-messages", false);
		cancelDefaultAction = getConfigBoolean("cancel-default-action", false);
		requireCancelledEvent = getConfigBoolean("require-cancelled-event", false);
		cancelDefaultActionWhenCastFails = getConfigBoolean("cancel-default-action-when-cast-fails", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		// Create spell list
		spells = new ArrayList<>();
		Subspell spell;
		if (spellNames != null) {
			for (String spellName : spellNames) {
				spell = initSubspell(spellName, "PassiveSpell '" + internalName + "' has an invalid spell listed: " + spellName);
				if (spell == null) continue;
				spells.add(spell);
			}
		}

		if (spells.isEmpty()) MagicSpells.error("PassiveSpell '" + internalName + "' has no spells defined!");
	}

	@Override
	public void turnOff() {
		super.turnOff();

		for (PassiveListener listener : passiveListeners) {
			listener.turnOff();
			HandlerList.unregisterAll(listener);
		}
		passiveListeners.clear();
	}

	public void initializeListeners() {
		List<?> triggers = getConfigList("triggers", null);
		if (triggers == null) {
			MagicSpells.error("PassiveSpell '" + internalName + "' has no triggers defined!");
			return;
		}

		int count = 0;
		for (int i = 0; i < triggers.size(); i++) {
			Object trigger = triggers.get(i);

			switch (trigger) {
				case String string -> {
					String type, args;
					if (string.contains(" ")) {
						String[] data = Util.splitParams(string, 2);
						type = data[0].toLowerCase();
						args = data.length > 1 ? data[1] : "";
					} else {
						type = string.toLowerCase();
						args = "";
					}

					EventPriority priority = MagicSpells.getPassiveManager().getEventPriorityFromName(type);
					if (priority == null) priority = EventPriority.NORMAL;

					String priorityName = MagicSpells.getPassiveManager().getEventPriorityName(priority);
					if (priorityName != null) type = type.replace(priorityName, "");

					PassiveListener listener = MagicSpells.getPassiveManager().getListenerByName(type);
					if (listener == null) {
						MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid trigger type defined: " + type);
						continue;
					}

					listener.setPassiveSpell(this);
					listener.setEventPriority(priority);
					listener.initialize(args);
					MagicSpells.registerEvents(listener, priority);
					passiveListeners.add(listener);
					count++;
				}
				case Map<?, ?> map -> {
					ConfigurationSection config = ConfigReaderUtil.mapToSection(map);

					String type = config.getString("trigger");
					if (type == null) {
						MagicSpells.error("PassiveSpell '" + internalName + "' has no 'trigger' defined for trigger at position " + i + ".");
						continue;
					}

					PassiveListener listener = MagicSpells.getPassiveManager().getListenerByName(type);
					if (listener == null) {
						MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid trigger type defined: " + type);
						continue;
					}

					listener.setPassiveSpell(this);
					if (!listener.initialize(config)) {
						MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid trigger defined at position " + i + ".");
						continue;
					}

					EventPriority priority = EventPriority.NORMAL;

					String priorityString = config.getString("priority");
					if (priorityString != null) {
						try {
							priority = EventPriority.valueOf(priorityString.toUpperCase());
						} catch (IllegalArgumentException e) {
							MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid 'priority' defined: " + priorityString);
						}
					}

					listener.setEventPriority(priority);
					MagicSpells.registerEvents(listener, priority);
					passiveListeners.add(listener);
					count++;
				}
				default ->
					MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid trigger defined: " + trigger);
			}
		}

		if (count == 0) MagicSpells.error("PassiveSpell '" + internalName + "' has no triggers defined!");
	}

	public List<PassiveListener> getPassiveListeners() {
		return passiveListeners;
	}

	public List<Subspell> getActivatedSpells() {
		return spells;
	}

	public ValidTargetList getTriggerList() {
		return triggerList;
	}

	public boolean cancelDefaultAction() {
		return cancelDefaultAction;
	}

	public boolean cancelDefaultActionWhenCastFails() {
		return cancelDefaultActionWhenCastFails;
	}

	public boolean ignoreCancelled() {
		return ignoreCancelled;
	}

	public boolean requireCancelledEvent() {
		return requireCancelledEvent;
	}

	@Override
	public boolean canBind(CastItem item) {
		return false;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}

	@Override
	public CastResult cast(SpellData data) {
		return new CastResult(PostCastAction.ALREADY_HANDLED, data);
	}

	public boolean activate(LivingEntity caster) {
		return activate(new SpellData(caster));
	}
	
	public boolean activate(LivingEntity caster, float power) {
		return activate(new SpellData(caster, power));
	}
	
	public boolean activate(LivingEntity caster, LivingEntity target) {
		return activate(new SpellData(caster, target));
	}
	
	public boolean activate(LivingEntity caster, Location location) {
		return activate(new SpellData(caster, location));
	}
	
	public boolean activate(final LivingEntity caster, final LivingEntity target, final Location location) {
		return activate(new SpellData(caster, target, location, 1f, null));
	}

	public boolean activate(final LivingEntity caster, final LivingEntity target, final Location location, final float power) {
		return activate(new SpellData(caster, target, location, power, null));
	}

	public boolean activate(SpellData data) {
		if (disabled) return false;

		int delay = this.delay.get(data);
		if (delay < 0) return activateSpells(data);
		MagicSpells.scheduleDelayedTask(() -> activateSpells(data), delay);

		return false;
	}
	
	// DEBUG INFO: level 3, activating passive spell spellName for player playerName state state
	// DEBUG INFO: level 3, casting spell effect spellName
	// DEBUG INFO: level 3, casting without target
	// DEBUG INFO: level 3, casting at entity
	// DEBUG INFO: level 3, target cancelled (TE)
	// DEBUG INFO: level 3, casting at location
	// DEBUG INFO: level 3, target cancelled (TL)
	// DEBUG INFO: level 3, casting normally
	// DEBUG INFO: level 3, target cancelled (UE)
	// DEBUG INFO: level 3, target cancelled (UL)
	// DEBUG INFO: level 3, passive spell cancelled
	private boolean activateSpells(SpellData data) {
		if (disabled || !triggerList.canTarget(data.caster(), true)) return false;

		float chance = this.chance.get(data) / 100;
		if (chance < 1 && random.nextFloat() > chance) return false;

		disabled = true;
		try {
			if (data.caster() instanceof Player) {
				MagicSpells.debug(3, "Activating passive spell '" + name + "' for player " + data.caster().getName());
			} else {
				MagicSpells.debug(3, "Activating passive spell '" + name + "' for livingEntity " + data.caster().getUniqueId());
			}

			SpellCastEvent castEvent = preCast(data);
			data = castEvent.getSpellData();

			if (castEvent.getSpellCastState() != SpellCastState.NORMAL) {
				if (sendFailureMessages) postCast(castEvent, PostCastAction.HANDLE_NORMALLY, data);
				else new SpellCastedEvent(castEvent, PostCastAction.HANDLE_NORMALLY, data).callEvent();

				return false;
			}

			if (data.hasTarget()) {
				SpellTargetEvent targetEvent = new SpellTargetEvent(this, data);
				if (!validTargetList.canTarget(data.caster(), data.target()) || !targetEvent.callEvent()) {
					MagicSpells.debug(3, "    Target cancelled (TE)");
					return false;
				}

				data = targetEvent.getSpellData();
			}

			if (data.hasLocation()) {
				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data);
				if (!targetEvent.callEvent()) {
					MagicSpells.debug(3, "    Target cancelled (TL)");
					return false;
				}

				data = targetEvent.getSpellData();
			}

			SpellData subData = castWithoutTarget ? data.noTargeting() : data;
			for (Subspell spell : spells) spell.subcast(subData);

			playSpellEffects(data);

			postCast(castEvent, PostCastAction.HANDLE_NORMALLY, data);
		} finally {
			disabled = false;
		}

		return true;
	}

}
