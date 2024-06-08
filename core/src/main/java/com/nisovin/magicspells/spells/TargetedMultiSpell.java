package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public final class TargetedMultiSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private static final Pattern DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private List<String> spellList;
	private final List<Action> actions;

	private final ConfigData<Float> yOffset;

	private final ConfigData<Boolean> pointBlank;
	private final ConfigData<Boolean> stopOnFail;
	private final ConfigData<Boolean> passTargeting;
	private final ConfigData<Boolean> requireEntityTarget;
	private final ConfigData<Boolean> castRandomSpellInstead;

	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		actions = new ArrayList<>();
		spellList = getConfigStringList("spells", null);

		yOffset = getConfigDataFloat("y-offset", 0F);

		pointBlank = getConfigDataBoolean("point-blank", false);
		stopOnFail = getConfigDataBoolean("stop-on-fail", true);
		passTargeting = getConfigDataBoolean("pass-targeting", false);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", false);
		castRandomSpellInstead = getConfigDataBoolean("cast-random-spell-instead", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (spellList == null) return;

		for (int i = 0; i < spellList.size(); i++) {
			String s = spellList.get(i);

			if (DELAY_PATTERN.asMatchPredicate().test(s)) {
				int delay = Integer.parseInt(s.split(" ")[1]);
				actions.add(new Action(delay));
				continue;
			}

			Subspell spell = initSubspell(s, false, "spells[" + i + "]");
			if (spell == null) continue;

			actions.add(new Action(spell));
		}

		spellList = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (requireEntityTarget.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);

			return runSpells(info.spellData());
		}

		if (pointBlank.get(data)) {
			SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!targetEvent.callEvent()) return noTarget(targetEvent);

			return runSpells(targetEvent.getSpellData());
		}

		TargetInfo<Location> info = getTargetedBlockLocation(data, 0.5, 0, 0.5, false);
		if (info.noTarget()) return noTarget(info);

		return runSpells(info.spellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		return runSpells(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		return runSpells(data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		return runSpells(data);
	}

	private CastResult runSpells(SpellData data) {
		if (data.hasLocation() && !data.hasTarget()) data = data.location(data.location().add(0, yOffset.get(data), 0));

		boolean stopOnFail = this.stopOnFail.get(data);
		boolean passTargeting = this.passTargeting.get(data);

		boolean somethingWasDone = false;
		if (!castRandomSpellInstead.get(data)) {
			int delay = 0;
			Subspell spell;
			List<DelayedSpell> delayedSpells = new ArrayList<>();
			for (Action action : actions) {
				if (action.isDelay()) {
					delay += action.getDelay();
					continue;
				}

				if (action.isSpell()) {
					spell = action.getSpell();
					if (delay == 0) {
						boolean ok = spell.subcast(data, passTargeting).success();
						if (ok) somethingWasDone = true;
						else if (stopOnFail) break;
						continue;
					}

					DelayedSpell ds = new DelayedSpell(spell, delayedSpells, data, stopOnFail, passTargeting);
					delayedSpells.add(ds);
					MagicSpells.scheduleDelayedTask(ds, delay);
					somethingWasDone = true;
				}
			}
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) somethingWasDone = action.getSpell().subcast(data).success();
		}

		if (somethingWasDone) playSpellEffects(data);
		return somethingWasDone ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(data);
	}

	private static class Action {

		private final Subspell spell;
		private final int delay;

		private Action(Subspell spell) {
			this.spell = spell;
			delay = 0;
		}

		private Action(int delay) {
			this.delay = delay;
			spell = null;
		}

		public boolean isSpell() {
			return spell != null;
		}

		public Subspell getSpell() {
			return spell;
		}

		public boolean isDelay() {
			return delay > 0;
		}

		public int getDelay() {
			return delay;
		}

	}

	private static class DelayedSpell implements Runnable {

		private final Subspell spell;
		private final SpellData data;
		private final boolean stopOnFail;
		private final boolean passTargeting;

		private List<DelayedSpell> delayedSpells;
		private boolean cancelled;

		private DelayedSpell(Subspell spell, List<DelayedSpell> delayedSpells, SpellData data, boolean stopOnFail, boolean passTargeting) {
			this.delayedSpells = delayedSpells;
			this.passTargeting = passTargeting;
			this.stopOnFail = stopOnFail;
			this.spell = spell;
			this.data = data;

			cancelled = false;
		}

		public void cancelAll() {
			for (DelayedSpell ds : delayedSpells) ds.cancelled = true;
			delayedSpells.clear();
		}

		@Override
		public void run() {
			if (cancelled) return;

			if (!data.hasCaster() || data.caster().isValid()) {
				boolean ok = spell.subcast(data, passTargeting).success();
				delayedSpells.remove(this);
				if (!ok && stopOnFail) cancelAll();
			} else cancelAll();

			delayedSpells = null;
		}

	}

}
