package server;

import leaderboard.ScoreRecord;
import model.UserData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OfflineDataHandler {
    private static final String OFFLINE_DATA_DIR = "offline_data";
    private static final String OFFLINE_SCORES_FILE = "offline_scores.json";
    private static final String OFFLINE_USER_DATA_FILE = "offline_user_data.json";

    private DatabaseManager databaseManager;
    private ObjectMapper objectMapper;
    private Map<String, List<ScoreRecord>> offlineScores;
    private Map<String, UserData> offlineUserData;
    private ScheduledExecutorService syncScheduler;
    private boolean isConnected;

    public OfflineDataHandler() {
        this.databaseManager = new DatabaseManager();
        this.objectMapper = new ObjectMapper();
        this.offlineScores = new ConcurrentHashMap<>();
        this.offlineUserData = new ConcurrentHashMap<>();
        this.isConnected = false;

        initializeOfflineDataDirectory();
        loadOfflineData();
        startSyncScheduler();
    }


    public void storeOfflineScore(String userId, ScoreRecord score) {
        offlineScores.computeIfAbsent(userId, k -> new ArrayList<>()).add(score);
        saveOfflineData();

        System.out.println("Stored offline score for user " + userId + ": " + score.getXpEarned() + " XP");
    }


    public void storeOfflineUserData(String userId, UserData userData) {
        offlineUserData.put(userId, userData);
        saveOfflineData();

        System.out.println("Stored offline user data for user " + userId);
    }

    public void synchronizeOfflineData(String userId) {
        if (!isConnected) {
            System.out.println("Cannot synchronize - server not connected");
            return;
        }

        try {
            // Sync offline scores to database
            List<ScoreRecord> userScores = offlineScores.get(userId);
            if (userScores != null && !userScores.isEmpty()) {
                System.out.println("Synchronizing " + userScores.size() + " offline scores for user " + userId);

                // Store each score in database
                for (ScoreRecord score : userScores) {
                    try {
                        databaseManager.storeScore(userId, score);
                        System.out.println("Synced score: " + score.getXpEarned() + " XP from level " + score.getLevelId());
                    } catch (Exception e) {
                        System.err.println("Error storing offline score: " + e.getMessage());
                    }
                }

                // Remove synced scores from offline storage
                offlineScores.remove(userId);
            }

            // Sync offline user data to database
            UserData userData = offlineUserData.get(userId);
            if (userData != null) {
                System.out.println("Synchronizing offline user data for user " + userId);

                try {
                    databaseManager.updateUserData(userId, userData);
                    System.out.println("Synced user data: XP Earned: " + userData.getXpEarned());
                } catch (Exception e) {
                    System.err.println("Error updating offline user data: " + e.getMessage());
                }

                // Remove synced data from offline storage
                offlineUserData.remove(userId);
            }

            // Save updated offline data
            saveOfflineData();

        } catch (Exception e) {
            System.err.println("Error synchronizing offline data for user " + userId + ": " + e.getMessage());
        }
    }

    public void synchronizeAllOfflineData() {
        if (!isConnected) {
            System.out.println("Cannot synchronize - server not connected");
            return;
        }

        Set<String> allUsers = new HashSet<>();
        allUsers.addAll(offlineScores.keySet());
        allUsers.addAll(offlineUserData.keySet());

        for (String userId : allUsers) {
            synchronizeOfflineData(userId);
        }

        System.out.println("Synchronized offline data for " + allUsers.size() + " users");
    }

    public void setConnectionStatus(boolean connected) {
        this.isConnected = connected;

        if (connected) {
            System.out.println("Server connection restored - starting offline data synchronization");
            // Schedule immediate sync
            syncScheduler.schedule(this::synchronizeAllOfflineData, 1, TimeUnit.SECONDS);
        } else {
            System.out.println("Server connection lost - offline mode enabled");
        }
    }


    public int getTotalOfflineScores() {
        return offlineScores.values().stream()
                .mapToInt(List::size)
                .sum();
    }


    public int getUsersWithOfflineData() {
        Set<String> allUsers = new HashSet<>();
        allUsers.addAll(offlineScores.keySet());
        allUsers.addAll(offlineUserData.keySet());
        return allUsers.size();
    }


    public List<ScoreRecord> getOfflineScores(String userId) {
        return new ArrayList<>(offlineScores.getOrDefault(userId, new ArrayList<>()));
    }


    public UserData getOfflineUserData(String userId) {
        return offlineUserData.get(userId);
    }


    private void initializeOfflineDataDirectory() {
        File offlineDir = new File(OFFLINE_DATA_DIR);
        if (!offlineDir.exists()) {
            if (offlineDir.mkdirs()) {
                System.out.println("Created offline data directory: " + OFFLINE_DATA_DIR);
            } else {
                System.err.println("Failed to create offline data directory: " + OFFLINE_DATA_DIR);
            }
        }
    }


    private void loadOfflineData() {
        try {
            // Load offline scores
            File scoresFile = new File(OFFLINE_DATA_DIR, OFFLINE_SCORES_FILE);
            if (scoresFile.exists()) {
                offlineScores = objectMapper.readValue(scoresFile,
                        objectMapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, List.class));
                System.out.println("Loaded " + getTotalOfflineScores() + " offline scores");
            }

            // Load offline user data
            File userDataFile = new File(OFFLINE_DATA_DIR, OFFLINE_USER_DATA_FILE);
            if (userDataFile.exists()) {
                offlineUserData = objectMapper.readValue(userDataFile,
                        objectMapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, UserData.class));
                System.out.println("Loaded offline data for " + offlineUserData.size() + " users");
            }

        } catch (Exception e) {
            System.err.println("Error loading offline data: " + e.getMessage());
            // Initialize with empty maps if loading fails
            offlineScores = new ConcurrentHashMap<>();
            offlineUserData = new ConcurrentHashMap<>();
        }
    }

    private void saveOfflineData() {
        try {
            // Save offline scores
            File scoresFile = new File(OFFLINE_DATA_DIR, OFFLINE_SCORES_FILE);
            objectMapper.writeValue(scoresFile, offlineScores);

            // Save offline user data
            File userDataFile = new File(OFFLINE_DATA_DIR, OFFLINE_USER_DATA_FILE);
            objectMapper.writeValue(userDataFile, offlineUserData);

        } catch (Exception e) {
            System.err.println("Error saving offline data: " + e.getMessage());
        }
    }

    private void startSyncScheduler() {
        syncScheduler = Executors.newScheduledThreadPool(1);

        // Schedule periodic sync attempts every 5 minutes
        syncScheduler.scheduleAtFixedRate(() -> {
            if (isConnected) {
                synchronizeAllOfflineData();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }


    public void shutdown() {
        if (syncScheduler != null) {
            syncScheduler.shutdown();
            try {
                if (!syncScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    syncScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                syncScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Save any remaining offline data
        saveOfflineData();
    }
}


