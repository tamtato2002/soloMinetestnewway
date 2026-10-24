package com.example.solosystem;

import com.example.solosystem.net.SyncPayload;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

public final class SystemLogic {
	private static final Set<UUID> DIRTY = new HashSet<>();
	private static final Map<UUID, long[]> LAST = new HashMap<>();

	private SystemLogic() {}

	public static PlayerData data(ServerPlayer p) { return p.getAttachedOrCreate(SoloSystemMod.DATA); }
	public static void markDirty(ServerPlayer p) { DIRTY.add(p.getUUID()); }
	public static void sync(ServerPlayer p) { ServerPlayNetworking.send(p, new SyncPayload(data(p).copy())); }

	public static void onJoin(ServerPlayer p) {
		LAST.put(p.getUUID(), snapshot(p));
		applyAll(p, data(p));
		QuestLogic.ensure(p, data(p));
		sync(p);
		msg(p, "Player detected. Press Y to open the System window.", ChatFormatting.AQUA);
	}

	public static void onLeave(ServerPlayer p) {
		LAST.remove(p.getUUID());
		DIRTY.remove(p.getUUID());
		PerkLogic.onLeave(p.getUUID());
	}

	// ---------------------------------------------------------------- EXP / levels

	public static void addExp(ServerPlayer p, double base) {
		if (base <= 0) return;
		PlayerData d = data(p);
		double mult = 1.0 + Perks.expBonus(d) + 0.10 * d.rank(PassiveSkill.MONARCH_DOMAIN) + 0.05 * d.rank(PassiveSkill.SAGES_MIND);
		d.exp += base * mult * ServerConfig.expMultiplier;
		boolean leveled = false;
		while (d.exp >= d.expToNext()) {
			d.exp -= d.expToNext();
			d.level++;
			d.points += 3;
			leveled = true;
			msg(p, "LEVEL UP!  Lv." + d.level + "  (+3 stat points)", ChatFormatting.GOLD);
		}
		if (leveled) {
			applyAll(p, d);
			sync(p);
		} else {
			markDirty(p);
		}
	}

	public static void allocate(ServerPlayer p, String statId, int amount) {
		Stat s = Stat.byId(statId);
		PlayerData d = data(p);
		if (s == null || d.points <= 0) return;
		int n = Math.max(1, Math.min(amount, Math.min(100, d.points)));
		d.points -= n;
		d.stats.merge(s.id(), n, Integer::sum);
		applyAll(p, d);
		sync(p);
	}

	// ---------------------------------------------------------------- skills

	/** Roll for a passive skill. rarityBoost shifts odds toward rarer skills. */
	public static void rollSkill(ServerPlayer p, double chance, double rarityBoost) {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		if (rnd.nextDouble() >= chance * ServerConfig.skillChanceMultiplier) return;
		PlayerData d = data(p);

		SkillRarity rarity = rollRarity(rnd, rarityBoost);
		List<PassiveSkill> pool = new ArrayList<>();
		for (PassiveSkill s : PassiveSkill.values())
			if (s.rarity == rarity && d.rank(s) < s.maxRank) pool.add(s);
		if (pool.isEmpty()) {
			for (PassiveSkill s : PassiveSkill.values())
				if (d.rank(s) < s.maxRank) pool.add(s);
		}
		if (pool.isEmpty()) return;

		PassiveSkill s = pool.get(rnd.nextInt(pool.size()));
		int rank = d.rank(s) + 1;
		d.skills.put(s.id(), rank);
		p.sendSystemMessage(Component.literal("[SYSTEM] ").withStyle(ChatFormatting.AQUA)
			.append(Component.literal("Passive skill acquired: " + s.displayName + " (Rank " + rank + "/" + s.maxRank + ") ["
				+ s.rarity.name() + "]").withStyle(s.rarity.color)));
		applyAll(p, d);
		sync(p);
	}

	private static SkillRarity rollRarity(ThreadLocalRandom rnd, double boost) {
		SkillRarity[] all = SkillRarity.values();
		double[] w = new double[all.length];
		double total = 0;
		for (int i = 0; i < all.length; i++) {
			w[i] = all[i].weight * (1.0 + boost * i);
			total += w[i];
		}
		double r = rnd.nextDouble() * total;
		for (int i = 0; i < all.length; i++) {
			r -= w[i];
			if (r <= 0) return all[i];
		}
		return SkillRarity.COMMON;
	}

	// ---------------------------------------------------------------- tick

