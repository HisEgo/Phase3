package server;

import model.UserProfile;
import model.UserData;
import security.MacAddressManager;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class UserManager {
    private DatabaseManager databaseManager;
    private Map<String, String> macToUserId;

    public UserManager() {
        this.databaseManager = new DatabaseManager();
        this.macToUserId = new ConcurrentHashMap<>();
        loadExistingUserProfiles();
    }

    public String identifyUser(Socket clientSocket) {
        try {
            String macAddress = getClientMacAddress(clientSocket);

            // Generate unique user ID based on MAC address + connection timestamp
            // This allows multiple clients from the same machine to have different IDs
            String userId = generateUniqueUserIdFromMacAddress(macAddress, clientSocket);

            // Check if user exists by generated user ID
            UserProfile profileById = databaseManager.getUserProfile(userId);
            if (profileById != null) {
                macToUserId.put(macAddress, userId);
                profileById.setLastUpdated(System.currentTimeMillis());
                databaseManager.storeUserProfile(profileById);
                System.out.println("Existing user identified by ID: " + profileById.getUsername() + " (MAC: " + macAddress + ", ID: " + userId + ")");
                return userId;
            }

            // Check if there's already a user with this MAC address
            UserProfile existingMacProfile = databaseManager.getUserProfileByMacAddress(macAddress);
            if (existingMacProfile != null) {
                // Use the existing profile but update the user ID mapping
                macToUserId.put(macAddress, userId);
                System.out.println("Using existing profile for MAC " + macAddress + " with new user ID: " + userId);
                return userId;
            }

            // New user - create profile with unique ID
            UserProfile newProfile = new UserProfile(userId, macAddress);
            newProfile.setUsername("Player_" + userId.substring(userId.length() - 6));
            newProfile.setLastUpdated(System.currentTimeMillis());

            try {
                macToUserId.put(macAddress, userId);
                databaseManager.storeUserProfile(newProfile);
                System.out.println("New user created with unique ID: " + newProfile.getUsername() + " (MAC: " + macAddress + ", ID: " + userId + ")");
            } catch (Exception e) {
                // If database constraint fails, use fallback ID
                System.err.println("Database constraint error, using fallback ID: " + e.getMessage());
                String fallbackId = "session_" + System.currentTimeMillis();
                macToUserId.put(macAddress, fallbackId);
                System.out.println("Using fallback user ID: " + fallbackId);
                return fallbackId;
            }

            return userId;
        } catch (Exception e) {
            System.err.println("Error identifying user: " + e.getMessage());
            // Fallback to session-based ID
            return "session_" + System.currentTimeMillis();
        }
    }

    private String getClientMacAddress(Socket clientSocket) {
        try {
            // Get the real system MAC address for consistent identification
            String systemMac = MacAddressManager.getSystemMacAddress();

            // Validate that we got a proper MAC address or fallback ID
            if (MacAddressManager.isValidSystemId(systemMac)) {
                System.out.println("Using system MAC address for user identification: " + systemMac);
                return systemMac;
            } else {
                // If MAC address is invalid, create a consistent fallback based on client IP
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                String consistentMac = createConsistentMacFromIP(clientIP);
                System.out.println("Using consistent MAC address based on IP: " + consistentMac);
                return consistentMac;
            }
        } catch (Exception e) {
            System.err.println("Error getting MAC address: " + e.getMessage());
            // Final fallback to MacAddressManager's fallback ID
            return MacAddressManager.getSystemMacAddress();
        }
    }

    private String createConsistentMacFromIP(String clientIP) {
        try {
            String[] ipParts = clientIP.split("\\.");

            // Create a consistent MAC address format using IP components
            // Format: XX:XX:XX:XX:XX:XX where XX are hex values
            StringBuilder macBuilder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                int ipPart = Integer.parseInt(ipParts[i]);
                macBuilder.append(String.format("%02X", ipPart));
                if (i < 3) {
                    macBuilder.append(":");
                }
            }
            // Add consistent components based on IP hash instead of timestamp
            int ipHash = Math.abs(clientIP.hashCode());
            macBuilder.append(":").append(String.format("%02X", (ipHash & 0xFF)));
            macBuilder.append(":").append(String.format("%02X", ((ipHash >> 8) & 0xFF)));

            return macBuilder.toString();
        } catch (Exception e) {
            // Fallback to a default MAC address
            return "00:00:00:00:00:01";
        }
    }

    public String generateUserIdFromMacAddress(String macAddress) {
        // Use MAC address as the primary user identifier
        // This ensures the same MAC address always gets the same user ID
        String cleanMac = macAddress.replace(":", "").replace("-", "");
        return "USER_" + cleanMac;
    }

    public String generateUniqueUserIdFromMacAddress(String macAddress, Socket clientSocket) {
        // Create a unique identifier that combines MAC address with connection details
        String cleanMac = macAddress.replace(":", "").replace("-", "");
        String clientIP = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getPort();
        long timestamp = System.currentTimeMillis();

        // Create a unique ID that combines MAC, IP, port, and timestamp
        // This ensures each connection gets a unique user ID
        String uniqueId = cleanMac + "_" + clientIP.replace(".", "") + "_" + clientPort + "_" + (timestamp % 10000);
        return "USER_" + uniqueId;
    }

    public void saveUserData(String userId, UserData data) {
        databaseManager.updateUserData(userId, data);
    }

    public void createUserProfile(UserProfile profile) {
        // Ensure the user ID is consistent with MAC address
        String expectedUserId = generateUserIdFromMacAddress(profile.getMacAddress());
        if (!expectedUserId.equals(profile.getUserId())) {
            System.out.println("Warning: User ID " + profile.getUserId() + " doesn't match MAC-based ID " + expectedUserId);
            // Update the profile to use MAC-based ID
            profile.setUserId(expectedUserId);
        }

        macToUserId.put(profile.getMacAddress(), profile.getUserId());
        databaseManager.storeUserProfile(profile);
        System.out.println("User profile created with MAC-based identification: " + profile.getUsername() + " (MAC: " + profile.getMacAddress() + ", ID: " + profile.getUserId() + ")");
    }

    public UserProfile getUserProfile(String userId) {
        return databaseManager.getUserProfile(userId);
    }

    public void updateUserProfile(String userId, UserData data) {
        databaseManager.updateUserData(userId, data);
    }

    public void updatePlayerScore(String userId, int score, int xpEarned, String levelId) {
        try {
            UserProfile profile = databaseManager.getUserProfile(userId);
            if (profile != null) {
                // Update profile with new score data
                UserData userData = new UserData();
                userData.setXpEarned(xpEarned);

                databaseManager.updateUserData(userId, userData);

                // Create score record
                leaderboard.ScoreRecord scoreRecord =
                        new leaderboard.ScoreRecord(
                                userId, levelId, score, 0.0, "session_" + System.currentTimeMillis()
                        );

                databaseManager.storeScore(userId, scoreRecord);

                System.out.println("Updated player score for user " + userId + ": " + score + " points, " + xpEarned + " XP");
            }
        } catch (Exception e) {
            System.err.println("Error updating player score: " + e.getMessage());
        }
    }

    private void loadExistingUserProfiles() {
        try {
            // Load all user profiles from database and build MAC to UserID mapping
            List<UserProfile> allProfiles = databaseManager.getAllUserProfiles();
            for (UserProfile profile : allProfiles) {
                macToUserId.put(profile.getMacAddress(), profile.getUserId());
            }
            System.out.println("Loaded " + allProfiles.size() + " existing user profiles from database");
        } catch (Exception e) {
            System.err.println("Error loading existing user profiles: " + e.getMessage());
            macToUserId = new ConcurrentHashMap<>();
        }
    }

    public int getUserCount() {
        return databaseManager.getAllUserProfiles().size();
    }

    public boolean userExists(String userId) {
        return databaseManager.getUserProfile(userId) != null;
    }

    public UserProfile getUserByMacAddress(String macAddress) {
        UserProfile profile = databaseManager.getUserProfileByMacAddress(macAddress);
        if (profile != null) {
            // Update cache
            macToUserId.put(macAddress, profile.getUserId());
            System.out.println("User retrieved by MAC address: " + profile.getUsername() + " (MAC: " + macAddress + ", ID: " + profile.getUserId() + ")");
        }
        return profile;
    }

    public String getMacAddressForUser(String userId) {
        // Search through the cache first
        for (Map.Entry<String, String> entry : macToUserId.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }

        // If not in cache, search database
        UserProfile profile = databaseManager.getUserProfile(userId);
        if (profile != null) {
            macToUserId.put(profile.getMacAddress(), userId);
            return profile.getMacAddress();
        }

        return null;
    }

    public List<UserProfile> getAllUserProfiles() {
        return databaseManager.getAllUserProfiles();
    }

    public Map<String, String> getMacToUserIdMapping() {
        return new HashMap<>(macToUserId);
    }

    public Map<String, Object> getDatabaseStats() {
        return databaseManager.getDatabaseStats();
    }

    public void clearCache() {
        databaseManager.clearCache();
    }

    public boolean isValidMacAddress(String macAddress) {
        if (macAddress == null || macAddress.trim().isEmpty()) {
            return false;
        }

        // Check if it's a real MAC address (contains colons or dashes)
        if (macAddress.contains(":") || macAddress.contains("-")) {
            return macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
        }

        // Check if it's a consistent IP-based MAC (should be 12 hex characters)
        return macAddress.matches("^[0-9A-Fa-f]{12}$");
    }

    public String getMacAddressDisplayName(String macAddress) {
        if (!isValidMacAddress(macAddress)) {
            return "Unknown System";
        }

        // If it's a real MAC address, format it nicely
        if (macAddress.contains(":") || macAddress.contains("-")) {
            return "System " + macAddress.toUpperCase();
        }

        // If it's an IP-based MAC, format it as a consistent identifier
        return "System " + macAddress.toUpperCase();
    }

    public void backupUserData(String userId) {
        // This method is kept for compatibility but doesn't need to do anything
        // since we're using database storage now
        System.out.println("Backup requested for user: " + userId + " (using database storage)");
    }

    public void clearAllData() {
        databaseManager.clearAllData();
        macToUserId.clear();
        System.out.println("All user data cleared");
    }
}

