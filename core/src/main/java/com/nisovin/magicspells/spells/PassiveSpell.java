package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PassiveSpell extends Spell {

	private final Random random = ThreadLocalRandom.current();

	private final List<PassiveListener> passiveListeners;
	private final List<String> triggers;
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

		triggers = getConfigStringList("triggers", null);
		spellNames = getConfigStringList("spells", null);

		if (config.isList("spells." + internalName + '.' + "can-trigger")) {
			List<String> defaultTargets = getConfigStringList("can-trigger", null);
			if (defaultTargets.isEmpty()) defaultTargets.add("players");
			triggerList = new ValidTargetList(this, defaultTargets);
		} else triggerList = new ValidTargetList(this, getConfigString("can-trigger", "players"));

		delay = getConfigDataInt("delay", -1);

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
		if (spellNames != null) {
			for (String spellName : spellNames) {
				Subspell spell = new Subspell(spellName);
				if (!spell.process()) {
					MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid spell listed: " + spellName);
					continue;
				}
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
		// Get trigger
		int trigCount = 0;
		if (triggers == null) {
			MagicSpells.error("PassiveSpell '" + internalName + "' has no triggers defined!");
			return;
		}

		for (String trigger : triggers) {
			String type = trigger;
			String args = null;
			if (trigger.contains(" ")) {
				String[] data = Util.splitParams(trigger, 2);
				type = data[0];
				args = data[1];
			}
			type = type.toLowerCase();

			EventPriority priority = MagicSpells.getPassiveManager().getEventPriorityFromName(type);
			if (priority == null) priority = EventPriority.NORMAL;

			String priorityName = MagicSpells.getPassiveManager().getEventPriorityName(priority);
			if (priorityName != null) type = type.replace(priorityName, "");

			PassiveListener listener = MagicSpells.getPassiveManager().getListenerByName(type);
			if (listener == null) {
				MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid trigger defined: " + type);
				continue;
			}

			listener.setPassiveSpell(this);
			listener.setEventPriority(priority);
			listener.initialize(args);
			MagicSpells.registerEvents(listener, priority);
			passiveListeners.add(listener);
			trigCount++;
		}

		if (trigCount == 0) MagicSpells.error("PassiveSpell '" + internalName + "' has no triggers defined!");
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

	private boolean isActuallyNonTargeted(Spell spell) {
		if (spell instanceof ExternalCommandSpell) return !((ExternalCommandSpell) spell).requiresPlayerTarget();
		if (spell instanceof BuffSpell) return !((BuffSpell) spell).isTargeted();
		return false;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}
	
	public boolean activate(LivingEntity caster) {
		return activate(caster, null, null);
	}
	
	public boolean activate(LivingEntity caster, float power) {
		return activate(caster, null, null, power);
	}
	
	public boolean activate(LivingEntity caster, LivingEntity target) {
		return activate(caster, target, null, 1F);
	}
	
	public boolean activate(LivingEntity caster, Location location) {
		return activate(caster, null, location, 1F);
	}
	
	public boolean activate(final LivingEntity caster, final LivingEntity target, final Location location) {
		return activate(caster, target, location, 1F);
	}
	
	public boolean activate(final LivingEntity caster, final LivingEntity target, final Location location, final float power) {
		int delay = this.delay.get(caster, target, power, null);
		if (delay < 0) return activateSpells(caster, target, location, power);
		MagicSpells.scheduleDelayedTask(() -> activateSpells(caster, target, location, power), delay);
		return false;
	}
	
	// DEBUG INFO: level 3, activating passive spell spellname for player playername state state
	// DEBUG INFO: level 3, casting spell effect spellname
	// DEBUG INFO: level 3, casting without target
	// DEBUG INFO: level 3, casting at entity
	// DEBUG INFO: level 3, target cancelled (TE)
	// DEBUG INFO: level 3, casting at location
	// DEBUG INFO: level 3, target cancelled (TL)
	// DEBUG INFO: level 3, casting normally
	// DEBUG INFO: level 3, target cancelled (UE)
	// DEBUG INFO: level 3, target cancelled (UL)
	// DEBUG INFO: level 3, passive spell cancelled
	private boolean activateSpells(LivingEntity caster, LivingEntity target, Location location, float basePower) {
		if (!triggerList.canTarget(caster, true)) return false;
		SpellCastState state = getCastState(caster);
		if (caster instanceof Player) {
			MagicSpells.debug(3, "Activating passive spell '" + name + "' for player " + caster.getName() + " (state: " + state + ')');
		} else {
			MagicSpells.debug(3, "Activating passive spell '" + name + "' for livingEntity " + caster.getUniqueId() + " (state: " + state + ')');
		}

		if (state != SpellCastState.NORMAL && sendFailureMessages) {
			if (state == SpellCastState.ON_COOLDOWN) {
				sendMessage(strOnCooldown, caster, null, "%c", Math.round(getCooldown(caster)) + "");
				return false;
			}

			if (state == SpellCastState.MISSING_REAGENTS) {
				MagicSpells.sendMessage(strMissingReagents, caster, MagicSpells.NULL_ARGS);
				if (MagicSpells.showStrCostOnMissingReagents() && strCost != null && !strCost.isEmpty()) {
					MagicSpells.sendMessage("    (" + strCost + ')', caster, MagicSpells.NULL_ARGS);
				}
			}
			return false;
		}

		if (disabled || state != SpellCastState.NORMAL) return false;

		float chance = this.chance.get(caster, target, basePower, null) / 100;
		if (chance < 1 && random.nextFloat() > chance) return false;

		disabled = true;
		SpellCastEvent castEvent = new SpellCastEvent(this, caster, SpellCastState.NORMAL, basePower, null, cooldown, reagents.clone(), 0);
		EventUtil.call(castEvent);

		if (castEvent.isCancelled() || castEvent.getSpellCastState() != SpellCastState.NORMAL) {
			MagicSpells.debug(3, "   Passive spell cancelled");
			disabled = false;
			return false;
		}

		if (castEvent.haveReagentsChanged() && !hasReagents(caster, castEvent.getReagents())) {
			disabled = false;
			return false;
		}

		basePower = castEvent.getPower();

		if (target != null) {
			SpellTargetEvent targetEvent = new SpellTargetEvent(this, caster, target, basePower);
			if (!targetEvent.callEvent()) {
				MagicSpells.debug(3, "    Target cancelled (TE)");

				disabled = false;
				return false;
			}

			basePower = targetEvent.getPower();
			target = targetEvent.getTarget();
		}

		if (location != null) {
			SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, caster, location, basePower);
			if (!targetEvent.callEvent()) {
				MagicSpells.debug(3, "    Target cancelled (TL)");

				disabled = false;
				return false;
			}

			basePower = targetEvent.getPower();
			location = targetEvent.getTargetLocation();
		}

		setCooldown(caster, castEvent.getCooldown());
		boolean spellEffectsDone = false;

		for (Subspell spell : spells) {
			MagicSpells.debug(3, "    Casting spell effect '" + spell.getSpell().getName() + '\'');
			if (castWithoutTarget) {
				MagicSpells.debug(3, "    Casting without target");

				spell.cast(caster, basePower);
				if (!spellEffectsDone) {
					playSpellEffects(EffectPosition.CASTER, caster);
					spellEffectsDone = true;
				}

				continue;
			}

			if (spell.isTargetedEntitySpell() && target != null && !isActuallyNonTargeted(spell.getSpell())) {
				spell.castAtEntity(caster, target, basePower);
				if (!spellEffectsDone) {
					playSpellEffects(caster, target);
					spellEffectsDone = true;
				}

				continue;
			}

			if (spell.isTargetedLocationSpell() && (location != null || target != null)) {
				MagicSpells.debug(3, "    Casting at location");

				Location loc = location != null ? location : target.getLocation();

				spell.castAtLocation(caster, loc, basePower);
				if (!spellEffectsDone) {
					playSpellEffects(caster, loc);
					spellEffectsDone = true;
				}

				continue;
			}

			MagicSpells.debug(3, "    Casting normally");

			spell.cast(caster, basePower);
			if (!spellEffectsDone) {
				playSpellEffects(EffectPosition.CASTER, caster);
				spellEffectsDone = true;
			}
		}

		removeReagents(caster, castEvent.getReagents());
		sendMessage(strCastSelf, caster, MagicSpells.NULL_ARGS);
		SpellCastedEvent castedEvent = new SpellCastedEvent(this, caster, SpellCastState.NORMAL, basePower, null, castEvent.getCooldown(), castEvent.getReagents(), PostCastAction.HANDLE_NORMALLY);
		EventUtil.call(castedEvent);
		disabled = false;
		return true;
	}

}
