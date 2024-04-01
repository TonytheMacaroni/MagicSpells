package com.nisovin.magicspells.spells.targeted;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityRegainHealthEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class DrainlifeSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final String STR_MANA = "mana";
	private static final String STR_HEALTH = "health";
	private static final String STR_HUNGER = "hunger";
	private static final String STR_EXPERIENCE = "experience";

	private static final int MAX_FOOD_LEVEL = 20;
	private static final int MIN_FOOD_LEVEL = 0;
	private static final double MIN_HEALTH = 0D;

	private final ConfigData<String> takeType;
	private final ConfigData<String> giveType;
	private final ConfigData<String> spellDamageType;

	private final ConfigData<Double> takeAmt;
	private final ConfigData<Double> giveAmt;

	private final ConfigData<Integer> animationSpeed;

	private final ConfigData<Boolean> instant;
	private final ConfigData<Boolean> ignoreArmor;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> showSpellEffect;
	private final ConfigData<Boolean> powerAffectsAmount;
	private final ConfigData<Boolean> avoidDamageModification;

	private Subspell spellOnAnimation;
	private String spellOnAnimationName;

	private final ConfigData<DamageCause> damageType;

	public DrainlifeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		takeType = getConfigDataString("take-type", "health");
		giveType = getConfigDataString("give-type", "health");
		spellDamageType = getConfigDataString("spell-damage-type", "");

		takeAmt = getConfigDataDouble("take-amt", 2);
		giveAmt = getConfigDataDouble("give-amt", 2);

		animationSpeed = getConfigDataInt("animation-speed", 2);

		instant = getConfigDataBoolean("instant", true);
		ignoreArmor = getConfigDataBoolean("ignore-armor", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		showSpellEffect = getConfigDataBoolean("show-spell-effect", true);
		powerAffectsAmount = getConfigDataBoolean("power-affects-amount", true);
		avoidDamageModification = getConfigDataBoolean("avoid-damage-modification", true);

		spellOnAnimationName = getConfigString("spell-on-animation", "");

		damageType = getConfigDataEnum("damage-type", DamageCause.class, DamageCause.ENTITY_ATTACK);
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOnAnimation = initSubspell(spellOnAnimationName, true, "for 'spell-on-animation'");
		spellOnAnimationName = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		return drain(data) ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		return drain(data) ? new CastResult(PostCastAction.HANDLE_NORMALLY, data) : noTarget(data);
	}

	private boolean drain(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		boolean checkPlugins = this.checkPlugins.get(data);

		double take = takeAmt.get(data);
		double give = giveAmt.get(data);
		if (powerAffectsAmount.get(data)) {
			take *= data.power();
			give *= data.power();
		}

		Player playerTarget = target instanceof Player p ? p : null;

		switch (takeType.get(data)) {
			case STR_HEALTH -> {
				DamageCause damageType = this.damageType.get(data);

				if (checkPlugins) {
					MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, damageType, take, this);
					if (!event.callEvent()) return false;
					if (!avoidDamageModification.get(data)) take = event.getDamage();
					target.setLastDamageCause(event);
				}

				SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, caster, target, take, damageType, spellDamageType.get(data));
				EventUtil.call(event);
				take = event.getFinalDamage();
				if (ignoreArmor.get(data)) {
					double health = target.getHealth();
					if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
					health -= take;
					if (health < MIN_HEALTH) health = MIN_HEALTH;
					if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
					if (health == MIN_HEALTH && caster instanceof Player) target.setKiller((Player) caster);
					target.setHealth(health);
					target.setLastDamage(take);
					Util.playHurtEffect(data.target(), data.caster());
				} else target.damage(take, caster);
			}
			case STR_MANA -> {
				if (playerTarget == null) break;
				boolean removed = MagicSpells.getManaHandler().removeMana(playerTarget, (int) Math.round(take), ManaChangeReason.OTHER);
				if (!removed) give = 0;
			}
			case STR_HUNGER -> {
				if (playerTarget == null) break;
				int food = playerTarget.getFoodLevel();
				if (give > food) give = food;
				food -= take;
				if (food < MIN_FOOD_LEVEL) food = MIN_FOOD_LEVEL;
				playerTarget.setFoodLevel(food);
			}
			case STR_EXPERIENCE -> {
				if (playerTarget == null) break;
				int exp = ExperienceUtils.getExperience(playerTarget);
				if (give > exp) give = exp;
				ExperienceUtils.changeExp(playerTarget, (int) Math.round(-take));
			}
		}

		String giveType = this.giveType.get(data);
		boolean instant = this.instant.get(data);

		if (instant) {
			giveToCaster(caster, giveType, give, checkPlugins);
			playSpellEffects(caster, target, data);
		} else playSpellEffects(EffectPosition.TARGET, target, data);

		if (showSpellEffect.get(data))
			new DrainAnimation(caster, target.getLocation(), giveType, give, instant, checkPlugins, data);

		return true;
	}

	private void giveToCaster(LivingEntity caster, String giveType, double give, boolean checkPlugins) {
		switch (giveType) {
			case STR_HEALTH -> {
				if (checkPlugins) {
					MagicSpellsEntityRegainHealthEvent event = new MagicSpellsEntityRegainHealthEvent(caster, give, EntityRegainHealthEvent.RegainReason.CUSTOM);
					if (!event.callEvent()) return;

					give = event.getAmount();
				}

				double h = caster.getHealth() + give;
				if (h > Util.getMaxHealth(caster)) h = Util.getMaxHealth(caster);
				caster.setHealth(h);
			}
			case STR_MANA -> {
				if (caster instanceof Player)
					MagicSpells.getManaHandler().addMana((Player) caster, (int) give, ManaChangeReason.OTHER);
			}
			case STR_HUNGER -> {
				if (caster instanceof Player) {
					int food = ((Player) caster).getFoodLevel();
					food += give;
					if (food > MAX_FOOD_LEVEL) food = MAX_FOOD_LEVEL;
					((Player) caster).setFoodLevel(food);
				}
			}
			case STR_EXPERIENCE -> {
				if (caster instanceof Player) ExperienceUtils.changeExp((Player) caster, (int) give);
			}
		}
	}

	private class DrainAnimation extends SpellAnimation {

		private final LivingEntity caster;
		private final boolean checkPlugins;
		private final boolean instant;
		private final String giveType;
		private final SpellData data;
		private final Vector current;
		private final double giveAmt;
		private final World world;
		private final int range;

		private DrainAnimation(LivingEntity caster, Location start, String giveType, double giveAmt, boolean instant, boolean checkPlugins, SpellData data) {
			super(animationSpeed.get(data), true);

			this.data = data;
			this.caster = caster;
			this.instant = instant;
			this.giveAmt = giveAmt;
			this.giveType = giveType;
			this.checkPlugins = checkPlugins;

			current = start.toVector();
			world = caster.getWorld();
			range = getRange(data);
		}

		@Override
		protected void onTick(int tick) {
			Vector v = current.clone().subtract(caster.getLocation().toVector()).normalize();
			current.subtract(v);

			Location playAt = current.toLocation(world).setDirection(v);
			playSpellEffects(EffectPosition.SPECIAL, playAt, data);
			if (current.distanceSquared(caster.getLocation().toVector()) < 4 || tick > range * 1.5) {
				stop(true);
				playSpellEffects(EffectPosition.DELAYED, caster, data);
				if (spellOnAnimation != null) spellOnAnimation.subcast(data.noTarget());
				if (!instant) giveToCaster(caster, giveType, giveAmt, checkPlugins);
			}
		}

	}

}
