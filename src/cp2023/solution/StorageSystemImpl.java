package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;
import cp2023.exceptions.TransferException;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class StorageSystemImpl implements StorageSystem {
    public StorageSystemImpl(Map<DeviceId, Integer> deviceTotalSlots, Map<ComponentId, DeviceId> componentPlacement) throws IllegalArgumentException {
        // Validation:
        // - everything not null
        // - totalSlots > 0  ???? jednak nie >= 0
        // - componentPlacement deviceID -> *exists* in deviceTotalSlots
        // - each componentId in componentPlacement is *unique*
        //   (must be, because it's a Map key)
        // - deviceID capacity not exceeded

        deviceMap = new HashMap<>();

        for (var entry : deviceTotalSlots.entrySet()) {
            var deviceId = entry.getKey();
            var freeSlots = entry.getValue();

            if (!(deviceId != null && freeSlots != null))
                throw new IllegalArgumentException("DeviceID config cannot be null");
            if (!(freeSlots >= 1))
                throw new IllegalArgumentException("Device must have capacity >= 1");

            deviceMap.put(deviceId, new Device(freeSlots));
        }

        componentMap = new HashMap<>();

        for (var entry : componentPlacement.entrySet()) {
            var componentId = entry.getKey();
            var deviceId = entry.getValue();

            if (!(componentId != null && deviceId != null)) {
                throw new IllegalArgumentException("Component placement config cannot be null");
            }

            if (!deviceMap.containsKey(deviceId)) {
                throw new IllegalArgumentException(
                        "Component must be placed in an existing device (deviceId not found).");
            }


            // Get the device and check if it's not full.
            var device = deviceMap.get(deviceId);
            if (device.freeSlots == 0) {
                throw new IllegalArgumentException("Too many components assigned to device " + deviceId);
            }

            // Create new component object and assign it to device.
            var component = new Component(device);
            component.setLocation(device);
            device.freeSlots -= 1;

            // Add component object to map.
            componentMap.put(componentId, component);
        }
    }

    Map<DeviceId, Device> deviceMap;
    Map<ComponentId, Component> componentMap;
    Semaphore mutex = new Semaphore(1);


    /**
     * @param transfer new transfer to be executed
     * @throws TransferException if the transfer is not valid
     */
    @Override
    public void execute(ComponentTransfer transfer) throws TransferException {
        try {
            mutex.acquire();











        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
    }
}
