# Solo System (Fabric 26.3) - v0.2

Upload everything in this folder to a GitHub repo -> Actions tab -> "build" -> Artifacts -> download.
Use build/libs/solo-system-0.4.0.jar (NOT the -sources jar). Press Y in game.

## What's new in 0.2
- The System window (Y) no longer pauses the game; the Daily Quest timer keeps running.
- Quest pop-ups are Minecraft-achievement style (top-right, fast slide-in) and are sent instantly.
- Works with other mods: modded ores (c:ores), logs, hostile mobs/bosses, crafting/furnace slots, and
  quest rewards can include items/weapons/armor from any installed mod (see below).
- Stats are now real quality-of-life perks (shown live in the STATUS tab):
  - STR: attack damage, faster mining AND chopping (all tools/mods), chance to save weapon/tool durability
  - VIT: max HP, chance to save armor durability, HP regeneration
  - AGI: move + attack speed, much faster fishing, less fall damage
  - INT: EXP gain, enchanting gives +1 level on enchants and sometimes an extra enchantment
  - PER: luck, reach, chance to double ore/log drops, better monster loot

## Mod rewards: config/solosystem.json (created on first launch)
- `autoDiscoverModGear`: every tool/weapon/armor from other mods can appear as a bonus quest reward (sorted into tiers by durability + rarity).
- `autoGearChance`: chance (0-1) that a quest gets such a bonus item.
- `extraRewards`: hand-picked items per tier (0=COMMON ... 4=LEGENDARY), e.g. `{ "item": "modid:item", "min": 1, "max": 3 }`.
- `blacklist`: item ids or whole mod ids to never give.

## v0.3
- Daily Quest timer follows the in-game day clock: sleeping through the night skips the timer too.
  (The world day-time getter is found by reflection; if a Minecraft update hides it, check the log for
  "quests will not skip when sleeping" - the timer then simply runs in real time.)
- More ways to find passive skills: mining stone, ores (rarer ore = better odds), chopping logs, harvesting ripe crops,
  fishing, breeding, villager trades, enchanting, walking long distances, jumping - plus monster kills / crafting / smelting as before.
- 8 new high-tier passives. EPIC: Immovable Stance, Tireless Arms, Soul Harvest, Tidal Blessing.
  LEGENDARY: Sky Lord, Undying Will, Archmage's Mind, Midas Touch.
- Skills tab now has pages (Prev / Next).
- Old saves: the quest timer field was renamed, so the quests of the current day are regenerated once.

## v0.4
- Minecraft-style pixel-art icons (drawn in code, no texture files): tabs (head, enchanted book, quest scroll, shield, cog),
  stat icons (sword, heart, feather, lapis, eye of ender) and the quest icon inside the pop-ups. Preview: icon-preview.png
- New CONFIG tab in the System window (Y):
  - DISPLAY (per client, saved to config/solosystem-client.json): quest pop-ups on/off, progress pop-ups on/off,
    side (left/right), height, how long they stay, animation speed (instant/fast/normal/smooth), stat bonus text, preview button, reset.
  - GAMEPLAY (whole world, saved to config/solosystem.json): EXP rate, passive-skill drop rate, chance of a mod-gear quest reward.
    Editable live in singleplayer / LAN host. On a dedicated server edit the file and restart.
