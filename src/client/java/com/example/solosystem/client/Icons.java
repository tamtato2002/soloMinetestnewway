package com.example.solosystem.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Minecraft-style 16x16 pixel-art icons, drawn with plain fills (no texture files, works with any resource pack).
 * Pixel art lives in the strings below (one char = one pixel, see PALETTE). tools/icons_gen.py renders a preview PNG of the same art.
 */
public final class Icons {
	private static final Map<Character, Integer> PALETTE = new HashMap<>();
	private static final Map<String, int[]> DATA = new HashMap<>();

	private Icons() {}

	static {
		Map<Character, Integer> p = PALETTE;
		p.put('k', 0xFF1B1B1B);
		p.put('w', 0xFFFFFFFF);
		p.put('g', 0xFFC6C6C6);
		p.put('G', 0xFF7B7B7B);
		p.put('D', 0xFF4A4A4A);
		p.put('b', 0xFF9C6A3A);
		p.put('B', 0xFF5E3B1E);
		p.put('r', 0xFFE03C3C);
		p.put('R', 0xFF8F1C1C);
		p.put('q', 0xFFFF8A8A);
		p.put('o', 0xFFF0932A);
		p.put('y', 0xFFFCE44A);
		p.put('Y', 0xFFC99A1A);
		p.put('c', 0xFF5CE0E0);
		p.put('C', 0xFF2A7FD0);
		p.put('n', 0xFF163A8A);
		p.put('l', 0xFF7CE04A);
		p.put('L', 0xFF2F8F2A);
		p.put('p', 0xFFB06CE8);
		p.put('P', 0xFF5A2A9A);
		p.put('s', 0xFFE0B08A);
		p.put('S', 0xFFB07C56);
		p.put('h', 0xFF4A2C14);
		p.put('W', 0xFFEEE6D2);
		p.put('V', 0xFFC9BD9A);
		put("status",
			"................",
			"..kkkkkkkkkkkk..",
			".khhhhhhhhhhhhk.",
			".khhhhhhhhhhhhk.",
			".khhhhhhhhhhhhk.",
			".khhhhhhhhhhhhk.",
			".khhsssssssshhk.",
			".khhwCCssCCwhhk.",
			".ksswCCssCCwssk.",
			".ksssssSSsssssk.",
			".ksssssSSsssssk.",
			".ksssRRRRRRsssk.",
			".kssssssssssssk.",
			".kSSSSSSSSSSSSk.",
			"..kkkkkkkkkkkk..",
			"................");
		put("skills",
			"...kkkkkkkkkk...",
			"..kppppppppppk..",
			".knnPPPPPPPPpk..",
			".knnPPPPPPPPpk..",
			".knnPcPPPPPPpk..",
			".knnPPPPyPPPpk..",
			".knnPPPywyPPpk..",
			".knnPPyywyyPpk..",
			".knnPPPyyyPPpk..",
			".knnPPPPyPPPpk..",
			".knnPPPPPPPcpk..",
			".knnPcPPPPPPpk..",
			".knnPPPPPPPPpk..",
			".knnPPPPPPPPpk..",
			"..kpWWWWWWWWWk..",
			"...kkkkkkkkkk...");
		put("quests",
			"..kkkkkkkkk.....",
			".kwwwwwwwwwk....",
			".kwWWWWWWWVVk...",
			".kwWWWWWWWWVVk..",
			".kwWWWWWWWWWVk..",
			".kwWGGGGGGWWVk..",
			".kwWWWWWWWWWVk..",
			".kwWGGGGGGWWVk..",
			".kwWWWWWWWWWVk..",
			".kwWGGGGGlLWVk..",
			".kwWWWWWlLWWVk..",
			".kwWWlWlLWWWVk..",
			".kwWWLlLWWWWVk..",
			".kwWWWLWWWWWVk..",
			".kwVVVVVVVVVVk..",
			"..kkkkkkkkkkk...");
		put("resist",
			"..kkkkkkkkkkkk..",
			".kgggggggggggGk.",
			".kgCCCCYYCCCCGk.",
			".kgCCCCyoCCCCGk.",
			".kgCCCCyoCCCCGk.",
			".kgyyyyyyyyyyGk.",
			".kgyyyyyyoyyyGk.",
			".kgCCCCyyCCCCGk.",
			".kgCCCCyoCCCCGk.",
			".kgCCCCyoCCCCGk.",
			".kCgCCCyoCCCGCk.",
			"..kCgCCyoCCGCk..",
			"...kCgCYYCGCk...",
			"....kCgCCGCk....",
			".....kCCCCk.....",
			"......kkkk......");
		put("config",
			".......kk.......",
			"....k.kwgk.k....",
			"...kwkkggkkgk...",
			"..kwwwgggggggk..",
			".kwwwggDDgggggk.",
			"..kwgDDDDDDggk..",
			".kkggDkkkkDggkk.",
			"kwggDDk..kDDGGGk",
			"kgggDDk..kDDGGGk",
			".kkggDkkkkDGGkk.",
			"..kggDDDDDDGGk..",
			".kgggggDDGGGGGk.",
			"..kggggGGGGGGk..",
			"...kgkkGGkkGk...",
			"....k.kGGk.k....",
			".......kk.......");
		put("str",
			".............kk.",
			"............kwgk",
			"...........kwgGk",
			"..........kwgGk.",
			".........kwgGk..",
			"........kwgGk...",
			".......kwgGk....",
			"...kk.kwgGk.....",
			"..kYokwgGk......",
			"...kYogGk.......",
			"...kkYok........",
			"..kbkkYok.......",
			".kBk..kYok......",
			"kBk....kk.......",
			"YYk.............",
			"kYk.............");
		put("vit",
			"..kkkk....kkkk..",
			".krrrrk..krrrrk.",
			"krqqrrrkkrrrrrrk",
			"krqrrrrrrrrrrrRk",
			"rrrrrrrrrrrrrrRR",
			"rrrrrrrrrrrrrrRR",
			"rrrrrrrrrrrrrrRR",
			"krrrrrrrrrrrrRRk",
			"krrrrrrrrrrrrRRk",
			".krrrrrrrrrrRRk.",
			"..krrrrrrrrRRk..",
			"...krrrrrrRRk...",
			"....krrrrRRk....",
			".....krrRRk.....",
			"......kRRk......",
			".......kk.......");
		put("agi",
			"................",
			"................",
			"........kkkkkk..",
			".....kkkwwwwVWk.",
			"....kwwwwwwVWk..",
			"...kwwwwcwVWVVk.",
			"....kwwwwcWVVk..",
			"...kwwcwVWVVVVk.",
			"..kwwwwVWVVVVk..",
			"...kwwVWVVVVk...",
			"..kwwVWVVVVVVk..",
			"...kVWVVVVVVk...",
			"..kbWVVVVkkk....",
			".kbkkkkkk.......",
			"kbk.............",
			"Bk..............");
		put("int",
			"................",
			"....kkkkkk......",
			"...knnnnnnkk....",
			"..knCCcCCCCnk...",
			".knCcwcCCnCCnk..",
			".knCcCCCCCCCnk..",
			"knCCCCnCCcCCCnk.",
			"knCCnCCCCCCCCnk.",
			"knCCCCCcCCCnCnk.",
			".knCCcCCCCCCCnk.",
			".knCCCCnCCCCnk..",
			"..knCCCCCCCCnk..",
			"...knnCCCCnnk...",
			"....kknnnnkk....",
			"......kkkk......",
			"................");
		put("per",
			".......kk.......",
			"....kkkLLkkk....",
			"...kLLLllLLLk...",
			"..kLLwlllllLLk..",
			".kLLwllllllcLLk.",
			".kLwlkkkkkkllLk.",
			".kLllkGGGGkllLk.",
			"kLlllkGyyGklLLLk",
			"kLlllkGyyGkLLLLk",
			".kLllkGGGGkLLLk.",
			".kLllkkkkkkLLLk.",
			".kLLclllLLLLLLk.",
			"..kLLllLLLLLLk..",
			"...kLLLLLLLLk...",
			"....kkkLLkkk....",
			".......kk.......");
	}

	private static void put(String name, String... rows) {
		int[] px = new int[256];
		for (int y = 0; y < 16; y++)
			for (int x = 0; x < 16; x++) px[y * 16 + x] = PALETTE.getOrDefault(rows[y].charAt(x), 0);
		DATA.put(name, px);
	}

	public static boolean has(String name) { return DATA.containsKey(name); }

	/** Draw icon "name" with its top-left corner at (x, y); each icon pixel becomes scale x scale screen pixels. */
	public static void draw(GuiGraphicsExtractor g, String name, int x, int y, int scale) {
		int[] px = DATA.get(name);
		if (px == null) return;
		for (int row = 0; row < 16; row++) {
			int col = 0;
			while (col < 16) {
				int c = px[row * 16 + col];
				if (c == 0) { col++; continue; }
				int end = col + 1; // merge horizontal runs of the same colour into one fill
				while (end < 16 && px[row * 16 + end] == c) end++;
				g.fill(x + col * scale, y + row * scale, x + end * scale, y + (row + 1) * scale, c);
				col = end;
			}
		}
	}
}
