package com.example.solosystem.client;

import com.example.solosystem.PlayerData;
import com.example.solosystem.Quest;
import com.example.solosystem.QuestLogic;
import com.example.solosystem.QuestType;
import com.example.solosystem.SkillRarity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Slide-in notifications on the top-left for Daily Quest progress / clear.
 * Animation runs on real time (not game ticks), so it stays smooth at any FPS, with the
 * system lagging, or while the System window is open.
 */
public final class QuestToasts {
	private static final int W = 160, H = 34, GAP = 4, TOP = 36;
	private static final long SLIDE_IN_MS = 320;
	private static final long SLIDE_OUT_MS = 260;
	private static final long LIFE_PROGRESS_MS = 2200;
	private static final long LIFE_DONE_MS = 4000;

	private static final class Toast {
		String key, name;
		int cur, target, tier;
		boolean done;
		long born, expires;
		float shownY = -1;       // smoothed vertical position (slides when toasts above disappear)
		float shownFill = 0;     // smoothed progress bar
		long flash;              // time of last update, for a short highlight
	}

	private static final List<Toast> LIST = new ArrayList<>();

	private QuestToasts() {}

	private static long now() { return System.nanoTime() / 1_000_000L; }

	/** Compare old and new quest data and pop a toast for anything that moved. */
	public static void diff(PlayerData old, PlayerData now) {
		if (old.quests.isEmpty() || now.quests.isEmpty()) return;
		for (Quest q : now.quests) {
			Quest o = null;
			for (Quest x : old.quests) if (x.type.equals(q.type) && x.tier == q.tier && x.target == q.target) o = x;
			if (o == null) continue;
			QuestType t = QuestType.byId(q.type);
			String name = t == null ? q.type : t.label;
			if (q.done && !o.done) push(q.type, name, q.target, q.target, q.tier, true);
			else if (!q.done && (int) q.progress > (int) o.progress) push(q.type, name, (int) q.progress, q.target, q.tier, false);
		}
	}

	private static void push(String key, String name, int cur, int target, int tier, boolean done) {
		long n = now();
		long life = done ? LIFE_DONE_MS : LIFE_PROGRESS_MS;
		for (Toast t : LIST) {
			if (t.key.equals(key)) {
				t.cur = cur; t.target = target; t.done = done; t.name = name;
				t.expires = n + life;
				t.flash = n;
				// already on screen: keep it there, never re-slide
				if (n - t.born < SLIDE_IN_MS) t.born = Math.min(t.born, n - SLIDE_IN_MS);
				return;
			}
		}
		Toast t = new Toast();
		t.key = key; t.name = name; t.cur = cur; t.target = target; t.tier = tier; t.done = done;
		t.born = n; t.expires = n + life; t.flash = n;
		LIST.add(t);
	}

	public static void tick() {
		long n = now();
		LIST.removeIf(t -> n >= t.expires + SLIDE_OUT_MS);
	}

	private static float easeOutCubic(float k) { float i = 1f - k; return 1f - i * i * i; }
	private static float easeInCubic(float k) { return k * k * k; }

	public static void render(GuiGraphicsExtractor g) {
		if (LIST.isEmpty()) return;
		var font = Minecraft.getInstance().font;
		long n = now();
		float targetY = TOP;

		for (Toast t : LIST) {
			// horizontal slide: ease-out in, ease-in out (starts when the life time ends)
			float inK = Math.min(1f, (n - t.born) / (float) SLIDE_IN_MS);
			float outK = n > t.expires ? Math.min(1f, (n - t.expires) / (float) SLIDE_OUT_MS) : 0f;
			float hidden = outK > 0 ? easeInCubic(outK) : 1f - easeOutCubic(inK); // 1 = fully off-screen
			int x = 6 - (int) ((W + 12) * hidden);

			// vertical: glide toward the slot instead of snapping
			if (t.shownY < 0) t.shownY = targetY;
			t.shownY += (targetY - t.shownY) * 0.25f;
			if (Math.abs(targetY - t.shownY) < 0.3f) t.shownY = targetY;
			int y = Math.round(t.shownY);
			targetY += (H + GAP) * (1f - (outK > 0 ? easeInCubic(outK) : 0f));

			// smooth progress bar
			float want = (float) Math.min(1.0, t.cur / (double) Math.max(1, t.target));
			t.shownFill += (want - t.shownFill) * 0.3f;
			if (Math.abs(want - t.shownFill) < 0.004f) t.shownFill = want;

			int tier = Math.max(0, Math.min(4, t.tier));
			SkillRarity r = QuestLogic.tierRarity(tier);
			int[] rgbByTier = {0xFFFFFF, 0x55FF55, 0x55FFFF, 0xFF55FF, 0xFFAA00};
			int accent = t.done ? 0xFF4CD964 : (0xFF000000 | rgbByTier[tier]);

			// short brighten when the toast updates
			float fl = Math.max(0f, 1f - (n - t.flash) / 220f);
			int bg = 0xE0101828;
			if (fl > 0) {
				int add = (int) (fl * 28);
				bg = 0xE0000000 | (Math.min(255, 0x10 + add) << 16) | (Math.min(255, 0x18 + add) << 8) | Math.min(255, 0x28 + add);
			}

			g.fill(x, y, x + W, y + H, bg);
			g.fill(x, y, x + W, y + 1, accent);
			g.fill(x, y + H - 1, x + W, y + H, accent);
			g.fill(x + W - 1, y, x + W, y + H, accent);
			g.fill(x, y, x + 3, y + H, accent);

			String title = t.done ? "QUEST CLEAR!" : "DAILY QUEST  [" + r.name() + "]";
			g.text(font, Component.literal(title).withStyle(t.done ? ChatFormatting.GREEN : ChatFormatting.GRAY), x + 8, y + 4, 0xFFFFFFFF, true);
			g.text(font, Component.literal(t.name + "   " + t.cur + "/" + t.target), x + 8, y + 15, 0xFFFFFFFF, true);

			int bx = x + 8, by = y + 27, bw = W - 16;
			g.fill(bx, by, bx + bw, by + 3, 0xFF2A3350);
			g.fill(bx, by, bx + (int) (bw * t.shownFill), by + 3, accent);

			if (t.done) check(g, x + W - 24, y + 5, 0xFF55FF55);
		}
	}

	/** Pixel-art green tick. */
	private static void check(GuiGraphicsExtractor g, int x, int y, int color) {
		int[][] px = {{0, 3}, {1, 4}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, {6, 1}, {7, 0}};
		for (int[] p : px) g.fill(x + p[0] * 2, y + p[1] * 2, x + p[0] * 2 + 3, y + p[1] * 2 + 3, color);
	}
}
