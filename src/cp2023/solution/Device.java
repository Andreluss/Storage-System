package cp2023.solution;

import cp2023.base.DeviceId;

import java.util.*;

public class Device {
    public boolean hasFreeSlots() {
        return freeSlots > 0;
    }

    public void occupyNewSlot() {
        this.freeSlots -= 1;
    }

    public void releaseNewSlot() {
        this.freeSlots += 1;
    }

    public final LinkedList<TransferWrapper> waiting = new LinkedList<>();

    protected Integer freeSlots;
    private final DeviceId deviceId;

    public Device(Integer freeSlots, DeviceId deviceId) {
        this.freeSlots = freeSlots;
        this.deviceId = deviceId;
    }
}
