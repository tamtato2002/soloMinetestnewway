package com.example.solosystem;

/** Central place for every stat -> gameplay formula (so the UI and the logic always agree). */
public final class Perks {
	private Perks() {}

	// ---- STR
	public static double attack(PlayerData d)    { return 0.25 * d.stat(Stat.STR); }
	/** multiplier added to block break speed: all blocks, all tools, all mods (mining + chopping) */
	public static double digSpeed(PlayerData d)  { return 0.015 * d.stat(Stat.STR); }
	/** chance to NOT lose durability on held weapons/tools */
	public static double toolSave(PlayerData d)  { return Math.min(0.5, 0.004 * d.stat(Stat.STR)); }

	// ---- VIT
	public static double health(PlayerData d)    { return 1.0 * d.stat(Stat.VIT); }
	public static double armorSave(PlayerData d) { return Math.min(0.5, 0.004 * d.stat(Stat.VIT)); }
	/** HP healed every 2 seconds (when not starving) */
	public static double regen(PlayerData d)     { return 0.02 * d.stat(Stat.VIT); }

	// ---- AGI
	public static double moveSpeed(PlayerData d) { return 0.005 * d.stat(Stat.AGI); }
	public static double atkSpeed(PlayerData d)  { return 0.01 * d.stat(Stat.AGI); }
	/** extra "wait ticks" removed from the fishing timer every tick (0.02 = +2% faster per point) */
	public static double fishRate(PlayerData d)  { return Math.min(5.0, 0.02 * d.stat(Stat.AGI)); }
	public static double fallReduce(PlayerData d){ return Math.min(0.5, 0.004 * d.stat(Stat.AGI)); }

	// ---- INT
	public static double expBonus(PlayerData d)  { return 0.01 * d.stat(Stat.INT); }
	/** per enchantment: chance to get +1 level when enchanting (up to that enchantment's max) */
	public static int effInt(PlayerData d) { return d.stat(Stat.INT) + 10 * d.rank(PassiveSkill.ARCHMAGE_MIND); }
	public static double enchantUp(PlayerData d) { return Math.min(0.75, 0.01 * effInt(d)); }
	/** chance to receive one extra compatible enchantment */
	public static double enchantExtra(PlayerData d) { return Math.min(0.5, 0.006 * effInt(d)); }

	// ---- PER
	public static double luck(PlayerData d)      { return 0.1 * d.stat(Stat.PER); }
	public static double blockReach(PlayerData d){ return 0.05 * d.stat(Stat.PER); }
	public static double entityReach(PlayerData d){ return 0.03 * d.stat(Stat.PER); }
	/** chance to double the drops of ores and logs */
	public static double doubleDrop(PlayerData d){ return Math.min(0.75, 0.005 * d.stat(Stat.PER) + 0.15 * d.rank(PassiveSkill.MIDAS_TOUCH)); }
}
