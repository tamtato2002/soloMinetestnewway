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

/** Status / Skills / Quests / Resistance / Config window. Buttons are real widgets; text and icons are drawn in extractRenderState. */
public class SystemScreen extends Screen {
	private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};
	private static final String[] TABS = {"STATUS", "SKILLS", "QUESTS", "RESIST", "CONFIG"};
	private static final String[] TAB_ICONS = {"status", "skills", "quests", "resist", "config"};
	private static final int TAB_W = 76, TAB_GAP = 2;
	private int tab = 0;
	private record Label(Component text, int x, int y) {}
	private record IconDraw(String name, int x, int y) {}
	private final List<Label> labels = new ArrayList<>();
	private final List<IconDraw> icons = new ArrayList<>();
	private int tabsX, tabsY;
	private int timerX, timerY;

	public SystemScreen() { super(Component.literal("SYSTEM")); }

	public void refresh() { rebuildWidgets(); }

	/** The System window must NOT pause the game: quests, timers and mobs keep running while it is open. */
	public boolean isPauseScreen() { return false; }

	@Override
	protected void init() {
		labels.clear();
		icons.clear();
		PlayerData d = ClientState.data;
		int cx = width / 2;
		int y = Math.max(8, height / 2 - 120);

		tabsX = cx - (TABS.length * TAB_W + (TABS.length - 1) * TAB_GAP) / 2;
		tabsY = y;
		for (int i = 0; i < TABS.length; i++) {
			final int idx = i;
			Component name = Component.literal(TABS[i]).withStyle(tab == i ? ChatFormatting.GOLD : ChatFormatting.WHITE);
			addRenderableWidget(Button.builder(name, b -> { tab = idx; rebuildWidgets(); })
				.bounds(tabsX + i * (TAB_W + TAB_GAP), y, TAB_W, 20).build());
			icons.add(new IconDraw(TAB_ICONS[i], tabsX + i * (TAB_W + TAB_GAP) + 3, y + 2));
		}
		y += 30;

		switch (tab) {
			case 0 -> status(d, cx, y);
			case 1 -> skills(d, cx, y);
			case 2 -> quests(d, cx, y);
			case 3 -> resistance(d, cx, y);
			default -> config(cx, y);
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
			icons.add(new IconDraw(s.id(), left, y + 4));
			label(Component.literal(s.name() + ": " + d.stat(s)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), left + 22, y + (ClientConfig.statBonuses ? 6 : 10));
			if (ClientConfig.statBonuses) label(Component.literal(s.bonuses(d)).withStyle(ChatFormatting.GREEN), left + 22, y + 20);
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

	private static final int SKILLS_PER_PAGE = 12;
	private int skillPage = 0;

	private void skills(PlayerData d, int cx, int y) {
		int left = cx - 160;
		PassiveSkill[] all = PassiveSkill.values();
		int found = 0;
		for (PassiveSkill s : all) if (d.rank(s) > 0) found++;
		int pages = (all.length + SKILLS_PER_PAGE - 1) / SKILLS_PER_PAGE;
		skillPage = Math.max(0, Math.min(pages - 1, skillPage));

		icons.add(new IconDraw("skills", left, y - 3));
		label(Component.literal("Passive Skills discovered: " + found + "/" + all.length
			+ "    Page " + (skillPage + 1) + "/" + pages).withStyle(ChatFormatting.YELLOW), left + 20, y + 2);
		y += 18;

		int start = skillPage * SKILLS_PER_PAGE;
		for (int n = 0; n < SKILLS_PER_PAGE; n++) {
			int idx = all.length - 1 - (start + n); // rarest first
			if (idx < 0) break;
			PassiveSkill s = all[idx];
			int rank = d.rank(s);
			Component c;
			if (rank > 0) {
				c = Component.literal(s.displayName + " " + ROMAN[rank] + "/" + s.maxRank).withStyle(s.rarity.color)
					.append(Component.literal("  " + s.desc).withStyle(ChatFormatting.GRAY));
			} else {
				c = Component.literal("??? (" + s.rarity.name() + ")").withStyle(ChatFormatting.DARK_GRAY);
			}
			label(c, left, y);
			y += 12;
		}

		y += 4;
		Button prev = Button.builder(Component.literal("< Prev"), b -> { skillPage--; rebuildWidgets(); })
			.bounds(cx - 64, y, 60, 20).build();
		Button next = Button.builder(Component.literal("Next >"), b -> { skillPage++; rebuildWidgets(); })
			.bounds(cx + 4, y, 60, 20).build();
		prev.active = skillPage > 0;
		next.active = skillPage < pages - 1;
		addRenderableWidget(prev);
		addRenderableWidget(next);
	}

	// ------------------------------------------------------------------ QUESTS

	private void quests(PlayerData d, int cx, int y) {
		int left = cx - 160;
		icons.add(new IconDraw("quests", left, y - 3));
		label(Component.literal("DAILY QUESTS").withStyle(ChatFormatting.GOLD), left + 20, y + 2);
		timerX = left + 120;
		timerY = y + 2;
		y += 20;

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
		label(Component.literal("Resets every Minecraft day (24000 ticks). Sleeping skips the timer too. Rewards are random.").withStyle(ChatFormatting.DARK_GRAY), left, y);
	}

	private String timeLeft() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return "--:--";
		long left = Math.max(0, ClientState.data.questLeft - (mc.level.getGameTime() - ClientState.syncGameTime));
		long sec = left / 20;
		return String.format("%02d:%02d", sec / 60, sec % 60);
	}

	// ------------------------------------------------------------------ RESISTANCE

	private void resistance(PlayerData d, int cx, int y) {
		int left = cx - 170;
		icons.add(new IconDraw("resist", left, y - 3));
		label(Component.literal("RESISTANCE").withStyle(ChatFormatting.GOLD)
			.append(Component.literal("   learned by taking hits / effects (max 75%)").withStyle(ChatFormatting.GRAY)), left + 20, y + 2);
		y += 20;

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

	// ------------------------------------------------------------------ CONFIG

	private static final double[] EXP_STEPS = {0.5, 1.0, 1.5, 2.0, 3.0, 5.0};
	private static final double[] SKILL_STEPS = {0.0, 0.5, 1.0, 2.0, 3.0, 5.0};
	private static final double[] GEAR_STEPS = {0.0, 0.25, 0.5, 0.75, 1.0};

	private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

	private static String mult(double v) { return (v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v)) + "x"; }

	/** next value in the list after the current one (wraps around; unknown values snap to the nearest step first) */
	private static double nextStep(double[] steps, double cur) {
		int best = 0;
		for (int i = 1; i < steps.length; i++) if (Math.abs(steps[i] - cur) < Math.abs(steps[best] - cur)) best = i;
		return steps[(best + 1) % steps.length];
	}

	private void cell(int col, int row, int top, int cx, String text, Runnable action) {
		int x = col == 0 ? cx - 155 : cx + 5;
		addRenderableWidget(Button.builder(Component.literal(text), b -> { action.run(); rebuildWidgets(); })
			.bounds(x, top + row * 22, 150, 20).build());
	}

	private void config(int cx, int y) {
		int left = cx - 155;
		icons.add(new IconDraw("config", left, y - 3));
		label(Component.literal("DISPLAY").withStyle(ChatFormatting.GOLD)
			.append(Component.literal("   this client only, saved automatically").withStyle(ChatFormatting.GRAY)), left + 20, y + 2);
		y += 20;

		cell(0, 0, y, cx, "Quest pop-ups: " + onOff(ClientConfig.toasts), () -> { ClientConfig.toasts = !ClientConfig.toasts; ClientConfig.save(); });
		cell(1, 0, y, cx, "Progress pop-ups: " + onOff(ClientConfig.progressToasts), () -> { ClientConfig.progressToasts = !ClientConfig.progressToasts; ClientConfig.save(); });
		cell(0, 1, y, cx, "Pop-up side: " + ClientConfig.SIDE[ClientConfig.side], () -> { ClientConfig.side = (ClientConfig.side + 1) % ClientConfig.SIDE.length; ClientConfig.save(); });
		cell(1, 1, y, cx, "Pop-up height: " + ClientConfig.TOP[ClientConfig.top], () -> { ClientConfig.top = (ClientConfig.top + 1) % ClientConfig.TOP.length; ClientConfig.save(); });
		cell(0, 2, y, cx, "Pop-up stay: " + ClientConfig.STAY[ClientConfig.stay], () -> { ClientConfig.stay = (ClientConfig.stay + 1) % ClientConfig.STAY.length; ClientConfig.save(); });
		cell(1, 2, y, cx, "Animation: " + ClientConfig.ANIM[ClientConfig.anim], () -> { ClientConfig.anim = (ClientConfig.anim + 1) % ClientConfig.ANIM.length; ClientConfig.save(); });
		cell(0, 3, y, cx, "Stat bonus text: " + onOff(ClientConfig.statBonuses), () -> { ClientConfig.statBonuses = !ClientConfig.statBonuses; ClientConfig.save(); });
		Button preview = Button.builder(Component.literal("Preview pop-up"), b -> QuestToasts.preview())
			.bounds(cx + 5, y + 3 * 22, 73, 20).build();
		Button reset = Button.builder(Component.literal("Reset"), b -> { ClientConfig.reset(); rebuildWidgets(); })
			.bounds(cx + 82, y + 3 * 22, 73, 20).build();
		addRenderableWidget(preview);
		addRenderableWidget(reset);
		y += 4 * 22 + 8;

		label(Component.literal("GAMEPLAY").withStyle(ChatFormatting.GOLD)
			.append(Component.literal("   whole world").withStyle(ChatFormatting.GRAY)), left + 20, y + 2);
		icons.add(new IconDraw("per", left, y - 3));
		y += 20;

		if (Minecraft.getInstance().getSingleplayerServer() != null) {
			cell(0, 0, y, cx, "EXP rate: " + mult(ServerConfig.expMultiplier), () -> { ServerConfig.expMultiplier = nextStep(EXP_STEPS, ServerConfig.expMultiplier); ServerConfig.save(); });
			cell(1, 0, y, cx, "Skill drop rate: " + mult(ServerConfig.skillChanceMultiplier), () -> { ServerConfig.skillChanceMultiplier = nextStep(SKILL_STEPS, ServerConfig.skillChanceMultiplier); ServerConfig.save(); });
			cell(0, 1, y, cx, "Mod gear reward: " + (int) Math.round(ServerConfig.modGearChance * 100) + "%", () -> { ServerConfig.modGearChance = nextStep(GEAR_STEPS, ServerConfig.modGearChance); ServerConfig.save(); });
			label(Component.literal("Mod item lists: config/solosystem.json").withStyle(ChatFormatting.DARK_GRAY), cx + 8, y + 22 + 6);
		} else {
			label(Component.literal("Only the world host can change these.").withStyle(ChatFormatting.GRAY), left, y + 2);
			label(Component.literal("On a server: edit config/solosystem.json and restart.").withStyle(ChatFormatting.DARK_GRAY), left, y + 14);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		for (Label l : labels) graphics.text(this.font, l.text(), l.x(), l.y(), 0xFFFFFFFF, true);
		for (IconDraw i : icons) Icons.draw(graphics, i.name(), i.x(), i.y(), 1);
		// gold underline marks the open tab
		graphics.fill(tabsX + tab * (TAB_W + TAB_GAP), tabsY + 21, tabsX + tab * (TAB_W + TAB_GAP) + TAB_W, tabsY + 23, 0xFFFFC040);
		if (tab == 2 && !ClientState.data.quests.isEmpty()) {
			graphics.text(this.font, Component.literal("Time left: " + timeLeft()).withStyle(ChatFormatting.RED),
				timerX, timerY, 0xFFFFFFFF, true);
		}
	}
}
