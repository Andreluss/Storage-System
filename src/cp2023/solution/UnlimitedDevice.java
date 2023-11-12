package cp2023.solution;

public class UnlimitedDevice extends Device {
    public UnlimitedDevice() {
        super(Integer.MAX_VALUE);
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
