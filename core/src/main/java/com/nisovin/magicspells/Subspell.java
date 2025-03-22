package com.nisovin.magicspells;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.FunctionData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class Subspell {

	private static final Random random = ThreadLocalRandom.current();

	private String text;

	private Spell spell;
	private final String spellName;
	private CastMode mode = CastMode.PARTIAL;
	private CastTargeting targeting = CastTargeting.NORMAL;

	private ConfigData<Integer> delay = data -> -1;
	private ConfigData<Double> chance = data -> -1D;
	private ConfigData<Float> subPower = data -> 1F;
	private ConfigData<String[]> args = data -> null;

	private ConfigData<Boolean> invert = data -> false;
	private ConfigData<Boolean> passArgs = data -> false;
	private ConfigData<Boolean> passPower = data -> true;
	private ConfigData<Boolean> passTargeting = data -> null;

	private boolean isTargetedEntity = false;
	private boolean isTargetedLocation = false;
	private boolean isTargetedEntityFromLocation = false;

	public Subspell(@NotNull String subspell) {
		try (var ignored = MagicDebug.section("Resolving subspell '%s'.", subspell)) {
			String[] split = subspell.split("\\(", 2);

			text = subspell;
			spellName = split[0].trim();
			MagicDebug.info("Spell name: %s", spellName);

			if (split.length == 1) {
				MagicDebug.info("No cast arguments - skipping.");
				return;
			}

			split[1] = split[1].trim();
			if (split[1].endsWith(")")) split[1] = split[1].substring(0, split[1].length() - 1);

			String[] arguments = split[1].split(";");
			for (String arg : arguments) {
				try (var ignored1 = MagicDebug.section("Resolving cast argument '%s'.", arg)) {
					String[] data = arg.split("=", 2);
					if (data.length != 2) {
						MagicDebug.warn("Invalid cast argument '%s' on subspell %s - too few/many values.", data[0], MagicDebug.resolveFullPath());
						continue;
					}

					resolveCastArgument(data[0].toLowerCase().trim(), data[1].trim());
				}
			}
		}
	}

	private void resolveCastArgument(String key, String value) {
		switch (key) {
			case "mode" -> {
				mode = CastMode.getFromString(value);

				if (mode == null)
					MagicDebug.warn("Invalid cast mode '%s' on subspell %s.", value, MagicDebug.resolveFullPath());
			}
			case "targeting" -> {
				try {
					targeting = CastTargeting.valueOf(value.toUpperCase());
				} catch (IllegalArgumentException e) {
					MagicDebug.warn("Invalid cast target '%s' on subspell %s.", value, MagicDebug.resolveFullPath());
				}
			}
			case "invert" -> invert = ConfigDataUtil.getBoolean(value, false);
			case "pass-args" -> passArgs = ConfigDataUtil.getBoolean(value, false);
			case "pass-power" -> passPower = ConfigDataUtil.getBoolean(value, true);
			case "pass-targeting" -> {
				ConfigData<String> supplier = ConfigDataUtil.getString(value);

				if (supplier.isConstant()) {
					String val = supplier.get();
					if (val == null) {
						passTargeting = data -> null;
						return;
					}

					Boolean b = switch (val.toLowerCase()) {
						case "true" -> true;
						case "false" -> false;
						default -> null;
					};

					passTargeting = data -> b;
					return;
				}

				passTargeting = data -> {
					String val = supplier.get(data);
					if (val == null) return null;

					return switch (val.toLowerCase()) {
						case "true" -> true;
						case "false" -> false;
						default -> null;
					};
				};
			}
			case "args" -> {
				try {
					JsonElement element = JsonParser.parseString(value);
					JsonArray array = element.getAsJsonArray();

					List<ConfigData<String>> argumentData = new ArrayList<>();
					List<String> arguments = new ArrayList<>();

					boolean constant = true;
					for (JsonElement je : array) {
						String val = je.getAsString();
						ConfigData<String> supplier = ConfigDataUtil.getString(val);

						argumentData.add(supplier);
						arguments.add(val);

						constant &= supplier.isConstant();
					}

					if (constant) {
						String[] arg = arguments.toArray(new String[0]);
						args = data -> arg;

						return;
					}

					args = data -> {
						String[] ret = new String[argumentData.size()];
						for (int j = 0; j < argumentData.size(); j++)
							ret[j] = argumentData.get(j).get(data);

						return ret;
					};
				} catch (Exception e) {
					MagicDebug.warn(e, "Invalid spell arguments '%s' on subspell %s.", value, MagicDebug.resolveFullPath());
				}
			}
			case "power" -> {
				try {
					float subPower = Float.parseFloat(value);
					this.subPower = data -> subPower;
				} catch (NumberFormatException e) {
					FunctionData<Float> subPowerData = FunctionData.build(value, Double::floatValue, true);
					if (subPowerData == null) {
						MagicDebug.warn("Invalid power '%s' on subspell %s.", value, MagicDebug.resolveFullPath());
						return;
					}

					subPower = subPowerData;
				}
			}
			case "delay" -> {
				try {
					int delay = Integer.parseInt(value);
					this.delay = data -> delay;
				} catch (NumberFormatException e) {
					FunctionData<Integer> delayData = FunctionData.build(value, Double::intValue, true);
					if (delayData == null) {
						MagicDebug.warn("Invalid delay '%s' on subspell %s.", value, MagicDebug.resolveFullPath());
						return;
					}

					delay = delayData;
				}
			}
			case "chance" -> {
				try {
					double chance = Double.parseDouble(value) / 100;
					this.chance = data -> chance;
				} catch (NumberFormatException e) {
					FunctionData<Double> chanceData = FunctionData.build(value, Function.identity(), true);
					if (chanceData == null) {
						MagicDebug.warn("Invalid chance '%s' on subspell %s.", value, MagicDebug.resolveFullPath());
						return;
					}

					chance = data -> chanceData.get(data) / 100;
				}
			}
			default ->
				MagicDebug.warn("Invalid cast argument '%s' on subspell %s.", key, MagicDebug.resolveFullPath());
		}
	}

	public boolean process() {
		spell = MagicSpells.getSpellByInternalName(spellName);
		if (spell == null) {
			MagicDebug.warn("Invalid spell '%s' on subspell '%s'.", spellName, MagicDebug.resolveFullPath());
			return false;
		}

		isTargetedEntity = spell instanceof TargetedEntitySpell;
		isTargetedLocation = spell instanceof TargetedLocationSpell;
		isTargetedEntityFromLocation = spell instanceof TargetedEntityFromLocationSpell;

		switch (targeting) {
			case NONE -> {
				isTargetedEntity = false;
				isTargetedLocation = false;
				isTargetedEntityFromLocation = false;
			}
			case ENTITY -> {
				if (!isTargetedEntity) return false;

				isTargetedLocation = false;
				isTargetedEntityFromLocation = false;
			}
			case LOCATION -> {
				if (!isTargetedLocation) return false;

				isTargetedEntity = false;
				isTargetedEntityFromLocation = false;
			}
			case ENTITY_FROM_LOCATION -> {
				if (!isTargetedEntityFromLocation) return false;

				isTargetedEntity = false;
				isTargetedLocation = false;
			}
		}

		return true;
	}

	public Spell getSpell() {
		return spell;
	}

	public boolean isTargetedEntitySpell() {
		return isTargetedEntity;
	}

	public boolean isTargetedLocationSpell() {
		return isTargetedLocation;
	}

	public boolean isTargetedEntityFromLocationSpell() {
		return isTargetedEntityFromLocation;
	}

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data) {
		return subcast(data, false, true, CastTargeting.DEFAULT_ORDERING);
	}

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data, boolean passTargeting) {
		return subcast(data, passTargeting, true, CastTargeting.DEFAULT_ORDERING);
	}

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data, boolean passTargeting, boolean useTargetForLocation) {
		return subcast(data, passTargeting, useTargetForLocation, CastTargeting.DEFAULT_ORDERING);
	}

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data, boolean passTargeting, boolean useTargetForLocation, @NotNull CastTargeting @NotNull [] ordering) {
		try (var ignored = MagicDebug.section(builder -> builder
			.message("Casting subspell '%s'.", text)
			.category(DebugCategory.CAST)
			.configure(spell)
		)) {
			MagicDebug.info("Original spell data: %s", data);
			MagicDebug.info("Mode: %s", mode);

			if (invert.get(data)) {
				data = data.invert();
				MagicDebug.info("Inverting. Spell data: %s", data);
			}

			boolean hasCaster = data.caster() != null;
			boolean hasTarget = data.target() != null;
			boolean hasLocation = data.location() != null;

			if (!hasCaster && (mode == CastMode.FULL || mode == CastMode.HARD)) {
				MagicDebug.info("No caster provided, but mode is %s - cast failed.", mode);
				return fail(data);
			}

			CastTargeting targeting = this.targeting;
			if (targeting == CastTargeting.NORMAL) {
				targeting = null;

				for (CastTargeting ct : ordering) {
					boolean valid = switch (ct) {
						case NORMAL -> false;
						case ENTITY_FROM_LOCATION -> isTargetedEntityFromLocation && hasTarget && hasLocation;
						case ENTITY -> isTargetedEntity && hasTarget;
						case LOCATION -> isTargetedLocation && (hasLocation || hasTarget && useTargetForLocation);
						case NONE -> hasCaster;
					};

					if (valid) {
						targeting = ct;
						break;
					}
				}

				if (targeting == null) {
					MagicDebug.info("No valid target was found - cast failed.");
					return fail(data);
				}
			}

			MagicDebug.info("Targeting: %s", targeting);

			return switch (targeting) {
				case ENTITY_FROM_LOCATION -> {
					if (!hasLocation || !hasTarget) {
						MagicDebug.info("Targeting is ENTITY_FROM_LOCATION, but no %s was found - cast failed.", hasLocation ? "target entity" : "location");
						yield wrapResult(spell.noTarget(data));
					}

					yield castAtEntityFromLocation(data, passTargeting);
				}
				case ENTITY -> {
					if (!hasTarget) {
						MagicDebug.info("Targeting is ENTITY, but no target entity was found - cast failed.");
						yield wrapResult(spell.noTarget(data));
					}

					yield castAtEntity(data, passTargeting);
				}
				case LOCATION -> {
					if (hasTarget && useTargetForLocation) {
						data = data.location(data.target().getLocation());
						yield castAtLocation(data);
					}

					if (!hasLocation) {
						MagicDebug.info("Targeting is LOCATION, but no target location was found - cast failed.");
						yield wrapResult(spell.noTarget(data));
					}

					yield castAtLocation(data);
				}
				case NONE -> {
					if (!hasCaster) {
						MagicDebug.info("Targeting is NONE, but no caster was found - cast failed.");
						yield fail(data);
					}

					yield cast(data);
				}
				default -> {
					MagicDebug.info("Reached an invalid targeting mode - cast failed.");
					yield fail(data);
				}
			};
		}
	}

	@Deprecated
	public PostCastAction cast(LivingEntity caster, float power) {
		try (var ignored = MagicDebug.section(builder -> builder
			.message("Legacy casting subspell '%s' (mode: %s, targeting: NONE).", text, mode)
			.category(DebugCategory.CAST)
			.configure(spell)
		)) {
			SpellCastResult result = cast(new SpellData(caster, power, null));
			return result.state == SpellCastState.NORMAL ? result.action : PostCastAction.ALREADY_HANDLED;
		}
	}

	@NotNull
	private SpellCastResult cast(@NotNull SpellData data) {
		data = data.builder()
			.recipient(null)
			.power((passPower.get(data) ? data.power() : 1) * subPower.get(data))
			.args(passArgs.get(data) ? data.args() : args.get(data))
			.build();

		MagicDebug.info("Subspell spell data: %s", data);

		double chance = this.chance.get(data), roll;
		if ((chance > 0 && chance < 1) && (roll = random.nextDouble()) > chance) {
			MagicDebug.info("Chance rolled too high (%s > %s) - casted failed.", roll, chance);
			return fail(data);
		}

		int delay = this.delay.get(data);
		if (delay < 0) {
			MagicDebug.info("Casting with no delay.");
			return castReal(data.noTargeting());
		}

		SpellData finalData = data.noTargeting();
		MagicSpells.scheduleDelayedTask(() -> {
			try (var ignored = MagicDebug.section(DebugCategory.CAST, "Casting delayed subspell '%s' (mode: %s, targeting: NONE).", text, mode)) {
				castReal(finalData);
			}
		}, delay);

		MagicDebug.info("Casting with a delay (%d ticks).", delay);
		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD, FULL -> spell.hardCast(data);
			case DIRECT -> wrapResult(spell.cast(data));
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				if (!castEvent.callEvent()) {
					MagicDebug.info("Spell cast cancelled.");
					yield postCast(castEvent, null, true);
				}

				CastResult result = spell.cast(castEvent.getSpellCastState(), castEvent.getSpellData());
				yield postCast(castEvent, result, true);
			}
		};
	}

	@Deprecated
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		try (var ignored = MagicDebug.section(builder -> builder
			.message("Legacy casting subspell '%s' (mode: %s, targeting: ENTITY).", text, mode)
			.category(DebugCategory.CAST)
			.configure(spell)
		)) {
			return castAtEntity(new SpellData(caster, target, power, null), false).success();
		}
	}

	@Deprecated
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, boolean passTargeting) {
		try (var ignored = MagicDebug.section(builder -> builder
			.message("Legacy casting subspell '%s' (mode: %s, targeting: ENTITY).", text, mode)
			.category(DebugCategory.CAST)
			.configure(spell)
		)) {
			return castAtEntity(new SpellData(caster, target, power, null), passTargeting).success();
		}
	}

	@NotNull
	private SpellCastResult castAtEntity(@NotNull SpellData data, boolean passTargeting) {
		if (!isTargetedEntity) {
			if (isTargetedLocation) return castAtLocation(data);
			return fail(data);
		}

		data = data.builder()
			.recipient(null)
			.power((passPower.get(data) ? data.power() : 1) * subPower.get(data))
			.args(passArgs.get(data) ? data.args() : args.get(data))
			.build();

		MagicDebug.info("Subspell spell data: %s", data);

		if (mode != CastMode.HARD && !this.passTargeting.getOr(data, passTargeting)) {
			ValidTargetList canTarget = spell.getValidTargetList();

			if (!canTarget.canTarget(data.caster(), data.target())) {
				MagicDebug.info("Invalid target - cast failed.");
				return wrapResult(spell.noTarget(data));
			}
		}

		double chance = this.chance.get(data), roll;
		if ((chance > 0 && chance < 1) && (roll = random.nextDouble()) > chance) {
			MagicDebug.info("Chance rolled too high (%s > %s) - casted failed.", roll, chance);
			return fail(data);
		}

		int delay = this.delay.get(data);
		if (delay < 0) {
			MagicDebug.info("Casting with no delay.");
			return castAtEntityReal(data.noLocation());
		}

		SpellData finalData = data.noLocation();
		MagicSpells.scheduleDelayedTask(() -> {
			try (var ignored = MagicDebug.section(DebugCategory.CAST, "Casting delayed subspell '%s' (mode: %s, targeting: ENTITY).", text, mode)) {
				castAtEntityReal(finalData);
			}
		}, delay);

		MagicDebug.info("Casting with a delay (%d ticks).", delay);
		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castAtEntityReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD -> spell.hardCast(data);
			case DIRECT -> {
				TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
				yield wrapResult(targetedSpell.castAtEntity(data));
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				castEvent.callEvent();

				SpellCastState state = castEvent.getSpellCastState();
				if (state != SpellCastState.NORMAL) {
					MagicDebug.info("Spell cast state is %s - cast failed.", state);
					yield postCast(castEvent, null, true);
				}

				data = castEvent.getSpellData();

				SpellTargetEvent targetEvent = new SpellTargetEvent(spell, data);
				if (!targetEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetEvent), true);

				data = targetEvent.getSpellData();

				TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
				CastResult result = targetedSpell.castAtEntity(data);

				yield postCast(castEvent, result, true);
			}
			case FULL -> {
				SpellCastEvent castEvent = spell.preCast(data);

				SpellCastState state = castEvent.getSpellCastState();
				if (state != SpellCastState.NORMAL) {
					MagicDebug.info("Spell cast state is %s - cast failed.", state);
					yield postCast(castEvent, null, false);
				}

				data = castEvent.getSpellData();

				SpellTargetEvent targetEvent = new SpellTargetEvent(spell, data);
				if (!targetEvent.callEvent()) {
					MagicDebug.info("Entity targeting cancelled - cast failed.");
					yield postCast(castEvent, spell.noTarget(targetEvent), false);
				}

				data = targetEvent.getSpellData();

				TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
				CastResult result = targetedSpell.castAtEntity(data);

				yield postCast(castEvent, result, false);
			}
		};
	}

	@Deprecated
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		try (var ignored = MagicDebug.section(builder -> builder
			.message("Legacy casting subspell '%s' (mode: %s, targeting: LOCATION).", text, mode)
			.category(DebugCategory.CAST)
			.configure(spell)
		)) {
			return castAtLocation(new SpellData(caster, target, power, null)).success();
		}
	}

	@NotNull
	private SpellCastResult castAtLocation(@NotNull SpellData data) {
		if (!isTargetedLocation) return fail(data);

		data = data.builder()
			.recipient(null)
			.power((passPower.get(data) ? data.power() : 1) * subPower.get(data))
			.args(passArgs.get(data) ? data.args() : args.get(data))
			.build();

		MagicDebug.info("Subspell spell data: %s", data);

		double chance = this.chance.get(data), roll;
		if ((chance > 0 && chance < 1) && (roll = random.nextDouble()) > chance) {
			MagicDebug.info("Chance rolled too high (%s > %s) - casted failed.", roll, chance);
			return fail(data);
		}

		int delay = this.delay.get(data);
		if (delay < 0) {
			MagicDebug.info("Casting with no delay.");
			return castAtLocationReal(data.noTarget());
		}

		SpellData finalData = data.noTarget();
		MagicSpells.scheduleDelayedTask(() -> {
			try (var ignored = MagicDebug.section(DebugCategory.CAST, "Casting delayed subspell '%s' (mode: %s, targeting: LOCATION).", text, mode)) {
				castAtLocationReal(finalData);
			}
		}, delay);

		MagicDebug.info("Casting with a delay (%d ticks).", delay);
		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castAtLocationReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD -> spell.hardCast(data);
			case DIRECT -> {
				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				yield wrapResult(targetedSpell.castAtLocation(data));
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				castEvent.callEvent();

				SpellCastState state = castEvent.getSpellCastState();
				if (state != SpellCastState.NORMAL) {
					MagicDebug.info("Spell cast state is %s - cast failed.", state);
					yield postCast(castEvent, null, true);
				}

				data = castEvent.getSpellData();

				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetEvent.callEvent()) {
					MagicDebug.info("Location targeting cancelled - cast failed.");
					yield postCast(castEvent, spell.noTarget(targetEvent), true);
				}

				data = targetEvent.getSpellData();

				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				CastResult result = targetedSpell.castAtLocation(data);

				yield postCast(castEvent, result, true);
			}
			case FULL -> {
				SpellCastEvent castEvent = spell.preCast(data);

				SpellCastState state = castEvent.getSpellCastState();
				if (state != SpellCastState.NORMAL) {
					MagicDebug.info("Spell cast state is %s - cast failed.", state);
					yield postCast(castEvent, null, false);
				}

				data = castEvent.getSpellData();

				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetEvent.callEvent()) {
					MagicDebug.info("Location targeting cancelled - cast failed.");
					yield postCast(castEvent, spell.noTarget(targetEvent), false);
				}

				data = targetEvent.getSpellData();

				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				CastResult result = targetedSpell.castAtLocation(data);

				yield postCast(castEvent, result, false);
			}
		};
	}

	@Deprecated
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		try (var ignored = MagicDebug.section(builder -> builder
			.message("Legacy casting subspell '%s' (mode: %s, targeting: ENTITY_FROM_LOCATION).", text, mode)
			.category(DebugCategory.CAST)
			.configure(spell)
		)) {
			return castAtEntityFromLocation(new SpellData(caster, target, from, power, null), false).success();
		}
	}

	@Deprecated
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, boolean passTargeting) {
		try (var ignored = MagicDebug.section(builder -> builder
			.message("Legacy casting subspell '%s' (mode: %s, targeting: ENTITY_FROM_LOCATION).", text, mode)
			.category(DebugCategory.CAST)
			.configure(spell)
		)) {
			return castAtEntityFromLocation(new SpellData(caster, target, from, power, null), passTargeting).success();
		}
	}

	@NotNull
	private SpellCastResult castAtEntityFromLocation(@NotNull SpellData data, boolean passTargeting) {
		if (!isTargetedEntityFromLocation) return fail(data);

		data = data.builder()
			.recipient(null)
			.power((passPower.get(data) ? data.power() : 1) * subPower.get(data))
			.args(passArgs.get(data) ? data.args() : args.get(data))
			.build();

		MagicDebug.info("Subspell spell data: %s", data);

		if (mode != CastMode.HARD && !this.passTargeting.getOr(data, passTargeting)) {
			ValidTargetList canTarget = spell.getValidTargetList();

			if (!canTarget.canTarget(data.caster(), data.target())) {
				MagicDebug.info("Invalid target - cast failed.");
				return wrapResult(spell.noTarget(data));
			}
		}

		double chance = this.chance.get(data), roll;
		if ((chance > 0 && chance < 1) && (roll = random.nextDouble()) > chance) {
			MagicDebug.info("Chance rolled too high (%s > %s) - casted failed.", roll, chance);
			return fail(data);
		}

		int delay = this.delay.get(data);
		if (delay < 0) {
			MagicDebug.info("Casting with no delay.");
			return castAtEntityFromLocationReal(data);
		}

		SpellData finalData = data;
		MagicSpells.scheduleDelayedTask(() -> {
			try (var ignored = MagicDebug.section(DebugCategory.CAST, "Casting delayed subspell '%s' (mode: %s, targeting: ENTITY).", text, mode)) {
				castAtEntityFromLocationReal(finalData);
			}
		}, delay);

		MagicDebug.info("Casting with a delay (%d ticks).", delay);
		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castAtEntityFromLocationReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD -> spell.hardCast(data);
			case DIRECT -> {
				TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
				yield wrapResult(targetedSpell.castAtEntityFromLocation(data));
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				castEvent.callEvent();

				SpellCastState state = castEvent.getSpellCastState();
				if (state != SpellCastState.NORMAL) {
					MagicDebug.info("Spell cast state is %s - cast failed.", state);
					yield postCast(castEvent, null, true);
				}

				data = castEvent.getSpellData();

				SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, data);
				if (!targetEntityEvent.callEvent()) {
					MagicDebug.info("Entity targeting cancelled - cast failed.");
					yield postCast(castEvent, spell.noTarget(targetEntityEvent), true);
				}

				data = targetEntityEvent.getSpellData();

				SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetLocationEvent.callEvent()) {
					MagicDebug.info("Location targeting cancelled - cast failed.");
					yield postCast(castEvent, spell.noTarget(targetLocationEvent), true);
				}

				data = targetLocationEvent.getSpellData();

				TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
				CastResult result = targetedSpell.castAtEntityFromLocation(data);

				yield postCast(castEvent, result, true);
			}
			case FULL -> {
				SpellCastEvent castEvent = spell.preCast(data);

				SpellCastState state = castEvent.getSpellCastState();
				if (state != SpellCastState.NORMAL) {
					MagicDebug.info("Spell cast state is %s - cast failed.", state);
					yield postCast(castEvent, null, false);
				}

				data = castEvent.getSpellData();

				SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, data);
				if (!targetEntityEvent.callEvent()) {
					MagicDebug.info("Entity targeting cancelled - cast failed.");
					yield postCast(castEvent, spell.noTarget(targetEntityEvent), false);
				}

				data = targetEntityEvent.getSpellData();

				SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetLocationEvent.callEvent()) {
					MagicDebug.info("Location targeting cancelled - cast failed.");
					yield postCast(castEvent, spell.noTarget(targetLocationEvent), false);
				}

				data = targetLocationEvent.getSpellData();

				TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
				CastResult result = targetedSpell.castAtEntityFromLocation(data);

				yield postCast(castEvent, result, false);
			}
		};
	}

	@NotNull
	private SpellCastResult wrapResult(@NotNull CastResult result) {
		return new SpellCastResult(SpellCastState.NORMAL, result.action(), result.data());
	}

	@NotNull
	private SpellCastResult fail(@NotNull SpellData data) {
		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.ALREADY_HANDLED, data);
	}

	@NotNull
	private SpellCastResult postCast(@NotNull SpellCastEvent castEvent, @Nullable CastResult result, boolean partial) {
		PostCastAction action = result == null ? PostCastAction.HANDLE_NORMALLY : result.action();
		SpellCastState state = castEvent.getSpellCastState();
		SpellData data = result == null ? castEvent.getSpellData() : result.data();

		if (partial) {
			new SpellCastedEvent(spell, state, action, data, 0, null).callEvent();
			return new SpellCastResult(state, action, data);
		}

		spell.postCast(castEvent, action, data);
		return new SpellCastResult(state, action, data);
	}

	public enum CastMode {

		HARD("h"),
		FULL("f"),
		PARTIAL("p"),
		DIRECT("d");

		private static final Map<String, CastMode> nameMap = new HashMap<>();

		private final String[] names;

		CastMode(String... names) {
			this.names = names;
		}

		public static CastMode getFromString(String label) {
			return nameMap.get(label.toLowerCase());
		}

		static {
			for (CastMode mode : CastMode.values()) {
				nameMap.put(mode.name().toLowerCase(), mode);
				for (String s : mode.names) nameMap.put(s.toLowerCase(), mode);
			}
		}

	}

	public enum CastTargeting {

		NORMAL,
		ENTITY_FROM_LOCATION,
		ENTITY,
		LOCATION,
		NONE;

		private static final CastTargeting[] DEFAULT_ORDERING = {ENTITY_FROM_LOCATION, ENTITY, LOCATION, NONE};

	}

}
