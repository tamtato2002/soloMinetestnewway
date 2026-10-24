package com.example.solosystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daily-quest rewards from OTHER mods.
 *  1) config/solosystem.json  -> "extraRewards": hand-picked items per quest tier (any mod, any item id)
 *  2) auto discovery           -> every tool / weapon / armor (anything with durability) registered by another mod
 *                                 is sorted into a tier by durability + rarity and can drop as a bonus reward.
 * Items from mods that are not installed are silently ignored, so the same config works in every modpack.
 */
public final class ModRewards {
	private static final Logger LOG = LoggerFactory.getLogger("solosystem");

	public record Extra(Item item, String id, int min, int max) {}

	public static final List<List<Extra>> EXTRA = new ArrayList<>();
	public static final List<List<Item>> AUTO_GEAR = new ArrayList<>();

	private static boolean loaded = false;

	private ModRewards() {}

	public static synchronized void ensureLoaded() {
		if (loaded) return;
		loaded = true;
		for (int i = 0; i < 5; i++) { EXTRA.add(new ArrayList<>()); AUTO_GEAR.add(new ArrayList<>()); }

		boolean auto = true;
		Set<String> blacklist = new HashSet<>();
		try {
			ServerConfig.load(); // also creates the file the first time
			Path file = ServerConfig.file();
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

			if (root.has("autoDiscoverModGear")) auto = root.get("autoDiscoverModGear").getAsBoolean();
			if (root.has("blacklist")) for (JsonElement e : root.getAsJsonArray("blacklist")) blacklist.add(e.getAsString());

			if (root.has("extraRewards")) {
				JsonObject er = root.getAsJsonObject("extraRewards");
				for (int tier = 0; tier < 5; tier++) {
					if (!er.has(String.valueOf(tier))) continue;
					JsonArray arr = er.getAsJsonArray(String.valueOf(tier));
					for (JsonElement e : arr) {
						JsonObject o = e.getAsJsonObject();
						String id = o.get("item").getAsString();
						Item item = lookup(id);
						if (item == null || blacklist.contains(id)) continue;
						int min = Math.max(1, o.has("min") ? o.get("min").getAsInt() : 1);
						int max = Math.max(min, o.has("max") ? o.get("max").getAsInt() : min);
						EXTRA.get(tier).add(new Extra(item, id, min, max));
					}
				}
			}
		} catch (Throwable t) {
			LOG.warn("[SoloSystem] could not read config/solosystem.json (using defaults): {}", t.toString());
		}

		if (auto) discover(blacklist);
		int total = 0;
		for (List<Item> l : AUTO_GEAR) total += l.size();
		LOG.info("[SoloSystem] mod reward pool ready: {} auto-discovered gear items", total);
	}

	/** id like "modid:item" -> Item, or null when that mod/item does not exist. */
	public static Item lookup(String id) {
		try {
			int c = id.indexOf(':');
			if (c <= 0 || c == id.length() - 1) return null;
			Identifier rid = Identifier.fromNamespaceAndPath(id.substring(0, c), id.substring(c + 1));
			return BuiltInRegistries.ITEM.getOptional(rid).orElse(null);
		} catch (Throwable t) {
			return null;
		}
	}

	private static void discover(Set<String> blacklist) {
		for (Item item : BuiltInRegistries.ITEM) {
			try {
				Identifier key = BuiltInRegistries.ITEM.getKey(item);
				String ns = key.getNamespace();
				if (ns.equals("minecraft")) continue;
				if (blacklist.contains(ns) || blacklist.contains(key.toString())) continue;
				String path = key.getPath();
				if (path.contains("creative") || path.contains("debug") || path.contains("admin") || path.contains("command")) continue;

				ItemStack st = new ItemStack(item);
				int dur = st.getMaxDamage();
				if (dur <= 0 || st.getMaxStackSize() != 1) continue; // only tools / weapons / armor

				int tier = dur >= 1800 ? 4 : dur >= 900 ? 3 : dur >= 300 ? 2 : dur >= 150 ? 1 : 0;
				if (st.getRarity().ordinal() >= 2) tier = Math.min(4, tier + 1);
				AUTO_GEAR.get(tier).add(item);
			} catch (Throwable ignored) {
				// a weird modded item must never break the quest system
			}
		}
	}

	/** A random auto-discovered gear piece for this tier (falls back to nearby tiers), or null. */
	public static Item randomGear(int tier, java.util.Random rnd) {
		for (int d = 0; d < 5; d++) {
			for (int sign : new int[]{-1, 1}) {
				int t = tier + sign * d;
				if (t < 0 || t > 4) continue;
				List<Item> l = AUTO_GEAR.get(t);
				if (!l.isEmpty()) return l.get(rnd.nextInt(l.size()));
			}
		}
		return null;
	}
}
