package cp2023.solution;

import cp2023.base.ComponentTransfer;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class TransferWrapper {
    /**
     * @return true, when this transfer will be performed in a cycle, and it shouldn't release space in its device and run another transfer.
     */
    public boolean isInCycle() {
        return inCycle;
    }

    public void setInCycle(boolean inCycle) {
        this.inCycle = inCycle;
    }

    private boolean inCycle = false;
    public Semaphore waitPrepare = new Semaphore(0);

    public TransferWrapper(Device sourceDevice, Device destinationDevice, Component component, ComponentTransfer transfer) {
        this.sourceDevice = sourceDevice;
        this.destinationDevice = destinationDevice;
        this.component = component;
        this.transfer = transfer;
    }

    public final Device sourceDevice;
    public final Device destinationDevice;
    public final Component component;
    public final ComponentTransfer transfer;

    private CyclicBarrier cycleBarrier;
    public void setCycleBarrier(CyclicBarrier cycleBarrier) {
        this.cycleBarrier = cycleBarrier;
    }
    public CyclicBarrier getCycleBarrier() {
        return cycleBarrier;
    }

    public void markAsFinished() {
        component.setTransferred(false);
        component.setLocation(destinationDevice);
    }
}
