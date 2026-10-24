package com.example.solosystem;

import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** All the "do anything -> get EXP" hooks. Tune the numbers here. */
public final class ExpEvents {
	private ExpEvents() {}

	private static TagKey<Block> tag(String name) {
		return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", name));
	}
	private static final TagKey<Block> COAL = tag("coal_ores");
	private static final TagKey<Block> COPPER = tag("copper_ores");
	private static final TagKey<Block> IRON = tag("iron_ores");
	private static final TagKey<Block> LAPIS = tag("lapis_ores");
	private static final TagKey<Block> REDSTONE = tag("redstone_ores");
	private static final TagKey<Block> GOLD = tag("gold_ores");
	private static final TagKey<Block> DIAMOND = tag("diamond_ores");
	private static final TagKey<Block> EMERALD = tag("emerald_ores");
	private static final TagKey<Block> CROPS = tag("crops");
	/** Fabric/NeoForge convention tag: every ore of every mod that follows the convention (c:ores) */
	private static final TagKey<Block> ALL_ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
			if (!(player instanceof ServerPlayer sp) || sp.isCreative()) return;
			SystemLogic.addExp(sp, blockExp(state, state.getDestroySpeed(level, pos)));
			QuestLogic.progress(sp, QuestType.MINE, 1);
			if (state.is(BlockTags.LOGS)) QuestLogic.progress(sp, QuestType.CHOP, 1);
			if (isOre(state)) QuestLogic.progress(sp, QuestType.ORE, 1);
			doubleDrops(sp, level, pos, state, be);
			skillFromBlock(sp, state);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer sp) || entity instanceof ServerPlayer) return;
			onKill(sp, entity);
		});
	}

	private static boolean isOre(BlockState s) {
		return s.is(COAL) || s.is(COPPER) || s.is(IRON) || s.is(LAPIS) || s.is(REDSTONE)
			|| s.is(GOLD) || s.is(DIAMOND) || s.is(EMERALD) || s.is(Blocks.ANCIENT_DEBRIS) || s.is(ALL_ORES);
	}

	/** Mining stone, ores, chopping and harvesting ripe crops can all award a random passive skill. */
	private static void skillFromBlock(ServerPlayer sp, BlockState s) {
		double chance = 0.0004, boost = 0;
		if (s.is(Blocks.ANCIENT_DEBRIS) || s.is(DIAMOND) || s.is(EMERALD)) { chance = 0.05; boost = 1.5; }
		else if (isOre(s)) { chance = 0.012; boost = 0.5; }
		else if (s.is(BlockTags.LOGS)) chance = 0.0025;
		else if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) chance = 0.004;
		else if (s.is(BlockTags.MINEABLE_WITH_PICKAXE)) chance = 0.0012;
		SystemLogic.rollSkill(sp, chance, boost);
	}

	/** PER: chance to double the drops of ores and logs (works for ores/logs of any mod). */
	private static void doubleDrops(ServerPlayer sp, Level level, BlockPos pos, BlockState state, BlockEntity be) {
		boolean log = state.is(BlockTags.LOGS), ore = isOre(state);
		if ((!log && !ore) || !(level instanceof ServerLevel sl)) return;
		double chance = Perks.doubleDrop(SystemLogic.data(sp));
		if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) return;
		for (ItemStack drop : Block.getDrops(state, sl, pos, be, sp, sp.getMainHandItem())) {
			if (ore && drop.getItem() == state.getBlock().asItem()) continue; // silk touch: no free ore blocks
			Block.popResource(sl, pos, drop.copy());
		}
	}

	private static double blockExp(BlockState s, float hardness) {
		double exp = hardness <= 0 ? 0.2 : 0.4 + Math.min(hardness, 10f) * 0.6;
		if (s.is(BlockTags.LOGS)) exp += 0.5;
		if (s.is(CROPS)) exp += 1.5; // harvesting crops
		if (s.is(COAL) || s.is(COPPER)) exp += 3;
		else if (s.is(IRON) || s.is(LAPIS) || s.is(REDSTONE)) exp += 5;
		else if (s.is(GOLD)) exp += 8;
		else if (s.is(DIAMOND)) exp += 20;
		else if (s.is(EMERALD)) exp += 25;
		else if (s.is(Blocks.ANCIENT_DEBRIS)) exp += 40;
		else if (s.is(ALL_ORES)) exp += 6; // ore from another mod
		return exp;
	}

	private static void onKill(ServerPlayer sp, LivingEntity victim) {
		double hp = victim.getMaxHealth();
		// vanilla bosses + any modded boss-sized monster (200+ HP)
		boolean boss = victim instanceof EnderDragon || victim instanceof WitherBoss || (victim instanceof Enemy && hp >= 200);
		boolean hostile = victim instanceof Enemy;

		double exp = hp * (hostile ? 0.8 : 0.3) + (hostile ? 4 : 0);
		if (boss) exp += 1000;
		SystemLogic.addExp(sp, exp);
		if (hostile) QuestLogic.progress(sp, QuestType.KILL, 1);
		int harvest = SystemLogic.data(sp).rank(PassiveSkill.SOUL_HARVEST);
		if (hostile && harvest > 0) sp.heal(2.0f * harvest);

		// skill drop from monsters
		double chance = boss ? 1.0 : hostile ? Math.min(0.5, 0.02 + hp * 0.002) : 0.005;
		double boost = boss ? 4.0 : hostile ? Math.min(2.0, hp / 40.0) : 0;
		SystemLogic.rollSkill(sp, chance, boost);

		// odd items + enchanted gear, driven by PER
		LootLogic.onKill(sp, victim, hostile, boss);
	}

	/** Called from ResultSlotMixin whenever a player takes a crafted item. */
	public static void onCraft(ServerPlayer sp, ItemStack stack) {
		if (stack.isEmpty() || sp.isCreative()) return;
		int tier = stack.getRarity().ordinal(); // COMMON, UNCOMMON, RARE, EPIC
		double exp = 1.0 + stack.getCount() * 0.5 + tier * 5.0;
		SystemLogic.addExp(sp, exp);
		QuestLogic.progress(sp, QuestType.CRAFT, stack.getCount());
		SystemLogic.rollSkill(sp, 0.004 + tier * 0.01, tier * 0.5);
	}

	/** Called from FurnaceResultSlotMixin when a player takes smelted/cooked output. */
	public static void onSmelt(ServerPlayer sp, ItemStack stack) {
		if (stack.isEmpty() || sp.isCreative()) return;
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		double per = 1.2;
		if (path.contains("ingot") || path.contains("netherite") || path.equals("diamond") || path.equals("emerald")) per += 3.0;
		else if (path.contains("cooked") || path.contains("baked") || path.contains("steak")) per += 1.0;
		SystemLogic.addExp(sp, per * stack.getCount());
		QuestLogic.progress(sp, QuestType.SMELT, stack.getCount());
		SystemLogic.rollSkill(sp, 0.002 * stack.getCount(), 0);
	}
}
