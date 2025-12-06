package com.nisovin.magicspells.util;

public record ModifierResult(SpellData data, boolean check) {

	public ModifierResult data(SpellData data) {
		return this.data.equals(data) ? this : new ModifierResult(data, check);
	}

	public ModifierResult check(boolean check) {
		return this.check == check ? this : new ModifierResult(data, check);
	}

}
