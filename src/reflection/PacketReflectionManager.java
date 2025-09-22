package reflection;

import model.Packet;
import model.PacketType;
import model.Point2D;
import model.Vec2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class PacketReflectionManager {
    private static final String PACKET_PACKAGE = "com.networksimulation.model";
    private static final String PACKET_SUFFIX = "Packet";

    private Map<Class<? extends Packet>, PacketType> packetTypeMap;
    private Map<PacketType, Integer> packetCooldowns;
    private Map<PacketType, Integer> packetQuantities;
    private Random random;

    public PacketReflectionManager() {
        this.packetTypeMap = new ConcurrentHashMap<>();
        this.packetCooldowns = new ConcurrentHashMap<>();
        this.packetQuantities = new ConcurrentHashMap<>();
        this.random = new Random();

        // Discover packet types using reflection
        discoverPacketTypes();
        initializePacketCooldowns();
        initializePacketQuantities();
    }

    private void discoverPacketTypes() {
        try {
            // Get the package
            Package packetPackage = Package.getPackage(PACKET_PACKAGE);
            if (packetPackage == null) {
                System.err.println("Could not find package: " + PACKET_PACKAGE);
                return;
            }

            // Get all classes in the package
            ClassLoader classLoader = getClass().getClassLoader();
            String packagePath = PACKET_PACKAGE.replace('.', '/');

            // This is a simplified approach - in a real implementation, you might use
            // a library like Reflections or scan the classpath more thoroughly
            scanForPacketClasses();

        } catch (Exception e) {
            System.err.println("Error discovering packet types: " + e.getMessage());
        }
    }

    private void scanForPacketClasses() {
        // Define known packet types and their mappings
        Map<String, PacketType> knownPackets = new HashMap<>();
        knownPackets.put("ConfidentialPacket", PacketType.CONFIDENTIAL);
        knownPackets.put("ProtectedPacket", PacketType.PROTECTED);
        knownPackets.put("TrojanPacket", PacketType.TROJAN);
        knownPackets.put("BulkPacket", PacketType.BULK_SMALL);
        knownPackets.put("BitPacket", PacketType.BIT_PACKET);
        knownPackets.put("MessengerPacket", PacketType.SMALL_MESSENGER);
        knownPackets.put("TrianglePacket", PacketType.TRIANGLE_MESSENGER);
        knownPackets.put("SquarePacket", PacketType.SQUARE_MESSENGER);

        // Try to instantiate each packet type using reflection
        for (Map.Entry<String, PacketType> entry : knownPackets.entrySet()) {
            String className = entry.getKey();
            PacketType packetType = entry.getValue();

            try {
                Class<?> packetClass = Class.forName(PACKET_PACKAGE + "." + className);

                // Check if it's a concrete class that extends Packet
                if (Packet.class.isAssignableFrom(packetClass) &&
                        !Modifier.isAbstract(packetClass.getModifiers())) {

                    // Try to find a suitable constructor
                    Constructor<?>[] constructors = packetClass.getConstructors();
                    if (constructors.length > 0) {
                        packetTypeMap.put((Class<? extends Packet>) packetClass, packetType);
                        System.out.println("Discovered packet type: " + className + " -> " + packetType);
                    }
                }
            } catch (ClassNotFoundException e) {
                // Packet class not found, skip it
            } catch (Exception e) {
                System.err.println("Error processing packet class " + className + ": " + e.getMessage());
            }
        }
    }

    private void initializePacketCooldowns() {
        for (PacketType packetType : PacketType.values()) {
            // Set different cooldowns based on packet type
            switch (packetType) {
                case SMALL_MESSENGER:
                case SQUARE_MESSENGER:
                case TRIANGLE_MESSENGER:
                    packetCooldowns.put(packetType, 5); // 5 seconds
                    break;
                case CONFIDENTIAL:
                    packetCooldowns.put(packetType, 15); // 15 seconds
                    break;
                case PROTECTED:
                    packetCooldowns.put(packetType, 10); // 10 seconds
                    break;
                case TROJAN:
                    packetCooldowns.put(packetType, 20); // 20 seconds
                    break;
                case BULK_SMALL:
                case BULK_LARGE:
                    packetCooldowns.put(packetType, 30); // 30 seconds
                    break;
                case BIT_PACKET:
                    packetCooldowns.put(packetType, 8); // 8 seconds
                    break;
                default:
                    packetCooldowns.put(packetType, 10); // Default 10 seconds
                    break;
            }
        }
    }

    private void initializePacketQuantities() {
        for (PacketType packetType : PacketType.values()) {
            // Set different quantities based on packet type
            switch (packetType) {
                case SMALL_MESSENGER:
                case SQUARE_MESSENGER:
                case TRIANGLE_MESSENGER:
                    packetQuantities.put(packetType, 50); // 50 packets
                    break;
                case CONFIDENTIAL:
                    packetQuantities.put(packetType, 10); // 10 packets
                    break;
                case PROTECTED:
                    packetQuantities.put(packetType, 20); // 20 packets
                    break;
                case TROJAN:
                    packetQuantities.put(packetType, 15); // 15 packets
                    break;
                case BULK_SMALL:
                case BULK_LARGE:
                    packetQuantities.put(packetType, 5); // 5 packets
                    break;
                case BIT_PACKET:
                    packetQuantities.put(packetType, 25); // 25 packets
                    break;
                default:
                    packetQuantities.put(packetType, 25); // Default 25 packets
                    break;
            }
        }
    }

    public Packet createPacket(PacketType packetType, Point2D position, Vec2D movementVector) {
        try {
            // Find the class for this packet type
            Class<? extends Packet> packetClass = getPacketClass(packetType);
            if (packetClass == null) {
                System.err.println("No class found for packet type: " + packetType);
                return null;
            }

            // Try to find a suitable constructor
            Constructor<?>[] constructors = packetClass.getConstructors();
            for (Constructor<?> constructor : constructors) {
                try {
                    if (constructor.getParameterCount() == 2) {
                        // Try constructor with position and movement vector
                        return (Packet) constructor.newInstance(position, movementVector);
                    } else if (constructor.getParameterCount() == 0) {
                        // Try default constructor and set properties
                        Packet packet = (Packet) constructor.newInstance();
                        // Use reflection to set properties if they exist
                        setPacketProperty(packet, "currentPosition", position);
                        setPacketProperty(packet, "movementVector", movementVector);
                        return packet;
                    }
                } catch (Exception e) {
                    // Try next constructor
                    continue;
                }
            }

            System.err.println("No suitable constructor found for packet type: " + packetType);
            return null;

        } catch (Exception e) {
            System.err.println("Error creating packet of type " + packetType + ": " + e.getMessage());
            return null;
        }
    }

    private Class<? extends Packet> getPacketClass(PacketType packetType) {
        for (Map.Entry<Class<? extends Packet>, PacketType> entry : packetTypeMap.entrySet()) {
            if (entry.getValue() == packetType) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void setPacketProperty(Packet packet, String propertyName, Object value) {
        try {
            java.lang.reflect.Field field = packet.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            field.set(packet, value);
        } catch (Exception e) {
            // Property doesn't exist or can't be set, ignore
        }
    }


    public List<Packet> generatePacketWave(int waveSize, Point2D sourcePosition) {
        List<Packet> wave = new ArrayList<>();

        // Create a hierarchy of packets based on wave size
        List<PacketType> availableTypes = getAvailablePacketTypes();

        for (int i = 0; i < waveSize; i++) {
            // Avoid difficult packets at the beginning
            PacketType selectedType = selectPacketTypeForWave(i, waveSize, availableTypes);

            if (selectedType != null && hasPacketAvailable(selectedType)) {
                // Generate random movement vector
                double angle = random.nextDouble() * 2 * Math.PI;
                double speed = 50.0 + random.nextDouble() * 100.0; // 50-150 pixels/second
                Vec2D movementVector = new Vec2D(
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed
                );

                Packet packet = createPacket(selectedType, sourcePosition, movementVector);
                if (packet != null) {
                    wave.add(packet);
                    consumePacket(selectedType);
                }
            }
        }

        return wave;
    }

    private PacketType selectPacketTypeForWave(int position, int totalSize, List<PacketType> availableTypes) {
        // Avoid difficult packets at the beginning
        if (position < totalSize * 0.3) { // First 30% of wave
            return selectEasyPacketType(availableTypes);
        } else if (position < totalSize * 0.7) { // Middle 40% of wave
            return selectMediumPacketType(availableTypes);
        } else { // Last 30% of wave
            return selectHardPacketType(availableTypes);
        }
    }

    private PacketType selectEasyPacketType(List<PacketType> availableTypes) {
        List<PacketType> easyTypes = Arrays.asList(
                PacketType.SMALL_MESSENGER, PacketType.TRIANGLE_MESSENGER, PacketType.SQUARE_MESSENGER
        );

        for (PacketType type : easyTypes) {
            if (availableTypes.contains(type) && hasPacketAvailable(type)) {
                return type;
            }
        }

        return availableTypes.isEmpty() ? null : availableTypes.get(0);
    }

    private PacketType selectMediumPacketType(List<PacketType> availableTypes) {
        List<PacketType> mediumTypes = Arrays.asList(
                PacketType.PROTECTED, PacketType.SQUARE_MESSENGER, PacketType.BIT_PACKET
        );

        for (PacketType type : mediumTypes) {
            if (availableTypes.contains(type) && hasPacketAvailable(type)) {
                return type;
            }
        }

        return selectEasyPacketType(availableTypes);
    }

    private PacketType selectHardPacketType(List<PacketType> availableTypes) {
        List<PacketType> hardTypes = Arrays.asList(
                PacketType.CONFIDENTIAL, PacketType.TROJAN, PacketType.BULK_SMALL, PacketType.BULK_LARGE
        );

        for (PacketType type : hardTypes) {
            if (availableTypes.contains(type) && hasPacketAvailable(type)) {
                return type;
            }
        }

        return selectMediumPacketType(availableTypes);
    }

    private List<PacketType> getAvailablePacketTypes() {
        List<PacketType> available = new ArrayList<>();
        for (Map.Entry<PacketType, Integer> entry : packetQuantities.entrySet()) {
            if (entry.getValue() > 0) {
                available.add(entry.getKey());
            }
        }
        return available;
    }

    private boolean hasPacketAvailable(PacketType packetType) {
        Integer quantity = packetQuantities.get(packetType);
        return quantity != null && quantity > 0;
    }

    private void consumePacket(PacketType packetType) {
        Integer quantity = packetQuantities.get(packetType);
        if (quantity != null && quantity > 0) {
            packetQuantities.put(packetType, quantity - 1);
        }
    }


    public int getPacketCooldown(PacketType packetType) {
        return packetCooldowns.getOrDefault(packetType, 10);
    }


    public int getPacketQuantity(PacketType packetType) {
        return packetQuantities.getOrDefault(packetType, 0);
    }

    public Map<Class<? extends Packet>, PacketType> getDiscoveredPacketTypes() {
        return new HashMap<>(packetTypeMap);
    }
}


