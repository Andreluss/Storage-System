package cp2023.solution;

import cp2023.base.DeviceId;

public class UnlimitedDevice extends Device {
    public UnlimitedDevice(DeviceId deviceId) {
        super(Integer.MAX_VALUE, deviceId);
    }

    @Override
    public boolean hasFreeSlots() {
        return true;
    }

    @Override
    public void occupyNewSlot() {
        freeSlots = Integer.MAX_VALUE;
    }

    @Override
    public void releaseNewSlot() {
        freeSlots = Integer.MAX_VALUE;
    }
}
