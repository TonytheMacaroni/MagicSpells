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
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PassiveSpell extends Spell {

	private final List<PassiveListener> passiveListeners;
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

		spells = initSubspells("spells");
		if (spells.isEmpty()) MagicDebug.warn("PassiveSpell %s has no spells defined!", MagicDebug.resolveFullPath());
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
			MagicDebug.warn("No 'triggers' defined for PassiveSpell %s.", MagicDebug.resolveFullPath());
			return;
		}

		int count = 0;
		try (var ignored = MagicDebug.section("Processing 'triggers'.")
			.pushPath("triggers", DebugPath.Type.LIST)
		) {
			for (int i = 0; i < triggers.size(); i++) {
				try (var ignored1 = MagicDebug.pushListEntry(i)) {
					Object trigger = triggers.get(i);

					// TODO: Add section for each list entry
					PassiveListener listener = switch (trigger) {
						case String string -> initializeListener(string);
						case Map<?, ?> map -> initializeListener(map);
						default -> {
							MagicDebug.warn("Invalid value '%s' found for passive trigger %s.", trigger, MagicDebug.resolveFullPath());
							yield null;
						}
					};

					if (listener == null) {
						MagicDebug.warn("Invalid passive trigger %s.", MagicDebug.resolveFullPath());
						continue;
					}

					MagicSpells.registerEvents(listener, listener.getEventPriority());
					passiveListeners.add(listener);
					count++;
				}
			}
		}

		if (count == 0) MagicDebug.warn("No 'triggers' defined for PassiveSpell %s", MagicDebug.resolveFullPath());
	}

	private PassiveListener initializeListener(String string) {
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
			MagicDebug.warn("Invalid passive trigger type '%s' %s.", type, MagicDebug.resolveFullPath());
			return null;
		}

		listener.setPassiveSpell(this);
		listener.setEventPriority(priority);
		listener.initialize(args);

		return listener;
	}

	private PassiveListener initializeListener(Map<?, ?> map) {
		ConfigurationSection config = ConfigReaderUtil.mapToSection(map);

		String type = config.getString("trigger");
		if (type == null) {
			MagicDebug.warn("No 'trigger' defined for passive trigger %s.", MagicDebug.resolveFullPath());
			return null;
		}

		PassiveListener listener = MagicSpells.getPassiveManager().getListenerByName(type);
		if (listener == null) {
			MagicDebug.warn("Invalid 'trigger' value '%s' defined for passive trigger %s.", type, MagicDebug.resolveFullPath());
			return null;
		}

		listener.setPassiveSpell(this);
		if (!listener.initialize(config)) {
			MagicDebug.warn("Invalid passive trigger defined at %s.", MagicDebug.resolveFullPath());
			return null;
		}

		EventPriority priority = EventPriority.NORMAL;

		String priorityString = config.getString("priority");
		if (priorityString != null) {
			try {
				priority = EventPriority.valueOf(priorityString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid 'priority' value '%s' defined at %s.",priorityString, MagicDebug.resolveFullPath());
				return null;
			}
		}

		listener.setEventPriority(priority);

		return listener;
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
		try (var ignored = MagicDebug.section(DebugCategory.CAST, this, "Activating passive spell '%s'.", this)) {
			MagicDebug.info("Spell data: %s", data);

			if (disabled) {
				MagicDebug.info("Recursive passive activation detected - cast failed.");
				return false;
			}

			int delay = this.delay.get(data);
			if (delay < 0) {
				MagicDebug.info("Casting with no delay.");
				return activateSpells(data);
			}

			MagicDebug.info("Casting with a delay (%s ticks).", delay);
			MagicSpells.scheduleDelayedTask(() -> {
				try (var ignored1 = MagicDebug.section(DebugCategory.CAST, this, "Activating delayed passive spell '%s'.", this)) {
					activateSpells(data);
				}
			}, delay);

			return false;
		}
	}

	private boolean activateSpells(SpellData data) {
		if (disabled) {
			MagicDebug.info("Recursive passive activation detected - cast failed.");
			return false;
		}

		if (!triggerList.canTarget(data.caster(), true)) {
			MagicDebug.info("Invalid trigger entity - cast failed.");
			return false;
		}

		float chance = this.chance.get(data) / 100, roll;
		if (chance < 1 && (roll = random.nextFloat()) > chance) {
			MagicDebug.info("Chance rolled too high (%s > %s) - casted failed.", roll, chance);
			return false;
		}

		disabled = true;
		try (var ignored = MagicDebug.section(DebugCategory.CAST, "Casting passive spell '%s' with caster '%s'.", this, data.caster())) {
			MagicDebug.info("Spell data: %s", data);

			SpellCastEvent castEvent = preCast(data);
			data = castEvent.getSpellData();

			if (castEvent.getSpellCastState() != SpellCastState.NORMAL) {
				MagicDebug.info("Spell cast state is %s - cast failed.", castEvent.getSpellCastState());

				if (sendFailureMessages) postCast(castEvent, PostCastAction.HANDLE_NORMALLY, data);
				else new SpellCastedEvent(castEvent, PostCastAction.HANDLE_NORMALLY, data).callEvent();

				return false;
			}

			if (data.hasTarget()) {
				if (!validTargetList.canTarget(data.caster(), data.target())) {
					MagicDebug.info("Target failed 'can-target' check - cast failed.");
					return false;
				}

				SpellTargetEvent targetEvent = new SpellTargetEvent(this, data);
				if (!targetEvent.callEvent()) {
					MagicDebug.info("Entity targeting cancelled - cast failed.");
					return false;
				}

				data = targetEvent.getSpellData();
			}

			if (data.hasLocation()) {
				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data);
				if (!targetEvent.callEvent()) {
					MagicDebug.info("Location targeting cancelled - cast failed.");
					return false;
				}

				data = targetEvent.getSpellData();
			}

			try (var ignored1 = MagicDebug.section("Casting subspells.")) {
				SpellData subData = castWithoutTarget ? data.noTargeting() : data;
				for (Subspell spell : spells) spell.subcast(subData);
			}

			playSpellEffects(data);

			postCast(castEvent, PostCastAction.HANDLE_NORMALLY, data);
		} finally {
			disabled = false;
		}

		return true;
	}

}
