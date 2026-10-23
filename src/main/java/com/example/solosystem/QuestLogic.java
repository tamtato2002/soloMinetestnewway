package com.example.solosystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Daily quests: 3 random quests (COMMON..LEGENDARY). They reset every 24000 game ticks
 * (= 1 Minecraft day, ~20 real minutes; sleeping does not skip it). Rewards are rolled randomly per quest.
 */
public final class QuestLogic {
	public static final long DAY_TICKS = 24000L;
	public static final int QUESTS_PER_DAY = 3;

	private static final double[] MULT = {1, 2, 3.5, 6, 10};          // target scaling per tier
	private static final int[] TIER_WEIGHT = {40, 28, 18, 10, 4};    // how often each tier is rolled

	private static final int[] EXP_BASE = {80, 200, 500, 1200, 3000};
	private static final int[] PTS_MIN = {0, 0, 1, 2, 4};
	private static final int[] PTS_MAX = {1, 2, 3, 5, 8};
	private static final double[] SK_MIN = {0.05, 0.20, 0.50, 0.80, 1.0};
	private static final double[] SK_MAX = {0.20, 0.50, 0.85, 1.00, 1.0};
	private static final double[] SK_BOOST = {0, 0.5, 1.5, 3, 6};

	private record Reward(Item item, String id, int min, int max) {}
	private static Reward[][] pools;

	private QuestLogic() {}

	private static Reward r(Item i, int a, int b) { return new Reward(i, BuiltInRegistries.ITEM.getKey(i).toString(), a, b); }

	private static void initPools() {
		if (pools != null) return;
		ModRewards.ensureLoaded();
		Reward[][] base = new Reward[][]{
			{ r(Items.IRON_INGOT, 4, 10), r(Items.COOKED_BEEF, 6, 12), r(Items.COAL, 8, 16), r(Items.BREAD, 8, 16),
			  r(Items.COPPER_INGOT, 6, 12), r(Items.ARROW, 16, 32), r(Items.TORCH, 16, 32), r(Items.BONE_MEAL, 8, 16) },
			{ r(Items.GOLD_INGOT, 6, 14), r(Items.DIAMOND, 1, 3), r(Items.LAPIS_LAZULI, 8, 16), r(Items.REDSTONE, 10, 24),
			  r(Items.ENDER_PEARL, 1, 3), r(Items.GOLDEN_CARROT, 8, 16), r(Items.EXPERIENCE_BOTTLE, 4, 10), r(Items.QUARTZ, 8, 16) },
			{ r(Items.DIAMOND, 3, 8), r(Items.EMERALD, 4, 10), r(Items.EXPERIENCE_BOTTLE, 10, 24), r(Items.GOLDEN_APPLE, 1, 3),
			  r(Items.ENDER_PEARL, 4, 8), r(Items.BLAZE_ROD, 3, 8), r(Items.NAME_TAG, 1, 2), r(Items.SADDLE, 1, 1) },
			{ r(Items.DIAMOND, 8, 20), r(Items.NETHERITE_SCRAP, 2, 5), r(Items.ENCHANTED_GOLDEN_APPLE, 1, 2), r(Items.TOTEM_OF_UNDYING, 1, 1),
			  r(Items.EMERALD, 12, 24), r(Items.EXPERIENCE_BOTTLE, 24, 48), r(Items.SHULKER_SHELL, 2, 4), r(Items.HEART_OF_THE_SEA, 1, 1) },
			{ r(Items.NETHERITE_INGOT, 2, 5), r(Items.ENCHANTED_GOLDEN_APPLE, 2, 4), r(Items.TOTEM_OF_UNDYING, 1, 2), r(Items.NETHER_STAR, 1, 1),
			  r(Items.EXPERIENCE_BOTTLE, 48, 64), r(Items.DIAMOND_BLOCK, 2, 5), r(Items.BEACON, 1, 1) }
		};
		// merge hand-picked rewards from other mods (config/solosystem.json)
		for (int t = 0; t < 5; t++) {
			List<Reward> l = new ArrayList<>(List.of(base[t]));
			for (ModRewards.Extra e : ModRewards.EXTRA.get(t)) l.add(new Reward(e.item(), e.id(), e.min(), e.max()));
			base[t] = l.toArray(new Reward[0]);
		}
		pools = base;
	}

	public static SkillRarity tierRarity(int tier) { return SkillRarity.values()[tier]; }

	// ---------------------------------------------------------------- reward description (server + client)

	/** Reward strings are "namespace:path*count". Items from mods that are missing are skipped. */
	public static List<ItemStack> itemStacks(Quest q) {
		List<ItemStack> out = new ArrayList<>();
		for (String code : q.items) {
			try {
				int star = code.lastIndexOf('*');
				if (star <= 0) continue;
				int count = Integer.parseInt(code.substring(star + 1));
				Item item = ModRewards.lookup(code.substring(0, star));
				if (item != null && count > 0) out.add(new ItemStack(item, count));
			} catch (Throwable ignored) {}
		}
		return out;
	}

	public static String rewardLine1(Quest q) {
		String pts = q.rewardPoints > 0 ? ", +" + q.rewardPoints + " stat pts" : "";
		return q.rewardExp + " EXP" + pts + ", skill " + Math.round(q.skillChance * 100) + "%";
	}

