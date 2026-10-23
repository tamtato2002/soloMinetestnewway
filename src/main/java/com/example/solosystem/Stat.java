package com.example.solosystem;

public enum Stat {
	STR("Damage, mining/chopping speed, weapon & tool durability"),
	VIT("Max HP, armor durability, regeneration"),
	AGI("Move/attack speed, fast fishing, less fall damage"),
	INT("EXP gain, better enchanting"),
	PER("Luck, reach, double ore/log drops, better loot");

	public final String desc;
	Stat(String desc) { this.desc = desc; }
	public String id() { return name().toLowerCase(); }

	public static Stat byId(String s) {
		for (Stat st : values()) if (st.id().equals(s)) return st;
		return null;
	}

	/** One-line list of what the CURRENT points give (shown in the System window). */
	public String bonuses(PlayerData d) {
		return switch (this) {
			case STR -> String.format("+%.1f dmg | Dig/Chop +%.0f%% | Tool/Weapon save %.0f%%",
				Perks.attack(d), Perks.digSpeed(d) * 100, Perks.toolSave(d) * 100);
			case VIT -> String.format("+%.0f HP | Armor save %.0f%% | Regen +%.1f HP/2s",
				Perks.health(d), Perks.armorSave(d) * 100, Perks.regen(d));
			case AGI -> String.format("Move +%.1f%% | Atk +%.0f%% | Fish +%.0f%% | Fall -%.0f%%",
				Perks.moveSpeed(d) * 100, Perks.atkSpeed(d) * 100, Perks.fishRate(d) * 100, Perks.fallReduce(d) * 100);
			case INT -> String.format("EXP +%.0f%% | Ench lvl-up %.0f%% | Extra ench %.0f%%",
				Perks.expBonus(d) * 100, Perks.enchantUp(d) * 100, Perks.enchantExtra(d) * 100);
			case PER -> String.format("Luck +%.1f | Reach +%.2f | Ore/Log x2 %.0f%%",
				Perks.luck(d), Perks.blockReach(d), Perks.doubleDrop(d) * 100);
		};
	}
}
