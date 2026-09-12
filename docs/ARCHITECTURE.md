# Architecture

Technical detail behind the README's one-paragraph summary - why this isn't just 9 real pots in one
block, and how each subsystem actually works.

## Why not 9 real pots in one block

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
([`CellContext`](../src/main/java/dev/mikey/botanygardens/block/context/CellContext.java)) instead of a
real block entity, and [`GardenCell.tick`](../src/main/java/dev/mikey/botanygardens/block/entity/GardenBlockEntity.java)
reimplements `BotanyPotBlockEntity#tickPot`'s body per cell.

## Resolving a cell's pot kind

A pot's *kind* (Basic/Hopper/Elite/Power/...) is resolved off whichever pot **block-item** a player
drops into a cell's pot-kind slot: `BotanyPotBlock#getGrowthModifier`/`getYieldModifier` are instance
methods that don't touch any real-world state, so they can be called directly on the registered
`Block` behind that item with zero placement in the world - see
[`GardenHelpers`](../src/main/java/dev/mikey/botanygardens/block/GardenHelpers.java). PowerPots is the
one exception (its tier lives on the block *entity*, not the block, and its own
`getGrowthModifier` always returns 0); see
[`compat/powerpots`](../src/main/java/dev/mikey/botanygardens/compat/powerpots), which reads the tier index
off the trailing digit of the pot-kind item's own registry path (`power_pot_<N>`) into
`Config.INSTANCE.TIERS`, exactly the technique `PowerPotBlock#appendHoverText` itself uses - not off
`BLOCK_ENTITY_DATA`, which only exists after a real placed `PowerPotBlockEntity` is broken and is
empty on a freshly crafted/creative item (confirmed by decompiling the actual jar; the NBT-only
approach silently resolved every fresh Power Pot item to `PotTier.ZERO`).

## Upgrades

Upgrades are a separate, always-available system, not PowerPots-specific: 4 shared slots
(Speed/Output/Energy/Fortune, see
[`GardenUpgradeItem`](../src/main/java/dev/mikey/botanygardens/upgrade/GardenUpgradeItem.java)/
[`GardenUpgradeSupport`](../src/main/java/dev/mikey/botanygardens/upgrade/GardenUpgradeSupport.java)) apply
to every cell regardless of pot kind; only Energy's cost reduction actually matters on a
Power-Pot-kind cell. Fortune stacks to 3 copies for a stacking bonus-roll chance (0.15/copy by
default, capped at 0.45), mirroring `PowerPotBlockEntity#getFortuneChance`; both numbers are
config-tunable (`GardenUpgradeType#getConfiguredModifier` reads them live, not baked in at item
registration).

## Deliberate departures from the parent mods

Two departures from what the decompiled parent mods actually do, both about not wasting FE/CPU on
cells that can't produce anything: a Power-Pot-kind cell only draws energy while it has both soil and
seed (real `PowerPotBlockEntity#tickPot` draws unconditionally, even empty); and every cell's whole
tick (soil/crop/FE) is skipped once the shared output bank is full, holding growth at "ready to
harvest" instead of completing the cycle and discarding it (real `BotanyPotBlockEntity#tickPot`
regrows-and-discards forever regardless of storage space) - checked once per garden-tick, not once per
cell, since all 9 cells share the one output bank.

## Client/server energy sync

The block entity's shared ticker runs unguarded on both sides (`GardenBlock#getTicker` registers the
same ticker for both), so `GardenBlockEntity#tickCell`'s energy draw is wrapped in an explicit
`!level.isClientSide` guard, matching what `PowerPotBlockEntity#tickPot` itself does. Missing that
guard once meant the client ran the same per-cell energy draw against its own *unsynced* copy of the
buffer every client tick, draining it to near-0 far faster than the periodic resync from the real
(healthy, cable-fed) server value could correct - so the GUI/Jade display spent most of its time
showing that self-inflicted near-empty number instead of the true one.
