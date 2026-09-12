package dev.mikey.botanygardens.block.menu;

import dev.mikey.botanygardens.block.entity.GardenBlockEntity;
import net.minecraft.world.inventory.ContainerData;

/**
 * Syncs each cell's growth progress (0-100, or -1 if the player has disabled that cell) to the
 * client for {@link GardenScreen}'s progress bars - the server side reads live off the block
 * entity every tick the menu polls it (standard {@code ContainerData} behaviour, same as a
 * furnace's burn/cook time); the client side is just a plain int array
 * {@code AbstractContainerMenu#addDataSlots} fills in over the network.
 */
public class CellProgressData implements ContainerData {

    public static final int DISABLED = -1;

    private final GardenBlockEntity pot;

    public CellProgressData(GardenBlockEntity pot) {
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
        return GardenBlockEntity.GRID_SIZE;
    }
}
