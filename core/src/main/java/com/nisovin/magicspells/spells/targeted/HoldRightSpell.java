package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class HoldRightSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private final Map<UUID, CastData> casting;

	private final ConfigData<Integer> resetTime;

	private final ConfigData<Float> maxDuration;
	private final ConfigData<Float> maxDistance;

	private final ConfigData<Boolean> targetEntity;
	private final ConfigData<Boolean> targetLocation;

	private Subspell spellToCast;
	private String spellToCastName;

	public HoldRightSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		resetTime = getConfigDataInt("reset-time", 250);

		maxDuration = getConfigDataFloat("max-duration", 0F);
		maxDistance = getConfigDataFloat("max-distance", 0F);

		targetEntity = getConfigDataBoolean("target-entity", true);
		targetLocation = getConfigDataBoolean("target-location", false);

		spellToCastName = getConfigString("spell", "");

		casting = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = initSubspell(spellToCastName, false, "for 'spell'");
		spellToCastName = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		CastData castData = casting.get(data.caster().getUniqueId());
		if (castData != null && castData.isValid()) {
			castData.cast();
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		if (targetEntity.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);

			castData = new CastData(info.spellData());
		} else if (targetLocation.get(data)) {
			TargetInfo<Location> info = getTargetedBlockLocation(data, 0.5, 0.5, 0.5, false);
			if (info.noTarget()) return noTarget(info);

			castData = new CastData(info.spellData());
		} else castData = new CastData(data);

		castData.cast();
		casting.put(data.caster().getUniqueId(), castData);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster() || !targetLocation.get(data)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		CastData castData = casting.get(data.caster().getUniqueId());
		if (castData != null && castData.isValid()) {
			castData.cast();
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		castData = new CastData(data);
		castData.cast();
		casting.put(data.caster().getUniqueId(), castData);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster() || !targetEntity.get(data)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		CastData castData = casting.get(data.caster().getUniqueId());
		if (castData != null && castData.isValid()) {
			castData.cast();
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		castData = new CastData(data);
		castData.cast();
		casting.put(data.caster().getUniqueId(), castData);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private class CastData {

		private final SpellData data;

		private final float maxDistance;
		private final float maxDuration;
		private final int resetTime;

		private final long start = System.currentTimeMillis();
		private long lastCast = 0;

		private CastData(SpellData data) {
			this.data = data;

			maxDistance = HoldRightSpell.this.maxDistance.get(data);
			maxDuration = HoldRightSpell.this.maxDuration.get(data);
			resetTime = HoldRightSpell.this.resetTime.get(data);
		}

		private boolean isValid() {
			if (lastCast < System.currentTimeMillis() - resetTime) return false;
			if (maxDuration > 0 && System.currentTimeMillis() - start > maxDuration * TimeUtil.MILLISECONDS_PER_SECOND)
				return false;

			if (maxDistance > 0) {
				Location l = data.location();
				if (data.target() != null) l = data.target().getLocation();
				if (l == null) return false;
				if (!l.getWorld().equals(data.caster().getWorld())) return false;

				return l.distanceSquared(data.caster().getLocation()) <= maxDistance * maxDistance;
			}

			return true;
		}

		private void cast() {
			lastCast = System.currentTimeMillis();

			spellToCast.subcast(data);
			playSpellEffects(data);
		}

	}

}
