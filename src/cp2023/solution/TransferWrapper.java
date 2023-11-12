package cp2023.solution;

import cp2023.base.ComponentTransfer;

public class TransferWrapper {
    protected Device sourceDevice;

    public Device getSourceDevice() {
        return sourceDevice;
    }

    public Device getDestinationDevice() {
        return destinationDevice;
    }

    public Component getComponent() {
        return component;
    }

    public TransferWrapper(Device sourceDevice, Device destinationDevice, Component component, ComponentTransfer transfer) {
        this.sourceDevice = sourceDevice;
        this.destinationDevice = destinationDevice;
        this.component = component;
        this.transfer = transfer;
    }

    protected Device destinationDevice;
    protected Component component;

    public ComponentTransfer getTransfer() {
        return transfer;
    }

    ComponentTransfer transfer;
}
