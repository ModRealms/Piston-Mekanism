package mekanism.common.upgrade;

import java.util.List;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.inventory.slot.chemical.GasInventorySlot;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.core.HolderLookup;

public class AdvancedMachineUpgradeData extends MachineUpgradeData {

    public final IGasTank stored;
    public final GasInventorySlot gasSlot;
    public final long[] usedSoFar;

    //Advanced Machine Constructor
    public AdvancedMachineUpgradeData(HolderLookup.Provider provider, boolean redstone, RedstoneControl controlType, IEnergyContainer energyContainer, int operatingTicks, long usedSoFar, IGasTank stored,
          GasInventorySlot gasSlot, EnergyInventorySlot energySlot, InputInventorySlot inputSlot, OutputInventorySlot outputSlot, List<ITileComponent> components) {
        super(provider, redstone, controlType, energyContainer, operatingTicks, energySlot, inputSlot, outputSlot, components);
        this.stored = stored;
        this.gasSlot = gasSlot;
        this.usedSoFar = new long[]{usedSoFar};
    }

    //Advanced Machine Factory Constructor
    public AdvancedMachineUpgradeData(HolderLookup.Provider provider, boolean redstone, RedstoneControl controlType, IEnergyContainer energyContainer, int[] progress, long[] usedSoFar, IGasTank stored,
          GasInventorySlot gasSlot, EnergyInventorySlot energySlot, List<IInventorySlot> inputSlots, List<IInventorySlot> outputSlots, boolean sorting,
          List<ITileComponent> components) {
        super(provider, redstone, controlType, energyContainer, progress, energySlot, inputSlots, outputSlots, sorting, components);
        this.stored = stored;
        this.gasSlot = gasSlot;
        this.usedSoFar = usedSoFar;
    }
}