package cp2023.solution;

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
    public Device(Integer freeSlots) {
        this.freeSlots = freeSlots;
    }
}
