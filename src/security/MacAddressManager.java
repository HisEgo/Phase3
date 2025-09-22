package security;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;


public class MacAddressManager {
    private static final String FALLBACK_ID_PREFIX = "FALLBACK_";


    public static String getSystemMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Skip loopback and virtual interfaces
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                byte[] macBytes = networkInterface.getHardwareAddress();
                if (macBytes != null && macBytes.length == 6) {
                    return formatMacAddress(macBytes);
                }
            }
        } catch (SocketException e) {
            System.err.println("Failed to retrieve MAC address: " + e.getMessage());
        }

        // Fallback to generated UUID if MAC address cannot be retrieved
        return generateFallbackId();
    }

    private static String formatMacAddress(byte[] macBytes) {
        StringBuilder macAddress = new StringBuilder();
        for (int i = 0; i < macBytes.length; i++) {
            macAddress.append(String.format("%02X", macBytes[i]));
            if (i < macBytes.length - 1) {
                macAddress.append(":");
            }
        }
        return macAddress.toString();
    }


    private static String generateFallbackId() {
        String fallbackId = FALLBACK_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        System.out.println("Using fallback ID: " + fallbackId);
        return fallbackId;
    }


    public static boolean isValidSystemId(String systemId) {
        if (systemId == null || systemId.trim().isEmpty()) {
            return false;
        }

        // Check if it's a fallback ID
        if (systemId.startsWith(FALLBACK_ID_PREFIX)) {
            return systemId.length() == FALLBACK_ID_PREFIX.length() + 12;
        }

        // Check if it's a valid MAC address format (XX:XX:XX:XX:XX:XX)
        return systemId.matches("^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$");
    }


    public static String getDisplayName(String systemId) {
        if (systemId == null || systemId.trim().isEmpty()) {
            return "Unknown System";
        }

        if (systemId.startsWith(FALLBACK_ID_PREFIX)) {
            return "System " + systemId.substring(FALLBACK_ID_PREFIX.length());
        }

        return "System " + systemId.substring(0, 8) + "...";
    }

    public static String createPlayerId(String systemId, String playerName) {
        if (systemId == null || systemId.trim().isEmpty()) {
            systemId = getSystemMacAddress();
        }

        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Anonymous";
        }

        // Create a hash-based ID to ensure uniqueness
        String combined = systemId + "_" + playerName;
        return "PLAYER_" + Math.abs(combined.hashCode());
    }
}


