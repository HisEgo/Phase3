package model;

public class AntiTrojanSystem extends System {
    private static final double DETECTION_RADIUS = 100.0; // Detection radius in pixels
    private static final double DEACTIVATION_TIME = 2.0; // Deactivation time in seconds

    public AntiTrojanSystem() {
        super();
        setSystemType(SystemType.ANTI_TROJAN);
    }

    public AntiTrojanSystem(Point2D position) {
        super(position, SystemType.ANTI_TROJAN);
    }

    @Override
    public void processPacket(Packet packet) {
        processPacket(packet, null);
    }
    
    @Override
    public void processPacket(Packet packet, Port entryPort) {
        // Process normally first
        super.processPacket(packet, entryPort);

        // Check for trojan packets in detection radius
        if (!isDeactivated()) {
            detectAndConvertTrojans();
        }
    }

    public void detectAndConvertTrojans() {
        // Access packets through parent level
        GameLevel level = getParentLevel();
        if (level == null) return;

        boolean convertedAny = false;
        for (WireConnection connection : level.getWireConnections()) {
            for (Packet packet : connection.getPacketsOnWire()) {
                if (!packet.isActive()) continue;
                if (!isWithinDetectionRadius(packet)) continue;
                if (packet.getPacketType() != null && packet.getPacketType().isTrojan()) {
                    convertTrojanPacket(packet);
                    convertedAny = true;
                }
            }
        }
        // Also scan packets currently held in systems (ports and storage)
        for (System system : level.getSystems()) {
            for (Port port : system.getInputPorts()) {
                Packet p = port.getCurrentPacket();
                if (p != null && p.isActive() && isWithinDetectionRadius(p) && p.getPacketType() != null && p.getPacketType().isTrojan()) {
                    convertTrojanPacket(p);
                    convertedAny = true;
                }
            }
            for (Port port : system.getOutputPorts()) {
                Packet p = port.getCurrentPacket();
                if (p != null && p.isActive() && isWithinDetectionRadius(p) && p.getPacketType() != null && p.getPacketType().isTrojan()) {
                    convertTrojanPacket(p);
                    convertedAny = true;
                }
            }
            for (Packet p : system.getStorage()) {
                if (p != null && p.isActive() && isWithinDetectionRadius(p) && p.getPacketType() != null && p.getPacketType().isTrojan()) {
                    convertTrojanPacket(p);
                    convertedAny = true;
                }
            }
        }

        if (convertedAny) {
            deactivate(DEACTIVATION_TIME);
            java.lang.System.out.println("AntiTrojanSystem: Converted trojan(s) and deactivated for " + DEACTIVATION_TIME + "s");
        }
    }

    public boolean isWithinDetectionRadius(Packet packet) {
        double distance = getPosition().distanceTo(packet.getCurrentPosition());
        return distance <= DETECTION_RADIUS;
    }

    public void convertTrojanPacket(Packet packet) {
        if (packet.getPacketType() != null && packet.getPacketType().isTrojan()) {
            packet.convertFromTrojan();
        }
    }

    public double getDetectionRadius() {
        return DETECTION_RADIUS;
    }
}