	public static void tick(MinecraftServer server) {
		int t = server.getTickCount();
		long clock = QuestLogic.clockDelta(server); // 1 normally, big when the night is skipped by sleeping
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!p.isAlive()) continue;
			PlayerData d = data(p);
			d.questLeft -= clock;
			if (d.questLeft <= 0) QuestLogic.ensure(p, d);
			else if (clock > 20) sync(p); // time jumped: tell the client right away
			if (t % 4 == 0) magnet(p, d);
			PerkLogic.tick(p, d, t);
			if (t % 5 == 0) pollStats(p); // fast poll -> walking/fishing/breeding quest pop-ups appear quickly
			if (t % 20 == 0) {
				applyAll(p, d);
				QuestLogic.ensure(p, d);
			}
			if (t % 200 == 0) stomach(p, d);
			if (t % 10 == 0 && DIRTY.remove(p.getUUID())) sync(p);
		}
	}

	static long stat(ServerPlayer p, Identifier id) { return p.getStats().getValue(Stats.CUSTOM.get(id)); }

	private static long totalCm(ServerPlayer p) {
		return stat(p, Stats.WALK_ONE_CM) + stat(p, Stats.SPRINT_ONE_CM) + stat(p, Stats.SWIM_ONE_CM);
	}

	private static long[] snapshot(ServerPlayer p) {
		return new long[]{totalCm(p), stat(p, Stats.FISH_CAUGHT), stat(p, Stats.ANIMALS_BRED),
			stat(p, Stats.TRADED_WITH_VILLAGER), stat(p, Stats.ENCHANT_ITEM), stat(p, Stats.JUMP)};
	}

	/** Actions that have no event hook: read vanilla statistics and award EXP for the change. */
	private static void pollStats(ServerPlayer p) {
		long[] cur = snapshot(p);
		long[] last = LAST.put(p.getUUID(), cur);
		if (last == null) return;

		long dCm = cur[0] - last[0];
		if (dCm > 0) {
			double blocks = dCm / 100.0;
			addExp(p, blocks * 0.02); // 0.02 EXP per block
			QuestLogic.progress(p, QuestType.WALK, blocks);
			rollSkill(p, Math.min(1.0, blocks * 0.00025), 0); // ~1 skill per 4000 blocks travelled
		}
		long fish = cur[1] - last[1];
		if (fish > 0) {
			addExp(p, fish * 12.0);
			QuestLogic.progress(p, QuestType.FISH, fish);
			rollSkill(p, Math.min(1.0, 0.08 * fish), 0.5);
		}
		long bred = cur[2] - last[2];
		if (bred > 0) {
			addExp(p, bred * 8.0);
			QuestLogic.progress(p, QuestType.BREED, bred);
			rollSkill(p, Math.min(1.0, 0.04 * bred), 0.3);
		}
		long trade = cur[3] - last[3];
		if (trade > 0) { addExp(p, trade * 10.0); rollSkill(p, Math.min(1.0, 0.025 * trade), 0.3); }
		long ench = cur[4] - last[4];
		if (ench > 0) { addExp(p, ench * 20.0); rollSkill(p, Math.min(1.0, 0.06 * ench), 1.0); }
		long jumps = cur[5] - last[5];
		if (jumps > 0) rollSkill(p, Math.min(1.0, 0.0008 * jumps), 0);
	}

	private static void stomach(ServerPlayer p, PlayerData d) {
		int rank = d.rank(PassiveSkill.IRON_STOMACH);
		if (rank <= 0) return;
		int food = p.getFoodData().getFoodLevel();
		if (food < 20) p.getFoodData().setFoodLevel(Math.min(20, food + rank));
	}

	private static void magnet(ServerPlayer p, PlayerData d) {
		int rank = d.rank(PassiveSkill.MAGNET);
		if (rank <= 0) return;
		double radius = rank == 1 ? 3 : rank == 2 ? 5 : 8;
		List<ItemEntity> items = p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(radius));
		for (ItemEntity it : items) {
			if (it.hasPickUpDelay()) continue;
			Vec3 dir = p.position().add(0, 0.5, 0).subtract(it.position());
			if (dir.lengthSqr() < 0.25) continue;
			it.setDeltaMovement(dir.normalize().scale(0.35));
		}
	}

	// ---------------------------------------------------------------- stats + passives -> attributes/effects

	public static void applyAll(ServerPlayer p, PlayerData d) {
				mod(p, Attributes.ATTACK_DAMAGE, "attack", Perks.attack(d) + 0.5 * d.rank(PassiveSkill.HUNTERS_INSTINCT) + 1.0 * d.rank(PassiveSkill.BERSERKER), Operation.ADD_VALUE);
		mod(p, Attributes.MAX_HEALTH, "health", Perks.health(d) + 2.0 * d.rank(PassiveSkill.HEARTY) + 4.0 * d.rank(PassiveSkill.TITAN_FRAME), Operation.ADD_VALUE);
		mod(p, Attributes.MOVEMENT_SPEED, "speed", Perks.moveSpeed(d) + 0.05 * d.rank(PassiveSkill.SWIFT_STEP) + 0.08 * d.rank(PassiveSkill.WINDWALKER), Operation.ADD_MULTIPLIED_BASE);
		mod(p, Attributes.ATTACK_SPEED, "atkspeed", Perks.atkSpeed(d), Operation.ADD_MULTIPLIED_BASE);
		mod(p, Attributes.LUCK, "luck", Perks.luck(d) + 1.0 * d.rank(PassiveSkill.LUCKY) + 0.5 * d.rank(PassiveSkill.ANGLERS_LUCK), Operation.ADD_VALUE);
		int sov = d.rank(PassiveSkill.SOVEREIGN_REACH);
		mod(p, Attributes.BLOCK_INTERACTION_RANGE, "block_reach", Perks.blockReach(d) + 2.0 * sov, Operation.ADD_VALUE);
		mod(p, Attributes.ENTITY_INTERACTION_RANGE, "entity_reach", Perks.entityReach(d) + 1.5 * sov, Operation.ADD_VALUE);

		mod(p, Attributes.ARMOR, "armor", 2.0 * d.rank(PassiveSkill.IRON_SKIN) + 5.0 * d.rank(PassiveSkill.MONARCH_AEGIS), Operation.ADD_VALUE);
		mod(p, Attributes.ARMOR_TOUGHNESS, "toughness", 1.0 * d.rank(PassiveSkill.THICK_SKIN) + 1.0 * d.rank(PassiveSkill.TITAN_FRAME) + 2.0 * d.rank(PassiveSkill.MONARCH_AEGIS), Operation.ADD_VALUE);
		mod(p, Attributes.STEP_HEIGHT, "step", 0.4 * d.rank(PassiveSkill.LIGHT_FEET), Operation.ADD_VALUE);
		mod(p, Attributes.JUMP_STRENGTH, "jump", 0.08 * d.rank(PassiveSkill.SPRINGS), Operation.ADD_VALUE);
		mod(p, Attributes.FALL_DAMAGE_MULTIPLIER, "fall", -Math.min(0.9, 0.25 * d.rank(PassiveSkill.FEATHER_FALL) + Perks.fallReduce(d)), Operation.ADD_VALUE);
		// STR: faster mining AND chopping on every block / tool / mod (multiplier on block break speed)
		mod(p, Attributes.BLOCK_BREAK_SPEED, "dig", Perks.digSpeed(d), Operation.ADD_MULTIPLIED_BASE);
		mod(p, Attributes.BLOCK_BREAK_SPEED, "skill_dig", 0.2 * d.rank(PassiveSkill.TIRELESS_ARMS), Operation.ADD_MULTIPLIED_BASE);
		mod(p, Attributes.KNOCKBACK_RESISTANCE, "knockback", 0.25 * d.rank(PassiveSkill.IMMOVABLE), Operation.ADD_VALUE);
		mod(p, Attributes.SAFE_FALL_DISTANCE, "safe_fall", 6.0 * d.rank(PassiveSkill.SKY_LORD), Operation.ADD_VALUE);
		mod(p, Attributes.JUMP_STRENGTH, "sky_jump", 0.1 * d.rank(PassiveSkill.SKY_LORD), Operation.ADD_VALUE);
		mod(p, Attributes.OXYGEN_BONUS, "oxygen", 3.0 * d.rank(PassiveSkill.GILLS), Operation.ADD_VALUE);
		mod(p, Attributes.WATER_MOVEMENT_EFFICIENCY, "swim", 0.15 * d.rank(PassiveSkill.GILLS), Operation.ADD_VALUE);
		mod(p, Attributes.MINING_EFFICIENCY, "mining", 1.0 * d.rank(PassiveSkill.MINERS_HANDS) + 0.5 * d.rank(PassiveSkill.QUICK_HANDS), Operation.ADD_VALUE);

		if (d.rank(PassiveSkill.CAT_EYES) > 0)
			p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
		if (d.rank(PassiveSkill.TIDAL_BLESSING) > 0) {
			p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, true, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0, true, false, false));
		}
		if (d.rank(PassiveSkill.HEAT_WARD) > 0)
			p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, false));
		int regen = d.rank(PassiveSkill.REGEN);
		if (regen > 0)
			p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, regen - 1, true, false, false));
	}

	private static void mod(ServerPlayer p, Holder<Attribute> attr, String key, double amount, Operation op) {
		AttributeInstance inst = p.getAttribute(attr);
		if (inst == null) return;
		Identifier id = SoloSystemMod.id(key);
		if (amount == 0) {
			if (inst.getModifier(id) != null) inst.removeModifier(id);
			return;
		}
		AttributeModifier cur = inst.getModifier(id);
		if (cur != null && cur.amount() == amount) return;
		inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
	}

	public static void msg(ServerPlayer p, String text, ChatFormatting color) {
		p.sendSystemMessage(Component.literal("[SYSTEM] ").withStyle(ChatFormatting.AQUA)
			.append(Component.literal(text).withStyle(color)));
	}
}
