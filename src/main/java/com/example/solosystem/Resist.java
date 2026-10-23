package com.example.solosystem;

/** Resistances that are LEARNED by being exposed to something. More exposure = higher % (caps at 75%). */
public enum Resist {
	MELEE("melee", "Melee", 40),
	RANGED("ranged", "Ranged", 40),
	FIRE("fire", "Fire & Lava", 30),
	EXPLOSION("explosion", "Explosion", 15),
	FALL("fall", "Fall", 25),
	DROWNING("drowning", "Drowning", 15),
	COLD("cold", "Freezing", 20),
	LIGHTNING("lightning", "Lightning", 3),
	POISON("poison", "Poison", 8),
	WITHER("wither", "Wither", 6),
	SLOWNESS("slowness", "Slowness", 8),
	WEAKNESS("weakness", "Weakness", 8),
	MINING_FATIGUE("mining_fatigue", "Mining Fatigue", 6),
	HUNGER("hunger", "Hunger", 8),
	BLINDNESS("blindness", "Blindness", 6),
	NAUSEA("nausea", "Nausea", 6),
	DARKNESS("darkness", "Darkness", 5),
	LEVITATION("levitation", "Levitation", 5);

	public static final double MAX = 0.75;

	public final String id, label;
	/** Exposures needed to reach half of the max resistance (37.5%). */
	public final int half;

	Resist(String id, String label, int half) { this.id = id; this.label = label; this.half = half; }

	/** 0.0 .. 0.75 for a given number of exposures. */
	public double percent(int count) {
		if (count <= 0) return 0;
		return MAX * (1.0 - Math.pow(0.5, count / (double) half));
	}
}
