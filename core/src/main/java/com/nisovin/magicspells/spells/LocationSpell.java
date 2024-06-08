package com.nisovin.magicspells.spells;

import org.bukkit.Location;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.config.ConfigData;

public class LocationSpell extends InstantSpell {

	private Subspell spellToCast;

	private String spellToCastName;

	private final ConfigData<String> location;

	public LocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellToCastName = getConfigString("spell", "");
		location = getConfigDataString("location", "world,0,0,0");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = initSubspell(spellToCastName, false, "spell");
		spellToCastName = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		Location location = LocationUtil.fromString(LocationSpell.this.location.get(data));
		if (location == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		data = data.location(location);
		if (spellToCast != null) spellToCast.subcast(data);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
