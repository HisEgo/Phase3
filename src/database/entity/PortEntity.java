package database.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "ports")
public class PortEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "port_id")
    private Long portId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    private SystemEntity system;

    @Column(name = "port_name", length = 100, nullable = false)
    private String portName;

    @Column(name = "port_type", length = 50, nullable = false)
    private String portType;

    @Column(name = "port_shape", length = 20, nullable = false)
    private String portShape;

    @Column(name = "position_x")
    private double positionX;

    @Column(name = "position_y")
    private double positionY;

    @Column(name = "is_input")
    private boolean isInput;

    @Column(name = "is_output")
    private boolean isOutput;

    @Column(name = "is_connected")
    private boolean isConnected;

    @Column(name = "max_connections")
    private int maxConnections;

    @Column(name = "current_connections")
    private int currentConnections;

    @Column(name = "bandwidth_capacity")
    private double bandwidthCapacity;

    @Column(name = "current_bandwidth_usage")
    private double currentBandwidthUsage;

    @OneToMany(mappedBy = "sourcePort", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WireConnectionEntity> outgoingConnections = new ArrayList<>();

    @OneToMany(mappedBy = "targetPort", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WireConnectionEntity> incomingConnections = new ArrayList<>();

    // Constructors
    public PortEntity() {
        this.isInput = false;
        this.isOutput = false;
        this.isConnected = false;
        this.maxConnections = 1;
        this.currentConnections = 0;
        this.bandwidthCapacity = 1.0;
        this.currentBandwidthUsage = 0.0;
    }

    public PortEntity(String portName, String portType, String portShape, double positionX, double positionY) {
        this();
        this.portName = portName;
        this.portType = portType;
        this.portShape = portShape;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    // Getters and Setters
    public Long getPortId() {
        return portId;
    }

    public void setPortId(Long portId) {
        this.portId = portId;
    }

    public SystemEntity getSystem() {
        return system;
    }

    public void setSystem(SystemEntity system) {
        this.system = system;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public String getPortType() {
        return portType;
    }

    public void setPortType(String portType) {
        this.portType = portType;
    }

    public String getPortShape() {
        return portShape;
    }

    public void setPortShape(String portShape) {
        this.portShape = portShape;
    }

    public double getPositionX() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
    }

    public boolean isOutput() {
        return isOutput;
    }

    public void setOutput(boolean output) {
        isOutput = output;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getCurrentConnections() {
        return currentConnections;
    }

    public void setCurrentConnections(int currentConnections) {
        this.currentConnections = currentConnections;
    }

    public double getBandwidthCapacity() {
        return bandwidthCapacity;
    }

    public void setBandwidthCapacity(double bandwidthCapacity) {
        this.bandwidthCapacity = bandwidthCapacity;
    }

    public double getCurrentBandwidthUsage() {
        return currentBandwidthUsage;
    }

    public void setCurrentBandwidthUsage(double currentBandwidthUsage) {
        this.currentBandwidthUsage = currentBandwidthUsage;
    }

    public List<WireConnectionEntity> getOutgoingConnections() {
        return outgoingConnections;
    }

    public void setOutgoingConnections(List<WireConnectionEntity> outgoingConnections) {
        this.outgoingConnections = outgoingConnections;
    }

    public List<WireConnectionEntity> getIncomingConnections() {
        return incomingConnections;
    }

    public void setIncomingConnections(List<WireConnectionEntity> incomingConnections) {
        this.incomingConnections = incomingConnections;
    }

    // Helper methods
    public void addOutgoingConnection(WireConnectionEntity connection) {
        outgoingConnections.add(connection);
        connection.setSourcePort(this);
        updateConnectionStatus();
    }

    public void addIncomingConnection(WireConnectionEntity connection) {
        incomingConnections.add(connection);
        connection.setTargetPort(this);
        updateConnectionStatus();
    }

    public void removeOutgoingConnection(WireConnectionEntity connection) {
        outgoingConnections.remove(connection);
        updateConnectionStatus();
    }

    public void removeIncomingConnection(WireConnectionEntity connection) {
        incomingConnections.remove(connection);
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        currentConnections = outgoingConnections.size() + incomingConnections.size();
        isConnected = currentConnections > 0;
    }

    public boolean canConnect() {
        return currentConnections < maxConnections;
    }

    public boolean isCompatibleWith(PortEntity otherPort) {
        if (otherPort == null || this.equals(otherPort)) {
            return false;
        }

        // Check if ports are on different systems
        if (system != null && otherPort.getSystem() != null &&
                system.equals(otherPort.getSystem())) {
            return false;
        }

        // Check port type compatibility
        if (portType != null && otherPort.getPortType() != null) {
            if (!portType.equals(otherPort.getPortType())) {
                return false;
            }
        }

        // Check port shape compatibility
        if (portShape != null && otherPort.getPortShape() != null) {
            if (!portShape.equals(otherPort.getPortShape())) {
                return false;
            }
        }

        // Check if both ports can accept connections
        return canConnect() && otherPort.canConnect();
    }

    public double getBandwidthUsagePercentage() {
        if (bandwidthCapacity == 0) {
            return 0.0;
        }
        return (currentBandwidthUsage / bandwidthCapacity) * 100.0;
    }

    public boolean isOverloaded() {
        return getBandwidthUsagePercentage() > 90.0;
    }

    public List<WireConnectionEntity> getAllConnections() {
        List<WireConnectionEntity> allConnections = new ArrayList<>();
        allConnections.addAll(outgoingConnections);
        allConnections.addAll(incomingConnections);
        return allConnections;
    }

    @Override
    public String toString() {
        return "PortEntity{" +
                "portId=" + portId +
                ", portName='" + portName + '\'' +
                ", portType='" + portType + '\'' +
                ", portShape='" + portShape + '\'' +
                ", positionX=" + positionX +
                ", positionY=" + positionY +
                ", isInput=" + isInput +
                ", isOutput=" + isOutput +
                ", isConnected=" + isConnected +
                ", currentConnections=" + currentConnections +
                '}';
    }
}


