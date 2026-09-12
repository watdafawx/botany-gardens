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

## Architecture (why this isn't just 9 real pots in one block)

BotanyPots' whole growth/render/recipe pipeline is hard-wired to one real `BlockPos`/`BlockState` per
pot - confirmed by reading the actual source: `BlockEntityContext` is a `record(BotanyPotBlockEntity
pot, ...)` (a concrete class, not an interface param), the renderer's type param is the concrete BE,
and the base block entity pulls its `PotType` straight off `getBlockState().getBlock()`. Every
existing addon (Tiers, PowerPots, Dim-Botany-Pots) follows the same mold: one registered
`Block`/`BlockEntity` pair per pot. None of them embed pots inside pots.

What's genuinely reusable is the *data-driven* half of the API:
`net.darkhax.botanypots.common.api.context.BotanyPotContext` is a plain interface (not tied to a real
block entity), and `Soil`/`Crop` recipes are global datapack content, not tied to any specific pot
block. So each grid cell gets its own lightweight `BotanyPotContext` implementation
([`CellContext`](src/main/java/dev/mikey/botanygardens/block/context/CellContext.java)) instead of a real
block entity, and [`GardenCell.tick`](src/main/java/dev/mikey/botanygardens/block/entity/GardenBlockEntity.java)
reimplements `BotanyPotBlockEntity#tickPot`'s body per cell.

A pot's *kind* (Basic/Hopper/Elite/Power/...) is resolved off whichever pot **block-item** a player
drops into a cell's pot-kind slot: `BotanyPotBlock#getGrowthModifier`/`getYieldModifier` are instance
methods that don't touch any real-world state, so they can be called directly on the registered
`Block` behind that item with zero placement in the world - see
[`GardenHelpers`](src/main/java/dev/mikey/botanygardens/block/GardenHelpers.java). PowerPots is the
one exception (its tier lives on the block *entity*, not the block, and its own
`getGrowthModifier` always returns 0); see
[`compat/powerpots`](src/main/java/dev/mikey/botanygardens/compat/powerpots), which reads the tier index
off the trailing digit of the pot-kind item's own registry path (`power_pot_<N>`) into
`Config.INSTANCE.TIERS`, exactly the technique `PowerPotBlock#appendHoverText` itself uses - not off
`BLOCK_ENTITY_DATA`, which only exists after a real placed `PowerPotBlockEntity` is broken and is
empty on a freshly crafted/creative item (confirmed by decompiling the actual jar; the NBT-only
approach silently resolved every fresh Power Pot item to `PotTier.ZERO`).

Upgrades are a separate, always-available system, not PowerPots-specific: 4 shared slots
(Speed/Output/Energy/Fortune, see
[`GardenUpgradeItem`](src/main/java/dev/mikey/botanygardens/upgrade/GardenUpgradeItem.java)/
[`GardenUpgradeSupport`](src/main/java/dev/mikey/botanygardens/upgrade/GardenUpgradeSupport.java)) apply
to every cell regardless of pot kind; only Energy's cost reduction actually matters on a
Power-Pot-kind cell. Fortune stacks to 3 copies for a stacking bonus-roll chance (0.15/copy by
default, capped at 0.45), mirroring `PowerPotBlockEntity#getFortuneChance`; both numbers are
config-tunable (`GardenUpgradeType#getConfiguredModifier` reads them live, not baked in at item
registration).

Two departures from what the decompiled parent mods actually do, both about not wasting FE/CPU on
cells that can't produce anything: a Power-Pot-kind cell only draws energy while it has both soil and
seed (real `PowerPotBlockEntity#tickPot` draws unconditionally, even empty); and every cell's whole
tick (soil/crop/FE) is skipped once the shared output bank is full, holding growth at "ready to
harvest" instead of completing the cycle and discarding it (real `BotanyPotBlockEntity#tickPot`
regrows-and-discards forever regardless of storage space) - checked once per garden-tick, not once per
cell, since all 9 cells share the one output bank.

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
