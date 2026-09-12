# Botany Gardens

NeoForge 1.21.1 addon for [BotanyPots](https://github.com/Darkhax-Minecraft/BotanyPots). Adds a
**Garden Pot**: one block housing a 3x3 grid of virtual pots, so 9 crops fit in the footprint of 1
block instead of 9. Each cell accepts any pot-kind item the player owns - vanilla
Basic/Hopper Pot, [Botany Pots Tiers](https://github.com/starforcraft/Botany-Pots-Tiers)'
Elite/Ultra/Mega, or [PowerPots](https://github.com/Absenteewings90/PowerPots)' Power Pot - and
grows/renders/harvests exactly like the real thing. The block exposes one FE port that powers any
Power-Pot-kind cells. A second block, **Dimensional Garden Pot**, redirects every hopper-kind cell's
harvest straight into a bound [Dim Storage](https://modrinth.com/mod/dimstorage) frequency instead of
its own output slots, mirroring
[Dim-Botany-Pots](https://github.com/Wxrlds/Dim-Botany-Pots).

Requires BotanyPots + Bookshelf. Botany Pots Tiers, PowerPots, Dim Storage, and Jade are all optional -
a cell can only be set to a pot-kind from a mod that's actually installed, and Jade (if present) shows
a hover tooltip with growing-cell count, each cell's seed name/progress, and energy (buffer + FE/t
currently being drawn). The GUI shows the same energy/FE-per-tick pair under the title.

Balance knobs (energy capacity/max-receive/sync-period, export period, all 4 upgrade modifiers, Fortune's
cap) live in `config/botanygardens-common.toml`, generated on first run - see
[`ModConfig`](src/main/java/dev/mikey/botanygardens/config/ModConfig.java).

## Architecture

Short version: BotanyPots expects one real block per pot, so this mod can't just place 9 tiny pots
inside one block - instead each of the 3x3 grid's cells is a lightweight stand-in that reuses
BotanyPots' recipe/growth rules without needing a real block of its own. Pot *kind* (Basic, Hopper,
Power Pot, ...) is read straight off whatever pot item is sitting in that cell. Upgrades (Speed,
Output, Energy, Fortune) are the mod's own system, shared across every cell regardless of kind. A
couple of spots also deliberately behave better than the mods being built on - e.g. a cell stops
drawing power the moment there's nothing left for it to grow, rather than burning it for nothing.

Full technical writeup - source-level detail, decompiled proof, and the exact reasoning behind each
design choice - lives in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Building

```
build.bat build          # or gradlew build if you have your own JDK 21
```

`build.bat` looks for a JDK in `toolchain\`, falling back to the sibling `foxs-tweaks` project's
toolchain if this one doesn't have its own.

## Status

Done: core growth/harvest loop, energy port + PowerPots tier compat, Botany Pots Tiers compat
(works for free - see Architecture), the grid GUI (per-cell enable/disable toggle with check/X mark,
growth progress bars, a vertical upgrade-slot column with its own stat readout, FE/FE-per-tick/Dim-
frequency status lines), the in-world renderer (real pot shape/model, growing cells clustered as mini
copies of whatever pot-kind item was inserted), the Dimensional Garden Pot variant (own texture, own
renderer registration, frequency binding via right-click), Jade integration (cell seed names/progress,
energy + FE/t), loot tables, crafting recipes, the mod's own 4-item global upgrade system
(Speed/Output/Energy/Fortune, generated icons), and a common config for every tunable balance number.
Playtested extensively in the dev client, not just compiled - including hunting down a real
client-vs-server desync bug where the block entity's shared ticker ran unguarded on both sides and
the client kept re-draining its own unsynced copy of the energy buffer (see `GardenBlockEntity#tickCell`'s
`!level.isClientSide` guard, added to match what `PowerPotBlockEntity#tickPot` itself does).

Not done yet, worth doing before a real release:
- **Art.** Block textures are a reused BotanyPots/DimStorage material pair; the GUI background is
  flat-filled rectangles with a bevel, not a real texture. Both work, neither is final art.
- No JEI/REI integration for the recipe side (BotanyPots itself has a JEI plugin for its own
  recipes; this mod's grid GUI doesn't add anything on top of that).
- A save from before the upgrade-item rework could still have PowerPots' own upgrade items sitting
  in a Garden Pot's upgrade slots (no longer recognized by `GardenUpgradeSupport.isUpgradeForSlot`,
  so they'd just sit inert) - harmless on a dev-only save, but worth a one-time cleanup/migration
  note before a real release.
