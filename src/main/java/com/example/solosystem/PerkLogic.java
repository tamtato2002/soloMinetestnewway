package com.example.solosystem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quality-of-life effects of the five stats that are not plain attributes.
 * Everything here works on vanilla objects only (ItemStack, containers, hooks), so it works with items,
 * tools, weapons, armor and blocks from ANY mod.
 */
public final class PerkLogic {
	private static final Logger LOG = LoggerFactory.getLogger("solosystem");

	private PerkLogic() {}

	public static void onLeave(UUID id) {
		SNAPS.remove(id);
		ENCH_LAST.remove(id);
	}

	/** Called every server tick for every living player. */
	public static void tick(ServerPlayer p, PlayerData d, int t) {
		durability(p, d);
		enchanting(p, d);
		fishing(p, d);
		if (t % 40 == 0) regen(p, d);
	}

	// ------------------------------------------------------------------ VIT: regeneration

	private static void regen(ServerPlayer p, PlayerData d) {
		double amount = Perks.regen(d);
		if (amount <= 0) return;
		if (p.getHealth() < p.getMaxHealth() && p.getFoodData().getFoodLevel() > 6) p.heal((float) amount);
	}

	// ------------------------------------------------------------------ STR / VIT: durability protection

	private record Snap(ItemStack stack, int damage) {}

	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Map<UUID, Snap[]> SNAPS = new HashMap<>();

	/**
	 * Watches the damage value of held items (STR) and worn armor (VIT). Every durability point lost
	 * since last tick gets a chance to be refunded. Same item instance only, so swapping items never "heals" anything.
	 */
	private static void durability(ServerPlayer p, PlayerData d) {
		double tool = Perks.toolSave(d), armor = Perks.armorSave(d);
		if (tool <= 0 && armor <= 0 || p.isCreative()) { SNAPS.remove(p.getUUID()); return; }

		Snap[] snaps = SNAPS.computeIfAbsent(p.getUUID(), k -> new Snap[SLOTS.length]);
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		for (int i = 0; i < SLOTS.length; i++) {
			ItemStack s = p.getItemBySlot(SLOTS[i]);
			if (s.isEmpty() || s.getMaxDamage() <= 0) { snaps[i] = null; continue; }

			Snap prev = snaps[i];
			if (prev != null && prev.stack() == s) {
				int lost = s.getDamageValue() - prev.damage();
				double chance = i < 2 ? tool : armor;
				if (lost > 0 && lost <= 100 && chance > 0) {
					int refund = 0;
					for (int k = 0; k < lost; k++) if (rnd.nextDouble() < chance) refund++;
					if (refund > 0) s.setDamageValue(Math.max(0, s.getDamageValue() - refund));
				}
			}
			snaps[i] = new Snap(s, s.getDamageValue());
		}
	}

	// ------------------------------------------------------------------ INT: better enchanting

	private static final Map<UUID, Long> ENCH_LAST = new HashMap<>();

	/** Detects a finished enchant (vanilla statistic) and upgrades the result in the table slot. */
	private static void enchanting(ServerPlayer p, PlayerData d) {
		long cur = SystemLogic.stat(p, Stats.ENCHANT_ITEM);
		Long last = ENCH_LAST.put(p.getUUID(), cur);
		if (last == null || cur <= last) return;
		if (!(p.containerMenu instanceof EnchantmentMenu menu)) return;

		double up = Perks.enchantUp(d), extra = Perks.enchantExtra(d);
		if (up <= 0 && extra <= 0) return;

		ItemStack st = menu.getSlot(0).getItem();
		if (st.isEmpty()) return;
		boolean book = st.is(Items.ENCHANTED_BOOK);
		ItemEnchantments ie = book ? st.get(DataComponents.STORED_ENCHANTMENTS) : st.get(DataComponents.ENCHANTMENTS);
		if (ie == null || ie.isEmpty()) return;

		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		List<Holder<Enchantment>> have = new ArrayList<>(ie.keySet());
		int upgraded = 0;
		for (Holder<Enchantment> h : have) {
			int lvl = ie.getLevel(h);
			if (lvl < h.value().getMaxLevel() && rnd.nextDouble() < up) {
				st.enchant(h, lvl + 1);
				upgraded++;
			}
		}

		String added = null;
		if (extra > 0 && rnd.nextDouble() < extra) {
			var registry = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			List<Holder<Enchantment>> candidates = new ArrayList<>();
			registry.listElements().forEach(ref -> {
				Holder<Enchantment> h = ref;
				if (have.contains(h) || h.getRegisteredName().contains("curse")) return;
				for (Holder<Enchantment> o : have) if (!Enchantment.areCompatible(h, o)) return;
				if (!book && !h.value().canEnchant(st)) return;
				candidates.add(h);
			});
			if (!candidates.isEmpty()) {
				Holder<Enchantment> pick = candidates.get(rnd.nextInt(candidates.size()));
				st.enchant(pick, 1 + rnd.nextInt(Math.max(1, pick.value().getMaxLevel())));
				added = pick.getRegisteredName();
			}
		}

		if (upgraded > 0 || added != null) {
			menu.broadcastChanges();
			String txt = "INT bonus!";
			if (upgraded > 0) txt += " " + upgraded + " enchant level" + (upgraded > 1 ? "s" : "") + " upgraded.";
			if (added != null) txt += " Extra enchantment: " + added.substring(added.indexOf(':') + 1).replace('_', ' ') + ".";
			SystemLogic.msg(p, txt, ChatFormatting.LIGHT_PURPLE);
		}
	}

	// ------------------------------------------------------------------ AGI: fast fishing

	private static Field FISHING_FIELD;
	private static final Map<Class<?>, Field> LURE_FIELDS = new HashMap<>();
	private static boolean fishBroken = false;

	/**
	 * Shortens the bobber's "waiting for a fish" timer. Done through reflection on purpose:
	 * if a Minecraft update renames these fields the mod still loads, only the fishing perk switches itself off.
	 */
	private static void fishing(ServerPlayer p, PlayerData d) {
		if (fishBroken) return;
		double rate = Perks.fishRate(d);
		if (rate <= 0) return;
		try {
			if (FISHING_FIELD == null) FISHING_FIELD = Player.class.getField("fishing");
			Object hook = FISHING_FIELD.get(p);
			if (hook == null) return;

			Field lure = LURE_FIELDS.get(hook.getClass());
			if (lure == null) {
				lure = findField(hook.getClass(), "timeUntilLured");
				if (lure == null) throw new NoSuchFieldException("timeUntilLured");
				LURE_FIELDS.put(hook.getClass(), lure);
			}
			int wait = lure.getInt(hook);
			if (wait > 2) {
				ThreadLocalRandom rnd = ThreadLocalRandom.current();
				int extra = (int) rate + (rnd.nextDouble() < rate - (int) rate ? 1 : 0);
				if (extra > 0) lure.setInt(hook, Math.max(2, wait - extra));
			}
		} catch (Throwable e) {
			fishBroken = true;
			LOG.warn("[SoloSystem] fast-fishing perk disabled (Minecraft internals changed): {}", e.toString());
		}
	}

	private static Field findField(Class<?> c, String name) {
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		return null;
	}
}
