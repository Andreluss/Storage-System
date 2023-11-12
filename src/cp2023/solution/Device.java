package cp2023.solution;

public class Device {
    public int getFreeSlots() {
        return freeSlots;
    }

    public Integer freeSlots;
    public Device(Integer freeSlots) {
        this.freeSlots = freeSlots;
    }
}
