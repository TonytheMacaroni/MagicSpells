package com.nisovin.magicspells.castmodifiers.customdata;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.FunctionData;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class CustomDataFloat extends CustomData {

	private String invalidText;
	private boolean isValid = false;
	private ConfigData<Float> customData;

	public CustomDataFloat(String data) {
		if (data == null) {
			invalidText = "Number was not defined.";
			return;
		}

		try {
			float value = Float.parseFloat(data);
			customData = spellData -> value;
		} catch (NumberFormatException e) {
			customData = FunctionData.build(data, Double::floatValue, 0f, true);
			if (customData == null) {
				invalidText = "Number or function is invalid.";
				return;
			}
		}

		isValid = true;
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	@Override
	public String getInvalidText() {
		return invalidText;
	}

	public float get(SpellData data) {
		return customData.get(data);
	}

	public static float from(CustomData data, SpellData spellData) {
		return ((CustomDataFloat) data).get(spellData);
	}

	public static float from(CustomData data, SpellCastEvent event) {
		return ((CustomDataFloat) data).get(event.getSpellData());
	}

	public static float from(CustomData data, ManaChangeEvent event) {
		return ((CustomDataFloat) data).get(new SpellData(event.getPlayer()));
	}

	public static float from(CustomData data, SpellTargetEvent event) {
		return ((CustomDataFloat) data).get(event.getSpellData());
	}

	public static float from(CustomData data, SpellTargetLocationEvent event) {
		return ((CustomDataFloat) data).get(event.getSpellData());
	}

	public static float from(CustomData data, MagicSpellsGenericPlayerEvent event) {
		return ((CustomDataFloat) data).get(new SpellData(event.getPlayer()));
	}

}
