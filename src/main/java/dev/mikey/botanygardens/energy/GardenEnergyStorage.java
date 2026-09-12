package dev.mikey.botanygardens.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Same shape as PowerPots' own {@code ModEnergyStorage} - vanilla {@link EnergyStorage} plus direct
 * mutators for our own tick logic to drain from (removeEnergy) without going through the
 * receive-energy capability path.
 */
public class GardenEnergyStorage extends EnergyStorage {

    public GardenEnergyStorage(int capacity, int maxReceive) {
        super(capacity, maxReceive, 0);
    }

    public void removeEnergy(int amount) {
        this.energy = Math.max(0, this.energy - amount);
    }
}
