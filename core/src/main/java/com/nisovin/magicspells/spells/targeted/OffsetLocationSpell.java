package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class OffsetLocationSpell extends TargetedSpell implements TargetedLocationSpell {

	private final ConfigData<Vector> relativeOffset;
	private final ConfigData<Vector> absoluteOffset;

	private final ConfigData<Angle> yaw;
	private final ConfigData<Angle> pitch;

	private Subspell spellToCast;
	private String spellToCastName;

	public OffsetLocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigDataVector("relative-offset", new Vector());
		absoluteOffset = getConfigDataVector("absolute-offset", new Vector());

		yaw = getConfigDataAngle("yaw", Angle.DEFAULT);
		pitch = getConfigDataAngle("pitch", Angle.DEFAULT);

		spellToCastName = getConfigString("spell", "");
	}

	public void initialize() {
		super.initialize();

		spellToCast = initSubspell(spellToCastName, false, "spell");
		spellToCastName = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> entityInfo = getTargetedEntity(data);
		if (entityInfo.cancelled()) return noTarget(entityInfo);

		if (entityInfo.empty()) {
			TargetInfo<Location> locationInfo = getTargetedBlockLocation(data);
			if (locationInfo.noTarget()) return noTarget(locationInfo);
			data = locationInfo.spellData();
		} else {
			data = entityInfo.spellData();
			data = data.builder().target(null).location(data.target().getLocation()).build();
		}

		return castAtLocation(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location location = data.location();

		Vector absoluteOffset = this.absoluteOffset.get(data);
		Vector relativeOffset = this.relativeOffset.get(data);
		Angle pitch = this.pitch.get(data);
		Angle yaw = this.yaw.get(data);

		Util.applyRelativeOffset(location, relativeOffset);
		location.add(absoluteOffset);
		location.setYaw(yaw.apply(location.getYaw()));
		location.setPitch(pitch.apply(location.getPitch()));

		data = data.location(location);

		playSpellEffects(data);
		if (spellToCast != null) spellToCast.subcast(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
