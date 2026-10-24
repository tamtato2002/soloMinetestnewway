package com.example.solosystem.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Per-player display settings (config/solosystem-client.json). Edited from the CONFIG tab, saved on every change. */
public final class ClientConfig {
	private static final Logger LOG = LoggerFactory.getLogger("solosystem");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final String[] SIDE = {"LEFT", "RIGHT"};
	public static final String[] STAY = {"SHORT", "NORMAL", "LONG"};
	public static final String[] ANIM = {"INSTANT", "FAST", "NORMAL", "SMOOTH"};
	public static final String[] TOP = {"HIGH", "DEFAULT", "LOW"};

	public static boolean toasts = true;          // show quest pop-ups at all
	public static boolean progressToasts = true;  // false = only "QUEST CLEAR" pop-ups
	public static boolean statBonuses = true;     // green bonus line under each stat
	public static int side = 0, stay = 1, anim = 2, top = 1;

	private ClientConfig() {}

	public static double stayMul() { return new double[]{0.6, 1.0, 1.8}[stay]; }
	public static long slideInMs() { return new long[]{1, 180, 320, 520}[anim]; }
	public static long slideOutMs() { return Math.max(1, slideInMs() * 4 / 5); }
	public static int topPx() { return new int[]{4, 36, 70}[top]; }

	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("solosystem-client.json"); }

	public static void load() {
		try {
			Path f = file();
			if (!Files.exists(f)) { save(); return; }
			JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
			if (o.has("toasts")) toasts = o.get("toasts").getAsBoolean();
			if (o.has("progressToasts")) progressToasts = o.get("progressToasts").getAsBoolean();
			if (o.has("statBonuses")) statBonuses = o.get("statBonuses").getAsBoolean();
			if (o.has("side")) side = idx(o.get("side").getAsInt(), SIDE.length);
			if (o.has("stay")) stay = idx(o.get("stay").getAsInt(), STAY.length);
			if (o.has("anim")) anim = idx(o.get("anim").getAsInt(), ANIM.length);
			if (o.has("top")) top = idx(o.get("top").getAsInt(), TOP.length);
		} catch (Throwable t) {
			LOG.warn("[SoloSystem] could not read solosystem-client.json (using defaults): {}", t.toString());
		}
	}

	public static void save() {
		try {
			JsonObject o = new JsonObject();
			o.addProperty("toasts", toasts);
			o.addProperty("progressToasts", progressToasts);
			o.addProperty("statBonuses", statBonuses);
			o.addProperty("side", side);
			o.addProperty("stay", stay);
			o.addProperty("anim", anim);
			o.addProperty("top", top);
			Files.writeString(file(), GSON.toJson(o));
		} catch (Throwable t) {
			LOG.warn("[SoloSystem] could not save solosystem-client.json: {}", t.toString());
		}
	}

	public static void reset() {
		toasts = true; progressToasts = true; statBonuses = true;
		side = 0; stay = 1; anim = 2; top = 1;
		save();
	}

	private static int idx(int v, int len) { return Math.max(0, Math.min(len - 1, v)); }
}
