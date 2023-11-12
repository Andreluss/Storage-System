package cp2023.solution;

public class Component {
    public boolean isTransferred() {
        return transferred;
    }

    public void setTransferred(boolean transferred) {
        this.transferred = transferred;
    }

    private boolean transferred = false;

    public Device getLocation() {
        return location;
    }

    public void setLocation(Device location) {
        this.location = location;
    }

    private Device location;

    public Component(Device location) {
        this.location = location;
    }
}
