package cp2023.solution;

public class Component {
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
