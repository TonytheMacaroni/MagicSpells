package com.nisovin.magicspells.mana;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ManaRank {
	
	private String name;
	private Component prefix;

	private char symbol;

	private int barSize;
	private int maxMana;
	private int startingMana;
	private int regenAmount;
	private int regenInterval;

	private TextColor colorFull;
	private TextColor colorEmpty;

	ManaRank() {

	}

	ManaRank(String name, Component prefix, char symbol, int barSize, int maxMana, int startingMana, int regenAmount, int regenInterval, TextColor colorFull, TextColor colorEmpty) {
		this.name = name;
		this.prefix = prefix;
		this.symbol = symbol;
		this.barSize = barSize;
		this.maxMana = maxMana;
		this.startingMana = startingMana;
		this.regenAmount = regenAmount;
		this.regenInterval = regenInterval;
		this.colorFull = colorFull;
		this.colorEmpty = colorEmpty;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Component getPrefix() {
		return prefix;
	}

	public void setPrefix(Component prefix) {
		this.prefix = prefix;
	}

	public char getSymbol() {
		return symbol;
	}

	public void setSymbol(char symbol) {
		this.symbol = symbol;
	}

	public int getBarSize() {
		return barSize;
	}

	public void setBarSize(int barSize) {
		this.barSize = barSize;
	}

	public int getMaxMana() {
		return maxMana;
	}

	public void setMaxMana(int maxMana) {
		this.maxMana = maxMana;
	}

	public int getStartingMana() {
		return startingMana;
	}

	public void setStartingMana(int startingMana) {
		this.startingMana = startingMana;
	}

	public int getRegenAmount() {
		return regenAmount;
	}

	public void setRegenAmount(int regenAmount) {
		this.regenAmount = regenAmount;
	}

	public int getRegenInterval() {
		return regenInterval;
	}

	public void setRegenInterval(int regenInterval) {
		this.regenInterval = regenInterval;
	}

	public TextColor getColorFull() {
		return colorFull;
	}

	public void setColorFull(TextColor colorFull) {
		this.colorFull = colorFull;
	}

	public TextColor getColorEmpty() {
		return colorEmpty;
	}

	public void setColorEmpty(TextColor colorEmpty) {
		this.colorEmpty = colorEmpty;
	}
	
	@Override
	public String toString() {
		return "ManaRank:["
			+ "name=" + name
			+ ",prefix=" + prefix
			+ ",symbol=" + symbol
			+ ",barSize" + barSize
			+ ",maxMana=" + maxMana
			+ ",startingMana=" + startingMana
			+ ",regenAmount=" + regenAmount
			+ ",regenInterval=" + regenInterval
			+ ",colorFull=" + colorFull
			+ ",colorEmpty=" + colorEmpty
			+ ']';
	}
	
}
