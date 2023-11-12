package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;
import cp2023.exceptions.*;

import javax.swing.plaf.basic.ComboPopup;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class StorageSystemImpl implements StorageSystem {
    public StorageSystemImpl(Map<DeviceId, Integer> deviceTotalSlots, Map<ComponentId, DeviceId> componentPlacement) throws IllegalArgumentException {
        // Validation:
        // - everything not null
        // - totalSlots > 0
        // - componentPlacement deviceID -> *exists* in deviceTotalSlots
        // - each componentId in componentPlacement is *unique*
        //   (must be, because it's a Map key)
        // - deviceID capacity not exceeded


        for (var entry : deviceTotalSlots.entrySet()) {
            var deviceId = entry.getKey();
            var freeSlots = entry.getValue();

            if (!(deviceId != null && freeSlots != null))
                throw new IllegalArgumentException("DeviceID config cannot be null");
            if (!(freeSlots >= 1))
                throw new IllegalArgumentException("Device must have capacity >= 1");

            deviceMap.put(deviceId, new Device(freeSlots));
        }


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
            if (!device.hasFreeSlots()) {
                throw new IllegalArgumentException("Too many components assigned to device " + deviceId);
            }

            // Create new component object and assign it to device.
            var component = new Component(device);
            component.setLocation(device);
            device.occupyNewSlot();

            // Add component object to map.
            componentMap.put(componentId, component);
        }
    }

    protected Device creationDevice = new UnlimitedDevice();
    protected Device deletionDevice = new UnlimitedDevice();
    protected Map<DeviceId, Device> deviceMap = new HashMap<>();
    protected Map<ComponentId, Component> componentMap = new HashMap<>();
    protected Semaphore mutex = new Semaphore(1);

    private Device getSourceDevice (DeviceId sourceDeviceId) throws DeviceDoesNotExist {
        return getDevice(sourceDeviceId, creationDevice);
    }

    private Device getDestinationDevice (DeviceId destinationDeviceId) throws DeviceDoesNotExist {
        return getDevice(destinationDeviceId, deletionDevice);
    }

    private Device getDevice(DeviceId deviceId, Device alternativeDevice) throws DeviceDoesNotExist {
        Device device;
        if (deviceId != null) { // Device should exist in the system.
            device = deviceMap.get(deviceId);
            if (device == null) {
                throw new DeviceDoesNotExist(deviceId);
            }
        }
        else {
            device = alternativeDevice;
        }
        return device;
    }

    protected TransferWrapper createTransferWrapper(ComponentTransfer transfer) throws TransferException {
        var sourceDeviceId = transfer.getSourceDeviceId();
        var destinationDeviceId = transfer.getDestinationDeviceId();
        var componentId = transfer.getComponentId();

        // Check illegal transfer (null -> null).
        if (sourceDeviceId == null && destinationDeviceId == null) {
            throw new IllegalTransferType(componentId);
        }

        // Set the source device (possibly to artificial 'creation' device - for 'add' type transfers).
        Device sourceDevice = getSourceDevice(sourceDeviceId);

        // Set the destination device.
        Device destinationDevice = getDestinationDevice(destinationDeviceId);

        // Set the component and check for other errors.
        Component component;
        if (sourceDeviceId == null) {
            // Add component.

            // Component should be new.
            if (componentMap.containsKey(componentId)) {
                throw new ComponentAlreadyExists(componentId);
            }

            component = new Component(sourceDevice);
        }
        else {
            // Move or delete component.

            // Component should already exist.
            if (!componentMap.containsKey(componentId)) {
                throw new ComponentDoesNotExist(componentId, sourceDeviceId);
            }
            component = componentMap.get(componentId);

            // Component should not be transferred at the moment.
            if (component.isTransferred()) {
                throw new ComponentIsBeingOperatedOn(componentId);
            }

            // Component should be on the source device.
            if (component.getLocation() != sourceDevice) {
                throw new ComponentDoesNotExist(componentId, sourceDeviceId);
            }

            if (destinationDevice != null) {
                // Move component.

                // Component should not be on the destination device.
                if (component.getLocation() == destinationDevice) {
                    throw new ComponentDoesNotNeedTransfer(componentId, destinationDeviceId);
                }
            }
        }

        // Reaching this point means we have a valid transfer.
        return new TransferWrapper(sourceDevice, destinationDevice, component, transfer);
    }

    /**
     * @param transfer new transfer to be executed
     * @throws TransferException if the transfer is not valid
     */
    @Override
    public void execute(ComponentTransfer transfer) throws TransferException {
        try {
            mutex.acquire();

            TransferWrapper transferWrapper;
            try {
                transferWrapper = createTransferWrapper(transfer);
            }
            catch (TransferException e) {
                // E.g. if the transfer was wrong, then release the mutex anyway.
                mutex.release();
                throw e;
            }


            mutex.release();






        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
    }
}
