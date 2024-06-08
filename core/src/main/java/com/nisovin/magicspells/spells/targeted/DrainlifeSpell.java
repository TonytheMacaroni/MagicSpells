package com.nisovin.magicspells.spells.targeted;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.damage.DamageType;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityRegainHealthEvent;

import io.papermc.paper.registry.RegistryKey;

@SuppressWarnings("UnstableApiUsage")
public class DrainlifeSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final DeprecationNotice HEALTH_DEPRECATION_NOTICE = new DeprecationNotice(
		"The 'health' drain type of '.targeted.DrainlifeSpell' does not function properly.",
		"Use the 'health_points' drain type",
		"https://github.com/TheComputerGeek2/MagicSpells/wiki/Deprecations#targeteddrainlifespell-health-drain-type"
	);

	private final ConfigData<DrainType> takeType;
	private final ConfigData<DrainType> giveType;
	private final ConfigData<String> spellDamageType;
	private final ConfigData<DamageType> drainDamageType;

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

		takeType = getConfigDataEnum("take-type", DrainType.class, DrainType.HEALTH_POINTS);
		giveType = getConfigDataEnum("give-type", DrainType.class, DrainType.HEALTH_POINTS);
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

		damageType = getConfigDataEnum("damage-type", DamageCause.class, null);
		drainDamageType = getConfigDataRegistryEntry("drain-damage-type", RegistryKey.DAMAGE_TYPE, null)
			.orDefault(data -> data.caster() instanceof Player ? DamageType.PLAYER_ATTACK : DamageType.MOB_ATTACK);

		MagicSpells.getDeprecationManager().addDeprecation(this, HEALTH_DEPRECATION_NOTICE,
			takeType.isConstant() && takeType.get() == DrainType.HEALTH ||
				giveType.isConstant() && giveType.get() == DrainType.HEALTH
		);
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOnAnimation = initSubspell(spellOnAnimationName, true, "spell-on-animation");
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

		switch (takeType.get(data)) {
			case HEALTH -> {
				DamageCause damageType = this.damageType.get(data);

				if (checkPlugins) {
					EntityDamageEvent event = createFakeDamageEvent(data.caster(), target, damageType, take);
					if (!event.callEvent()) return false;

					if (!avoidDamageModification.get(data)) take = event.getDamage();
					target.setLastDamageCause(event);
				}

				SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, caster, target, take, damageType, spellDamageType.get(data));
				event.callEvent();
				take = event.getFinalDamage();

				if (!ignoreArmor.get(data)) {
					target.damage(take, caster);
					break;
				}

				double maxHealth = Util.getMaxHealth(target);
				double health = Math.min(target.getHealth(), maxHealth);

				health = Math.clamp(health - take, 0, maxHealth);
				if (health == 0 && caster instanceof Player playerCaster) target.setKiller(playerCaster);

				target.setHealth(health);
				target.setLastDamage(take);
				Util.playHurtEffect(target, caster);
			}
			case HEALTH_POINTS -> {
				DamageType type = drainDamageType.get(data);

				SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, caster, target, take, type, spellDamageType.get(data));
				event.callEvent();
				take = event.getFinalDamage();

				DamageSource source = DamageSource.builder(type)
					.withDirectEntity(caster)
					.build();

				target.damage(take, source);
			}
			case MANA -> {
				if (!(target instanceof Player playerTarget)) break;

				boolean removed = MagicSpells.getManaHandler().removeMana(playerTarget, (int) Math.round(take), ManaChangeReason.OTHER);
				if (!removed) give = 0;
			}
			case HUNGER -> {
				if (!(target instanceof Player playerTarget)) break;

				int food = playerTarget.getFoodLevel() - (int) take;
				playerTarget.setFoodLevel(Math.clamp(food, 0, 20));
			}
			case EXPERIENCE -> {
				if (!(target instanceof Player playerTarget)) break;

				Util.addExperience(playerTarget, (int) Math.round(-take));
			}
		}

		DrainType giveType = this.giveType.get(data);
		boolean instant = this.instant.get(data);

		if (instant) {
			giveToCaster(caster, giveType, give, checkPlugins);
			playSpellEffects(caster, target, data);
		} else playSpellEffects(EffectPosition.TARGET, target, data);

		if (showSpellEffect.get(data))
			new DrainAnimation(caster, target.getLocation(), giveType, give, instant, checkPlugins, data);

		return true;
	}

	private void giveToCaster(LivingEntity caster, DrainType giveType, double give, boolean checkPlugins) {
		switch (giveType) {
			case HEALTH -> {
				if (checkPlugins) {
					MagicSpellsEntityRegainHealthEvent event = new MagicSpellsEntityRegainHealthEvent(caster, give, EntityRegainHealthEvent.RegainReason.CUSTOM);
					if (!event.callEvent()) return;

					give = event.getAmount();
				}

				double h = caster.getHealth() + give;
				if (h > Util.getMaxHealth(caster)) h = Util.getMaxHealth(caster);
				caster.setHealth(h);
			}
			case HEALTH_POINTS -> caster.heal(give);
			case MANA -> {
				if (caster instanceof Player player)
					MagicSpells.getManaHandler().addMana(player, (int) give, ManaChangeReason.OTHER);
			}
			case HUNGER -> {
				if (caster instanceof Player player) {
					int food = player.getFoodLevel() + (int) give;
					player.setFoodLevel(Math.clamp(food, 0, 20));
				}
			}
			case EXPERIENCE -> {
				if (caster instanceof Player player)
					Util.addExperience(player, (int) give);
			}
		}
	}

	private class DrainAnimation extends SpellAnimation {

		private final LivingEntity caster;
		private final boolean checkPlugins;
		private final boolean instant;
		private final DrainType giveType;
		private final SpellData data;
		private final Vector current;
		private final double giveAmt;
		private final World world;
		private final int range;

		private DrainAnimation(LivingEntity caster, Location start, DrainType giveType, double giveAmt, boolean instant, boolean checkPlugins, SpellData data) {
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

	private enum DrainType{

		EXPERIENCE,
		HEALTH,
		HEALTH_POINTS,
		HUNGER,
		MANA

	}

}
