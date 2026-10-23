# Solo System (Fabric 26.3) - v0.2

Upload everything in this folder to a GitHub repo -> Actions tab -> "build" -> Artifacts -> download.
Use build/libs/solo-system-0.2.0.jar (NOT the -sources jar). Press Y in game.

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
