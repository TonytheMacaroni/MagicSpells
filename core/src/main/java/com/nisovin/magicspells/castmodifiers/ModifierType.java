package com.nisovin.magicspells.castmodifiers;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.VariableMod.VariableOwner;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.castmodifiers.customdata.CustomData;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;
import com.nisovin.magicspells.castmodifiers.customdata.CustomDataFloat;

public enum ModifierType {

	REQUIRED(false, "required", "require") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - cancelling spell cast.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition passed - continuing.");

				return check;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - cancelling mana change.");
					event.setNewAmount(event.getOldAmount());
				} else MagicDebug.info("Condition passed - continuing.");

				return check;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - cancelling entity targeting.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition passed - continuing.");

				return check;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - cancelling location targeting.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition passed - continuing.");

				return check;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - cancelling.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition passed - continuing.");

				return check;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return result;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return result;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'required' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return result;
			}
		}

	},

	DENIED(false, "denied", "deny") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (check) {
					MagicDebug.info("Condition passed - cancelling spell cast.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (check) {
					MagicDebug.info("Condition passed - cancelling mana change.");
					event.setNewAmount(event.getOldAmount());
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (check) {
					MagicDebug.info("Condition passed - cancelling entity targeting.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (check) {
					MagicDebug.info("Condition passed - cancelling location targeting.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (check) {
					MagicDebug.info("Condition passed - cancelling.");
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'denied' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

	},

	POWER(true, "power", "empower", "multiply") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'power' modifier action.")){
				if (check) {
					float oldPower = event.getPower();
					event.increasePower((CustomDataFloat.from(customData, event)));
					float newPower = event.getPower();

					MagicDebug.info("Condition passed - changing power from %s to %s.", oldPower, newPower);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'power' modifier action.")) {
				if (check) {
					int diff = event.getNewAmount() - event.getOldAmount();
					diff = Math.round(diff * CustomDataFloat.from(customData, event));

					int oldMana = event.getOldAmount();
					int newMana = event.getNewAmount();
					int modifiedMana = Math.clamp(oldMana + diff, 0, event.getMaxMana());

					MagicDebug.info("Condition passed. Mana change modified from (%s -> %s) to (%s -> %s).", oldMana, newMana, oldMana, modifiedMana);
					event.setNewAmount(modifiedMana);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'power' modifier action.")){
				if (check) {
					float oldPower = event.getPower();
					event.increasePower((CustomDataFloat.from(customData, event)));
					float newPower = event.getPower();

					MagicDebug.info("Condition passed - changing power from %s to %s.", oldPower, newPower);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'power' modifier action.")){
				if (check) {
					float oldPower = event.getPower();
					event.increasePower((CustomDataFloat.from(customData, event)));
					float newPower = event.getPower();

					MagicDebug.info("Condition passed - changing power from %s to %s.", oldPower, newPower);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'power' modifier action - continuing.");
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'power' modifier action.")) {
				if (result.check()) {
					SpellData data = result.data();
					float newPower = data.power() * CustomDataFloat.from(customData, data);

					MagicDebug.info("Condition passed - changing power from %s to %s.", data.power(), newPower);
					return new ModifierResult(data.power(newPower), true);
				}

				return result.check(true);
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'power' modifier action.")) {
				if (result.check()) {
					SpellData data = result.data();
					float newPower = data.power() * CustomDataFloat.from(customData, data);

					MagicDebug.info("Condition passed - changing power from %s to %s.", data.power(), newPower);
					return new ModifierResult(data.power(newPower), true);
				}

				return result.check(true);
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'power' modifier action.")) {
				if (result.check()) {
					SpellData data = result.data();
					float newPower = data.power() * CustomDataFloat.from(customData, data);

					MagicDebug.info("Condition passed - changing power from %s to %s.", data.power(), newPower);
					return new ModifierResult(data.power(newPower), true);
				}

				return result.check(true);
			}
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}

	},

	ADD_POWER(true, "addpower", "add") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'addpower' modifier action.")){
				if (check) {
					float oldPower = event.getPower();
					float newPower = oldPower + CustomDataFloat.from(customData, event);

					MagicDebug.info("Condition passed - changing power from %s to %s.", oldPower, newPower);
					event.setPower(newPower);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'addpower' modifier action.")) {
				if (check) {
					int oldMana = event.getOldAmount();
					int newMana = event.getNewAmount();
					int modifiedMana = Math.clamp(newMana + (int) CustomDataFloat.from(customData, event), 0, event.getMaxMana());

					MagicDebug.info("Condition passed. Mana change modified from (%s -> %s) to (%s -> %s).", oldMana, newMana, oldMana, modifiedMana);
					event.setNewAmount(modifiedMana);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'addpower' modifier action.")){
				if (check) {
					float oldPower = event.getPower();
					float newPower = oldPower + CustomDataFloat.from(customData, event);

					MagicDebug.info("Condition passed - changing power from %s to %s.", oldPower, newPower);
					event.setPower(newPower);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'addpower' modifier action.")){
				if (check) {
					float oldPower = event.getPower();
					float newPower = oldPower + CustomDataFloat.from(customData, event);

					MagicDebug.info("Condition passed - changing power from %s to %s.", oldPower, newPower);
					event.setPower(newPower);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'addpower' modifier action - continuing.");
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'addpower' modifier action.")) {
				if (result.check()) {
					SpellData data = result.data();
					float newPower = data.power() + CustomDataFloat.from(customData, data);

					MagicDebug.info("Condition passed - changing power from %s to %s.", data.power(), newPower);
					return new ModifierResult(data.power(newPower), true);
				}

				return result.check(true);
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'addpower' modifier action.")) {
				if (result.check()) {
					SpellData data = result.data();
					float newPower = data.power() + CustomDataFloat.from(customData, data);

					MagicDebug.info("Condition passed - changing power from %s to %s.", data.power(), newPower);
					return new ModifierResult(data.power(newPower), true);
				}

				return result.check(true);
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'addpower' modifier action.")) {
				if (result.check()) {
					SpellData data = result.data();
					float newPower = data.power() + CustomDataFloat.from(customData, data);

					MagicDebug.info("Condition passed - changing power from %s to %s.", data.power(), newPower);
					return new ModifierResult(data.power(newPower), true);
				}

				return result.check(true);
			}
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}

	},

	COOLDOWN(true, "cooldown") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cooldown' modifier action.")) {
				if (check) {
					float oldCooldown = event.getCooldown();
					float newCooldown = CustomDataFloat.from(customData, event);

					MagicDebug.info("Condition passed - modifying cooldown from %s to %s.", oldCooldown, newCooldown);
					event.setCooldown(newCooldown);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'cooldown' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'cooldown' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'cooldown' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'cooldown' modifier action - continuing.");
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'cooldown' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'cooldown' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'cooldown' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}

	},

	REAGENTS(true, "reagents") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'reagents' modifier action.")) {
				if (check) {
					float multiplier = CustomDataFloat.from(customData, event);
					MagicDebug.info("Condition passed - multiplying reagents by %s.", multiplier);

					SpellReagents reagents = event.getReagents();
					MagicDebug.info("Old reagents: %s.", reagents);

					reagents.multiply(multiplier);
					MagicDebug.info("New reagents: %s.", reagents);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'reagents' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'reagents' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'reagents' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'reagents' modifier action - continuing.");
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'reagents' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'reagents' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'reagents' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}

	},

	CAST_TIME(true, "casttime") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'casttime' modifier action.")) {
				if (check) {
					int oldCastTime = event.getCastTime();
					int newCastTime = (int) CustomDataFloat.from(customData, event);

					MagicDebug.info("Condition passed - modifying cast time from %s to %s.", oldCastTime, newCastTime);
					event.setCastTime(newCastTime);
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'casttime' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'casttime' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'casttime' modifier action - continuing.");
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'casttime' modifier action - continuing.");
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'casttime' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'casttime' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			MagicDebug.info(DebugCategory.MODIFIERS, "Attempted to perform invalid 'casttime' modifier action - continuing.");
			return result.check(true);
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}

	},

	STOP(false, "stop") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'stop' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - stopping.");
				else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

	},

	CONTINUE(false, "continue") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return check;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return check;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return check;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return check;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (check) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return check;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return result;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return result;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'continue' modifier action.")) {
				if (result.check()) MagicDebug.info("Condition passed - continuing.");
				else MagicDebug.info("Condition failed - stopping.");

				return result;
			}
		}

	},

	CAST(true, "cast") {

		static class CastData extends CustomData {

			public String invalidText;

			public Subspell spell;

			@Override
			public boolean isValid() {
				return spell != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(event.getSpellData().noTargeting());
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(new SpellData(event.getPlayer()));
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(event.getSpellData().noLocation());
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(event.getSpellData().noTarget());
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(new SpellData(event.getPlayer()));
				} else MagicDebug.info("Condition failed - continuing.");

				return true;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (result.check()) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(result.data().noTargeting());
				} else MagicDebug.info("Condition failed - continuing.");

				return result.check(true);
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (result.check()) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(result.data().noLocation());
				} else MagicDebug.info("Condition failed - continuing.");

				return result.check(true);
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'cast' modifier action.")) {
				if (result.check()) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell.");
					data.spell.subcast(result.data().noTarget());
				} else MagicDebug.info("Condition failed - continuing.");

				return result.check(true);
			}
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			CastData data = new CastData();
			if (text == null) {
				data.invalidText = "No spell defined.";
				return data;
			}

			Subspell spell = new Subspell(text);
			if (spell.process()) data.spell = spell;
			else data.invalidText = "Spell '" + text + "' does not exist.";

			return data;
		}

	},

	CAST_INSTEAD(true, "castinstead") {

		static class CastData extends CustomData {

			public String invalidText;

			public Subspell spell;

			@Override
			public boolean isValid() {
				return spell != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell and cancelling spell cast.");
					data.spell.subcast(event.getSpellData().noTargeting());
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell and cancelling mana change.");
					data.spell.subcast(new SpellData(event.getPlayer()));
					event.setNewAmount(event.getOldAmount());
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell, and cancelling entity targeting and spell cast.");
					data.spell.subcast(event.getSpellData().noLocation());
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell and cancelling location targeting.");
					data.spell.subcast(event.getSpellData().noTarget());
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (check) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell and cancelling.");
					data.spell.subcast(new SpellData(event.getPlayer()));
					event.setCancelled(true);
				} else MagicDebug.info("Condition failed - continuing.");

				return !check;
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (result.check()) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell and stopping.");
					data.spell.subcast(result.data().noTargeting());
				} else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (result.check()) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell and stopping.");
					data.spell.subcast(result.data().noLocation());
				} else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'castinstead' modifier action.")) {
				if (result.check()) {
					CastData data = (CastData) customData;

					MagicDebug.info("Condition passed - casting spell and stopping.");
					data.spell.subcast(result.data().noTarget());
				} else MagicDebug.info("Condition failed - continuing.");

				return new ModifierResult(result.data(), !result.check());
			}
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			CastData data = new CastData();
			if (text == null) {
				data.invalidText = "No spell defined.";
				return data;
			}

			Subspell spell = new Subspell(text);
			if (spell.process()) data.spell = spell;
			else data.invalidText = "Spell '" + text + "' does not exist.";

			return data;
		}

	},

	VARIABLE_MODIFY(true, "variable") {

		static class VariableModData extends CustomData {

			private String invalidText = "Variable action is invalid.";

			public VariableOwner variableOwner;
			public Variable variable;
			public VariableMod mod;

			@Override
			public boolean isValid() {
				return variable != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}

		private void modifyVariable(boolean check, CustomData customData, SpellData spellData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'variable' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - continuing.");
					return;
				}

				VariableModData data = (VariableModData) customData;

				LivingEntity owner = data.variableOwner == VariableOwner.CASTER ? spellData.caster() : spellData.target();
				if (!(owner instanceof Player playerOwner)) return;

				MagicDebug.info("Condition passed - performing variable mod.");
				MagicSpells.getVariableManager().processVariableMods(data.variable, data.mod, playerOwner, spellData);
			}
		}

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			modifyVariable(check, customData, event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			modifyVariable(check, customData, new SpellData(event.getPlayer()));
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			modifyVariable(check, customData, event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			modifyVariable(check, customData, event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			modifyVariable(check, customData, new SpellData(event.getPlayer()));
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			modifyVariable(result.check(), customData, result.data());
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			modifyVariable(result.check(), customData, result.data());
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			modifyVariable(result.check(), customData, result.data());
			return result.check(true);
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			VariableModData data = new VariableModData();
			if (text == null) {
				data.invalidText = "No data action data defined.";
				return data;
			}

			if (!text.contains(";")) {
				data.invalidText = "Data is invalid.";
				return data;
			}

			String[] splits = text.split(";", 2);
			if (splits.length < 2) {
				data.invalidText = "VarMod is not defined.";
				return data;
			}

			String varData = splits[0];
			VariableOwner variableOwner = VariableOwner.CASTER;
			String variableName;
			if (varData.contains(":")) {
				String[] varDataSplits = varData.split(":");
				if (varDataSplits[0].startsWith("target")) variableOwner = VariableOwner.TARGET;
				variableName = varDataSplits[1];
			} else variableName = varData;

			data.variableOwner = variableOwner;
			data.mod = new VariableMod(splits[1]);
			data.variable = MagicSpells.getVariableManager().getVariable(variableName);
			if (data.variable == null) data.invalidText = "Variable does not exist.";
			return data;
		}

	},

	STRING(true, "string") {

		static class StringData extends CustomData {

			public String invalidText;

			public Variable variable;
			public String value;

			@Override
			public boolean isValid() {
				return variable != null && value != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}

		private void setVariable(boolean check, CustomData customData, SpellData spellData) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'string' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - continuing.");
					return;
				}

				StringData data = (StringData) customData;
				if (!(spellData.caster() instanceof Player caster)) {
					MagicDebug.info("Condition passed, but target is not a player - continuing.");
					return;
				}

				String value = MagicSpells.doReplacements(data.value, spellData);
				MagicDebug.info("Condition passed - setting variable '%s' to value '%s'.", data.variable, value);
				MagicSpells.getVariableManager().set(data.variable, caster.getName(), value);
			}
		}

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			setVariable(check, customData, event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			setVariable(check, customData, new SpellData(event.getPlayer()));
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			setVariable(check, customData, event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			setVariable(check, customData, event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			setVariable(check, customData, new SpellData(event.getPlayer()));
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			setVariable(result.check(), customData, result.data());
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			setVariable(result.check(), customData, result.data());
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			setVariable(result.check(), customData, result.data());
			return result.check(true);
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			StringData data = new StringData();
			if (text == null || text.trim().isEmpty() || !text.contains(" ")) {
				data.invalidText = "Data is invalid.";
				return data;
			}

			String[] splits = text.split(" ", 2);
			data.variable = MagicSpells.getVariableManager().getVariable(splits[0]);
			if (data.variable == null) data.invalidText = "Variable does not exist.";
			data.value = splits[1];
			return data;
		}

	},

	MESSAGE(true, "message") {

		static class MessageData extends CustomData {

			public final String message;

			MessageData(String message) {
				this.message = message;
			}

			@Override
			public boolean isValid() {
				return message != null && !message.isEmpty();
			}

			@Override
			public String getInvalidText() {
				return "No message specified.";
			}

		}

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			sendMessage(check, customData, event.getCaster(), event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			sendMessage(check, customData, event.getPlayer(), SpellData.NULL);
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			sendMessage(check, customData, event.getCaster(), event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			sendMessage(check, customData, event.getCaster(), event.getSpellData());
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			sendMessage(check, customData, event.getPlayer(), SpellData.NULL);
			return true;
		}

		@Override
		public ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData) {
			sendMessage(result.check(), customData, caster, result.data());
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData) {
			sendMessage(result.check(), customData, caster, result.data());
			return result.check(true);
		}

		@Override
		public ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData) {
			sendMessage(result.check(), customData, caster, result.data());
			return result.check(true);
		}

		private void sendMessage(boolean check, CustomData customData, LivingEntity caster, SpellData data) {
			try (var ignored = MagicDebug.section(DebugCategory.MODIFIERS, "Performing 'message' modifier action.")) {
				if (!check) {
					MagicDebug.info("Condition failed - continuing.");
					return;
				}

				MagicDebug.info("Condition passed - messaging.");
				MagicSpells.sendMessage(((MessageData) customData).message, caster, data);
			}
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new MessageData(text);
		}

	}

	;

	private final String[] keys;
	private static boolean initialized = false;

	private final boolean usesCustomData;

	ModifierType(boolean usesCustomData, String... keys) {
		this.keys = keys;
		this.usesCustomData = usesCustomData;
	}

	public boolean usesCustomData() {
		return usesCustomData;
	}

	public abstract boolean apply(SpellCastEvent event, boolean check, CustomData customData);
	public abstract boolean apply(ManaChangeEvent event, boolean check, CustomData customData);
	public abstract boolean apply(SpellTargetEvent event, boolean check, CustomData customData);
	public abstract boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData);
	public abstract boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData);

	public abstract ModifierResult apply(LivingEntity caster, ModifierResult result, CustomData customData);
	public abstract ModifierResult apply(LivingEntity caster, LivingEntity target, ModifierResult result, CustomData customData);
	public abstract ModifierResult apply(LivingEntity caster, Location target, ModifierResult result, CustomData customData);

	public CustomData buildCustomActionData(String text) {
		return null;
	}

	static Map<String, ModifierType> nameMap;

	static void initialize() {
		nameMap = new HashMap<>();
		for (ModifierType type : ModifierType.values()) {
			for (String key : type.keys) {
				nameMap.put(key.toLowerCase(), type);
			}
		}
		initialized = true;
	}

	public static ModifierType getModifierTypeByName(String name) {
		if (!initialized) initialize();
		return nameMap.get(name.toLowerCase());
	}

}
