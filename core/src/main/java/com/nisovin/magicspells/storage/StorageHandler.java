package com.nisovin.magicspells.storage;

import com.nisovin.magicspells.Spellbook;

public interface StorageHandler {

	default void enable() {

	}

	default void disable() {

	}

	void load(Spellbook spellbook);

	void save(Spellbook spellbook);

}
