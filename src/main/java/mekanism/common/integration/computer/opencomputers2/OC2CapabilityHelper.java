package mekanism.common.integration.computer.opencomputers2;

import li.cil.oc2.api.bus.device.Device;
import mekanism.common.capabilities.resolver.BasicCapabilityResolver;
import mekanism.common.capabilities.resolver.ICapabilityResolver;
import mekanism.common.integration.computer.IComputerTile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;

public class OC2CapabilityHelper {

    private static final Capability<Device> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    public static <TILE extends BlockEntity & IComputerTile> ICapabilityResolver getOpenComputers2Capability(TILE tile) {
        throw new IllegalStateException("OC not porter");//TODO - 1.20.2: when OC2 updates
        /*if (tile.isComputerCapabilityPersistent()) {
            return BasicCapabilityResolver.persistent(CAPABILITY, () -> MekanismDevice.create(tile));
        }
        return BasicCapabilityResolver.create(CAPABILITY, () -> MekanismDevice.create(tile));*/
    }
}