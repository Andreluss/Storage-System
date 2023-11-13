package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;
import cp2023.exceptions.*;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
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

            deviceMap.put(deviceId, new Device(freeSlots, deviceId));
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

    protected Device creationDevice = new UnlimitedDevice(new DeviceId(-111));
    protected Device deletionDevice = new UnlimitedDevice(new DeviceId(-222));
    protected Map<DeviceId, Device> deviceMap = new HashMap<>();
    protected Map<ComponentId, Component> componentMap = new HashMap<>();
    protected Semaphore mutex = new Semaphore(1, true);

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

            // Set the state of this component to being transferred.
            transferWrapper.component.setTransferred(true);


            if (transferWrapper.destinationDevice.hasFreeSlots()) {
                // Update destination device
                transferWrapper.destinationDevice.occupyNewSlot();
                // and run this transfer without mutex.
                mutex.release();

                transfer.prepare();
                transfer.perform(); transferWrapper.markAsFinished();

                // Acquire mutex to update the state of sourceDevice and possible run another waiting transfer.
                mutex.acquire();
                releaseNextTransferIfWaitingAndUpdateTheDevice(transferWrapper); // (releases the mutex)
            }
            else {
                // There are 2 cases:
                // (1) there is a cycle, created by this transfer
                // (2) the transfer has to wait for prepare() and maybe for perform() too (if it's in a cycle).

                var cycle = findCycle(transferWrapper);
                if (cycle != null) { /* (1) there is a cycle, created by this transfer */
                    // Prepare and perform the whole cycle.
                    mutex.release();
                    transfer.prepare();

                    // Make a cyclePerformBarrier to run perform() on transfers only when all of them have already run prepare().
                    CyclicBarrier cyclePerformBarrier = new CyclicBarrier(cycle.size() + 1);

                    for (var otherTransferWrapper : cycle) {
                        otherTransferWrapper.setInCycle(true);
                        otherTransferWrapper.setCycleBarrier(cyclePerformBarrier);
                        otherTransferWrapper.waitPrepare.release();
                    }

                    // Wait until all transfers in cycle are prepared.
                    cyclePerformBarrier.await();

                    transfer.perform(); transferWrapper.markAsFinished();
                    // Other transfers in cycle will run their perform() as well, because of released cyclePerformBarrier.
                }
                else { /* (2) the transfer has to wait for prepare() */
                    // Add this transfer to destinationDevice's waiting list.
                    transferWrapper.destinationDevice.waiting.add(transferWrapper);
                    mutex.release();

                    transferWrapper.waitPrepare.acquire();
                    transfer.prepare();

                    if (transferWrapper.isInCycle()) {
                        assert transferWrapper.getCycleBarrier() != null;
                        transferWrapper.getCycleBarrier().await();
                    }

                    // transferWrapper.waitPerform.acquire();
                    transfer.perform(); transferWrapper.markAsFinished();
                    // if we shouldn't run perform() in mutex, than let's do markAsFinished() update just after the function call

                    if (!transferWrapper.isInCycle()) {
                        mutex.acquire();
                        // No other transfer has been scheduled to be performed just after this one,
                        // so we can run the longest-waiting one, if there is one.
                        releaseNextTransferIfWaitingAndUpdateTheDevice(transferWrapper);
                        mutex.release();
                    } // Otherwise do nothing, since there will be next transfer coming to this sourceDevice in a moment.
                }
            }


            // After all steps, the transfer was successful, and we can finally update
            // the component's state and location.
//            mutex.acquire();
//            transferWrapper.markAsFinished();
//            mutex.release();



        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
    }

    /**
     * Resumes the longest-waiting transfer from the sourceDevice of transferWrapper.
     * Should be run with mutex acquired; after this function mutex is released.
     * @param transferWrapper transferWrapper with sourceDevice, which may contain other transfers waiting
     */
    private void releaseNextTransferIfWaitingAndUpdateTheDevice(TransferWrapper transferWrapper) {
        if (!transferWrapper.sourceDevice.waiting.isEmpty()) {
            var nextTransferWrapper = transferWrapper.sourceDevice.waiting.removeFirst();
            mutex.release();
            nextTransferWrapper.waitPrepare.release();
        }
        else {
            transferWrapper.sourceDevice.releaseNewSlot();
            mutex.release();
        }
    }


    private List<TransferWrapper> findCycleDFS(Device currentDevice, Set<Device> visited, Device cycleNode) {
        visited.add(currentDevice);

        var iter = currentDevice.waiting.iterator();
        while (iter.hasNext()) {
            var transferWrapper = iter.next();

            var nextDevice = transferWrapper.sourceDevice;
            if (!visited.contains(nextDevice)) {
                var maybeCycle = findCycleDFS(nextDevice, visited, cycleNode);

                if (maybeCycle != null) {
                    // Add current transfer to the cycle.
                    maybeCycle.add(transferWrapper);
                    // Remove this transfer from waiting list on this device.
                    iter.remove();

                    return maybeCycle;
                }
            }
            else if (nextDevice == cycleNode) {
                // Start the cycle.
                var cycle = new ArrayList<TransferWrapper>();

                // Add current transfer to the cycle.
                cycle.add(transferWrapper);
                // Remove this transfer from waiting list on this device.
                iter.remove();

                return cycle;
            }
            else {
                // it's ok, we do nothing, we already visited this device with no effect,
                // we're here again probably because of multiple transfers from this device
//                System.out.println("WEIRD-ERROR the cycle should be only found when trying to visit cycleNode");
//                throw new UnexpectedException("The cycle should be only found when trying to visit cycleNode");
            }
        }
        return null;
    }

    private List<TransferWrapper> findCycle(TransferWrapper firstTransfer) {
        HashSet<Device> visited = new HashSet<>();
        visited.add(firstTransfer.destinationDevice);

        var cycle = findCycleDFS(firstTransfer.sourceDevice, visited, firstTransfer.destinationDevice);
        if (cycle != null) {
//            cycle.add(firstTransfer); this is the real beginning of the cycle
            // Reverse to get the right order,
            // with [the first transfer freed after firstTransfer] at the beginning.
            Collections.reverse(cycle);
        }

        return cycle;
    }

}
