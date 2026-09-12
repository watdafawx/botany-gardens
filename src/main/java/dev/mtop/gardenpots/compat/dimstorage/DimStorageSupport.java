package dev.mtop.gardenpots.compat.dimstorage;

import edivad.dimstorage.api.Frequency;
import edivad.dimstorage.items.DimTablet;
import edivad.dimstorage.items.ItemDimBase;
import edivad.dimstorage.items.components.DimStorageComponents;
import edivad.dimstorage.items.components.FrequencyTabletComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Frequency-item recognition, mirroring {@code Dim-Botany-Pots}' own {@code DimBotanyPotHelper} -
 * reimplemented here (rather than depending on that addon's jar) since we only need the base
 * {@code dimstorage} mod, not Dim-Botany-Pots itself.
 *
 * <p>Every call site into this class is guarded by {@code GardenPots.isDimStorageLoaded()}, so this
 * class - and the {@code edivad.dimstorage} types it imports - is only ever classloaded when
 * DimStorage is actually present.
 */
public class DimStorageSupport {

    public static boolean isValidFrequencyItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof DimTablet) {
            final FrequencyTabletComponent comp = stack.get(DimStorageComponents.FREQUENCY_TABLET);
            return comp != null && comp.bound();
        }
        return stack.getItem() instanceof ItemDimBase && stack.has(DimStorageComponents.FREQUENCY);
    }

    public static Frequency getFrequency(ItemStack stack) {
        if (!isValidFrequencyItem(stack)) {
            return new Frequency();
        }
        if (stack.getItem() instanceof DimTablet) {
            final FrequencyTabletComponent comp = stack.get(DimStorageComponents.FREQUENCY_TABLET);
            return comp != null ? comp.frequency() : new Frequency();
        }
        final Frequency freq = stack.get(DimStorageComponents.FREQUENCY);
        return freq != null ? freq : new Frequency();
    }
}
