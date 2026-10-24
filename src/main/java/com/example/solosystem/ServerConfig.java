package com.example.solosystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gameplay settings (shared by everyone in the world). Stored in config/solosystem.json together with the
 * mod-reward lists. In singleplayer / as LAN host they can be changed live from the CONFIG tab.
 */
public final class ServerConfig {
	private static final Logger LOG = LoggerFactory.getLogger("solosystem");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Multiplier for all System EXP gained. */
	public static volatile double expMultiplier = 1.0;
	/** Multiplier for every passive-skill drop chance (0 = skills never drop). */
	public static volatile double skillChanceMultiplier = 1.0;
	/** Chance (0-1) that a Daily Quest also rewards a weapon/tool/armor from another mod. */
	public static volatile double modGearChance = 0.5;

	static final String DEFAULT_CONFIG = """
		{
		  "_help": "Rewards from other mods. Items of mods that are not installed are ignored. Tiers: 0=COMMON 1=UNCOMMON 2=RARE 3=EPIC 4=LEGENDARY",
		  "expMultiplier": 1.0,
		  "skillChanceMultiplier": 1.0,
		  "autoDiscoverModGear": true,
		  "autoGearChance": 0.5,
		  "blacklist": ["examplemod", "examplemod:broken_item"],
		  "extraRewards": {
		    "0": [ { "item": "examplemod:example_ingot", "min": 2, "max": 6 } ],
		    "1": [],
		    "2": [ { "item": "examplemod:example_sword", "min": 1, "max": 1 } ],
		    "3": [],
		    "4": []
		  }
		}
		""";

	private ServerConfig() {}

	public static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("solosystem.json"); }

	public static synchronized void load() {
		try {
			Path f = file();
			if (!Files.exists(f)) Files.writeString(f, DEFAULT_CONFIG);
			JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
			if (root.has("expMultiplier")) expMultiplier = clamp(root.get("expMultiplier").getAsDouble(), 0, 20);
			if (root.has("skillChanceMultiplier")) skillChanceMultiplier = clamp(root.get("skillChanceMultiplier").getAsDouble(), 0, 20);
			if (root.has("autoGearChance")) modGearChance = clamp(root.get("autoGearChance").getAsDouble(), 0, 1);
		} catch (Throwable t) {
			LOG.warn("[SoloSystem] could not read config/solosystem.json (using defaults): {}", t.toString());
		}
	}

	/** Writes the three live values back into the file, keeping every other entry (rewards, blacklist...) untouched. */
	public static synchronized void save() {
		try {
			Path f = file();
			JsonObject root = Files.exists(f) ? JsonParser.parseString(Files.readString(f)).getAsJsonObject()
				: JsonParser.parseString(DEFAULT_CONFIG).getAsJsonObject();
			root.addProperty("expMultiplier", expMultiplier);
			root.addProperty("skillChanceMultiplier", skillChanceMultiplier);
			root.addProperty("autoGearChance", modGearChance);
			Files.writeString(f, GSON.toJson(root));
		} catch (Throwable t) {
			LOG.warn("[SoloSystem] could not save config/solosystem.json: {}", t.toString());
		}
	}

	private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
