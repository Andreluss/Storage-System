package cp2023.solution;

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



    protected Integer freeSlots;
    public Device(Integer freeSlots) {
        this.freeSlots = freeSlots;
    }
}
