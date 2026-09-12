package dev.mtop.gardenpots.block.menu;

import dev.mtop.gardenpots.block.entity.GardenPotBlockEntity;
import net.minecraft.world.inventory.ContainerData;

/**
 * Syncs each cell's growth progress (0-100, or -1 if the player has disabled that cell) to the
 * client for {@link GardenPotScreen}'s progress bars - the server side reads live off the block
 * entity every tick the menu polls it (standard {@code ContainerData} behaviour, same as a
 * furnace's burn/cook time); the client side is just a plain int array
 * {@code AbstractContainerMenu#addDataSlots} fills in over the network.
 */
public class CellProgressData implements ContainerData {

    public static final int DISABLED = -1;

    private final GardenPotBlockEntity pot;

    public CellProgressData(GardenPotBlockEntity pot) {
        this.pot = pot;
    }

    @Override
    public int get(int index) {
        if (!this.pot.getCell(index).enabled) {
            return DISABLED;
        }
        return Math.round(this.pot.getGrowthProgress(index) * 100f);
    }

    @Override
    public void set(int index, int value) {
        // Server-authoritative; the client never writes back.
    }

    @Override
    public int getCount() {
        return GardenPotBlockEntity.GRID_SIZE;
    }
}