	public static String rewardLine2(Quest q) {
		StringBuilder sb = new StringBuilder("Items: ");
		boolean first = true;
		for (ItemStack s : itemStacks(q)) {
			if (!first) sb.append(", ");
			sb.append(s.getCount()).append("x ").append(s.getHoverName().getString());
			first = false;
		}
		return sb.toString();
	}

	// ---------------------------------------------------------------- generation

	public static long gameTime(ServerPlayer p) { return p.level().getServer().overworld().getGameTime(); }

	public static void ensure(ServerPlayer p, PlayerData d) {
		long now = gameTime(p);
		if (d.quests.isEmpty() || now >= d.questExpiry || d.questExpiry - now > DAY_TICKS) generate(p, d, now);
	}

	private static void generate(ServerPlayer p, PlayerData d, long now) {
		initPools();
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		boolean reset = !d.quests.isEmpty();
		d.quests.clear();
		d.questExpiry = now + DAY_TICKS;

		List<QuestType> types = new ArrayList<>(List.of(QuestType.values()));
		Collections.shuffle(types);
		for (int i = 0; i < QUESTS_PER_DAY; i++) {
			QuestType t = types.get(i);
			int tier = rollTier(rnd);
			int target = Math.max(1, (int) Math.round(t.base * MULT[tier]));

			// random reward, rolled now and fixed for this quest
			int exp = (int) Math.round(EXP_BASE[tier] * (0.7 + rnd.nextDouble() * 0.8));
			int pts = PTS_MIN[tier] + rnd.nextInt(PTS_MAX[tier] - PTS_MIN[tier] + 1);
			double sk = SK_MIN[tier] + rnd.nextDouble() * (SK_MAX[tier] - SK_MIN[tier]);
			sk = Math.round(sk * 100) / 100.0;

			Reward[] pool = pools[tier];
			List<Integer> idxs = new ArrayList<>();
			for (int k = 0; k < pool.length; k++) idxs.add(k);
			Collections.shuffle(idxs);
			int n = tier <= 1 ? 2 : 3;
			List<String> codes = new ArrayList<>();
			for (int k = 0; k < n && k < idxs.size(); k++) {
				Reward rw = pool[idxs.get(k)];
				int count = rw.min() + rnd.nextInt(rw.max() - rw.min() + 1);
				codes.add(rw.id() + "*" + count);
			}
			// bonus: a weapon / tool / armor piece from ANOTHER mod (if any are installed)
			if (rnd.nextDouble() < ModRewards.autoGearChance) {
				Item gear = ModRewards.randomGear(tier, rnd);
				if (gear != null) codes.add(BuiltInRegistries.ITEM.getKey(gear).toString() + "*1");
			}
			d.quests.add(new Quest(t.id, tier, target, 0, false, exp, pts, sk, codes));
		}
		SystemLogic.msg(p, reset ? "A new day begins. Daily quests have reset!" : "[Daily Quest] has arrived!", ChatFormatting.YELLOW);
		SystemLogic.sync(p);
	}

	private static int rollTier(ThreadLocalRandom rnd) {
		int total = 0;
		for (int w : TIER_WEIGHT) total += w;
		int r = rnd.nextInt(total);
		for (int i = 0; i < TIER_WEIGHT.length; i++) {
			r -= TIER_WEIGHT[i];
			if (r < 0) return i;
		}
		return 0;
	}

	// ---------------------------------------------------------------- progress + completion

	public static void progress(ServerPlayer p, QuestType type, double amount) {
		if (amount <= 0) return;
		PlayerData d = SystemLogic.data(p);
		boolean moved = false;
		for (Quest q : d.quests) {
			if (q.done || !q.type.equals(type.id)) continue;
			int before = (int) q.progress;
			q.progress = Math.min(q.target, q.progress + amount);
			if ((int) q.progress != before) moved = true;
			if (q.progress >= q.target) complete(p, d, q);
		}
		// send immediately (not on the 0.5s dirty timer) so the pop-up appears the moment you act
		if (moved) SystemLogic.sync(p);
	}

	private static void complete(ServerPlayer p, PlayerData d, Quest q) {
		q.done = true;
		q.progress = q.target;
		int t = Math.max(0, Math.min(4, q.tier));
		QuestType type = QuestType.byId(q.type);
		String label = type == null ? q.type : type.label;
		SystemLogic.msg(p, "QUEST CLEAR [" + tierRarity(t).name() + "]: " + label + " x" + q.target, tierRarity(t).color);
		SystemLogic.msg(p, "Reward: " + rewardLine1(q), ChatFormatting.GREEN);

		d.points += q.rewardPoints;
		SystemLogic.addExp(p, q.rewardExp);
		SystemLogic.rollSkill(p, q.skillChance, SK_BOOST[t]);
		for (ItemStack s : itemStacks(q)) give(p, s);
		SystemLogic.msg(p, rewardLine2(q), ChatFormatting.GREEN);
		SystemLogic.sync(p);
	}

	/** Put the item straight into the inventory; whatever does not fit drops at the player's feet. */
	public static void give(ServerPlayer p, ItemStack stack) {
		p.getInventory().add(stack);
		if (!stack.isEmpty())
			p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY() + 0.3, p.getZ(), stack));
	}
}
