package com.example.solosystem.client;

import com.example.solosystem.*;
import com.example.solosystem.net.AllocatePayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Status / Skills / Quests / Resistance window. Buttons are real widgets; text is drawn in extractRenderState. */
public class SystemScreen extends Screen {
	private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};
	private static final String[] TABS = {"STATUS", "SKILLS", "QUESTS", "RESISTANCE"};
	private int tab = 0;
	private record Label(Component text, int x, int y) {}
	private final List<Label> labels = new ArrayList<>();
	private int timerX, timerY;

	public SystemScreen() { super(Component.literal("SYSTEM")); }

	public void refresh() { rebuildWidgets(); }

	/** The System window must NOT pause the game: quests, timers and mobs keep running while it is open. */
	public boolean isPauseScreen() { return false; }

	@Override
	protected void init() {
		labels.clear();
		PlayerData d = ClientState.data;
		int cx = width / 2;
		int y = Math.max(8, height / 2 - 120);

		for (int i = 0; i < TABS.length; i++) {
			final int idx = i;
			addRenderableWidget(Button.builder(Component.literal(tab == i ? "[" + TABS[i] + "]" : TABS[i]), b -> { tab = idx; rebuildWidgets(); })
				.bounds(cx - 174 + i * 88, y, 86, 20).build());
		}
		y += 30;

		switch (tab) {
			case 0 -> status(d, cx, y);
			case 1 -> skills(d, cx, y);
			case 2 -> quests(d, cx, y);
			default -> resistance(d, cx, y);
		}
	}

	private void label(Component c, int x, int y) { labels.add(new Label(c, x, y)); }

	// ------------------------------------------------------------------ STATUS

	private void status(PlayerData d, int cx, int y) {
		int left = cx - 150;
		label(Component.literal("LEVEL  " + d.level).withStyle(ChatFormatting.GOLD), left, y); y += 14;

		double frac = Math.min(1.0, d.exp / d.expToNext());
		int filled = (int) (frac * 24);
		String bar = "[" + "=".repeat(filled) + "-".repeat(24 - filled) + "]";
		label(Component.literal("EXP " + bar + " " + (int) d.exp + "/" + (int) d.expToNext()).withStyle(ChatFormatting.AQUA), left, y); y += 14;

		label(Component.literal("Available Points: " + d.points).withStyle(ChatFormatting.YELLOW), left, y); y += 18;

		for (Stat s : Stat.values()) {
			label(Component.literal(s.name() + ": " + d.stat(s)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), left, y + 6);
			label(Component.literal(s.bonuses(d)).withStyle(ChatFormatting.GREEN), left, y + 20);
			Button b1 = Button.builder(Component.literal("+1"), b -> send(s, 1)).bounds(cx + 92, y + 2, 28, 18).build();
			Button b5 = Button.builder(Component.literal("+5"), b -> send(s, 5)).bounds(cx + 122, y + 2, 28, 18).build();
			b1.active = b5.active = d.points > 0;
			addRenderableWidget(b1);
			addRenderableWidget(b5);
			y += 34;
		}
	}

	private void send(Stat s, int n) { ClientPlayNetworking.send(new AllocatePayload(s.id(), n)); }

	// ------------------------------------------------------------------ SKILLS

	private void skills(PlayerData d, int cx, int y) {
		int left = cx - 160;
		PassiveSkill[] all = PassiveSkill.values();
		int found = 0;
		for (PassiveSkill s : all) if (d.rank(s) > 0) found++;
		label(Component.literal("Passive Skills discovered: " + found + "/" + all.length).withStyle(ChatFormatting.YELLOW), left, y);
		y += 14;

		int rowH = Math.max(9, Math.min(12, (height - y - 8) / all.length));
		for (int i = all.length - 1; i >= 0; i--) { // rarest first
			PassiveSkill s = all[i];
			int rank = d.rank(s);
			Component c;
			if (rank > 0) {
				c = Component.literal(s.displayName + " " + ROMAN[rank] + "/" + s.maxRank).withStyle(s.rarity.color)
					.append(Component.literal("  " + s.desc).withStyle(ChatFormatting.GRAY));
			} else {
				c = Component.literal("??? (" + s.rarity.name() + ")").withStyle(ChatFormatting.DARK_GRAY);
			}
			label(c, left, y);
			y += rowH;
		}
	}

	// ------------------------------------------------------------------ QUESTS

	private void quests(PlayerData d, int cx, int y) {
		int left = cx - 160;
		label(Component.literal("DAILY QUESTS").withStyle(ChatFormatting.GOLD), left, y);
		timerX = left + 100;
		timerY = y;
		y += 16;

		if (d.quests.isEmpty()) {
			label(Component.literal("Waiting for quests...").withStyle(ChatFormatting.GRAY), left, y);
			return;
		}
		for (Quest q : d.quests) {
			SkillRarity r = QuestLogic.tierRarity(Math.max(0, Math.min(4, q.tier)));
			QuestType t = QuestType.byId(q.type);
			String name = t == null ? q.type : t.label;
			Component head = Component.literal("[" + r.name() + "] " + "*".repeat(q.tier + 1) + "  " + name + "  " + (int) q.progress + "/" + q.target).withStyle(r.color);
			if (q.done) head = head.copy().append(Component.literal("  CLEAR").withStyle(ChatFormatting.GREEN));
			label(head, left, y); y += 11;
			label(Component.literal("Reward: " + QuestLogic.rewardLine1(q)).withStyle(ChatFormatting.GRAY), left + 8, y); y += 10;
			label(Component.literal(QuestLogic.rewardLine2(q)).withStyle(ChatFormatting.DARK_GRAY), left + 8, y); y += 16;
		}
		label(Component.literal("Resets every Minecraft day (24000 ticks). Rewards are random per quest.").withStyle(ChatFormatting.DARK_GRAY), left, y);
	}

	private String timeLeft() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return "--:--";
		long left = Math.max(0, ClientState.data.questExpiry - mc.level.getGameTime());
		long sec = left / 20;
		return String.format("%02d:%02d", sec / 60, sec % 60);
	}

	// ------------------------------------------------------------------ RESISTANCE

	private void resistance(PlayerData d, int cx, int y) {
		int left = cx - 170;
		label(Component.literal("RESISTANCE").withStyle(ChatFormatting.GOLD)
			.append(Component.literal("   learned by taking hits / effects (max 75%)").withStyle(ChatFormatting.GRAY)), left, y);
		y += 16;

		Resist[] all = Resist.values();
		int perCol = (all.length + 1) / 2;
		for (int i = 0; i < all.length; i++) {
			Resist r = all[i];
			int col = i / perCol, row = i % perCol;
			int x = left + col * 175;
			int ry = y + row * 14;
			int count = d.resist.getOrDefault(r.id, 0);
			Component c;
			if (count > 0) {
				double pct = r.percent(count) * 100.0;
				ChatFormatting color = pct >= 50 ? ChatFormatting.GREEN : pct >= 20 ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
				c = Component.literal(r.label + ": " + String.format("%.1f%%", pct)).withStyle(color)
					.append(Component.literal("  (" + count + ")").withStyle(ChatFormatting.DARK_GRAY));
			} else {
				c = Component.literal("?  ???").withStyle(ChatFormatting.DARK_GRAY);
			}
			label(c, x, ry);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		for (Label l : labels) graphics.text(this.font, l.text(), l.x(), l.y(), 0xFFFFFFFF, true);
		if (tab == 2 && !ClientState.data.quests.isEmpty()) {
			graphics.text(this.font, Component.literal("Time left: " + timeLeft()).withStyle(ChatFormatting.RED),
				timerX, timerY, 0xFFFFFFFF, true);
		}
	}
}
