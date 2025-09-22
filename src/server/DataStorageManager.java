package server;

import model.UserProfile;
import leaderboard.ScoreRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorageManager {
    private static final String DATA_DIR = "server_data";
    private static final String USERS_FILE = "users.json";
    private static final String SCORES_FILE = "scores.json";
    private static final String LEADERBOARD_FILE = "leaderboard.json";

    private ObjectMapper objectMapper;
    private Map<String, UserProfile> users;
    private Map<String, List<ScoreRecord>> userScores;
    private Map<String, ScoreRecord> topScores;

    public DataStorageManager() {
        this.objectMapper = new ObjectMapper();
        this.users = new ConcurrentHashMap<>();
        this.userScores = new ConcurrentHashMap<>();
        this.topScores = new ConcurrentHashMap<>();

        initializeDataDirectory();
        loadData();
    }

    public void storeUserProfile(UserProfile profile) {
        users.put(profile.getUserId(), profile);
        saveData();
    }

    public UserProfile getUserProfile(String userId) {
        return users.get(userId);
    }

    public void storeScore(String userId, ScoreRecord score) {
        userScores.computeIfAbsent(userId, k -> new ArrayList<>()).add(score);

        // Update top scores if this is a new record
        String levelKey = score.getLevelId();
        ScoreRecord currentTop = topScores.get(levelKey);

        if (currentTop == null || score.getXpEarned() > currentTop.getXpEarned()) {
            topScores.put(levelKey, score);
        }

        saveData();
    }

    public List<ScoreRecord> getUserScores(String userId) {
        List<ScoreRecord> scores = userScores.get(userId);
        return scores != null ? new ArrayList<>(scores) : new ArrayList<>();
    }

    public ScoreRecord getTopScore(String levelId) {
        return topScores.get(levelId);
    }

    public Map<String, ScoreRecord> getAllTopScores() {
        return new HashMap<>(topScores);
    }


    public List<UserProfile> getTopUsers(int limit) {
        return users.values().stream()
                .sorted((u1, u2) -> Integer.compare(u2.getTotalXP(), u1.getTotalXP()))
                .limit(limit)
                .toList();
    }


    public ScoreRecord getFastestCompletion(String levelId) {
        return userScores.values().stream()
                .flatMap(List::stream)
                .filter(score -> levelId.equals(score.getLevelId()))
                .min(Comparator.comparingDouble(ScoreRecord::getCompletionTime))
                .orElse(null);
    }


    public Map<String, Object> getUserStats(String userId) {
        UserProfile profile = users.get(userId);
        List<ScoreRecord> scores = getUserScores(userId);

        Map<String, Object> stats = new HashMap<>();
        if (profile != null) {
            stats.put("totalXP", profile.getTotalXP());
            stats.put("userRank", profile.getUserRank());
            stats.put("totalGames", scores.size());
            stats.put("bestScore", profile.getBestScore());
            stats.put("averageScore", profile.getAverageScore());
            stats.put("lastPlayed", profile.getLastPlayed());
        }

        return stats;
    }

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();

        int totalUsers = users.size();
        int totalGames = userScores.values().stream().mapToInt(List::size).sum();
        int totalXP = users.values().stream().mapToInt(UserProfile::getTotalXP).sum();

        stats.put("totalUsers", totalUsers);
        stats.put("totalGames", totalGames);
        stats.put("totalXP", totalXP);
        stats.put("averageXPPerUser", totalUsers > 0 ? totalXP / totalUsers : 0);
        stats.put("averageGamesPerUser", totalUsers > 0 ? totalGames / totalUsers : 0);

        return stats;
    }


    public List<UserProfile> searchUsers(String pattern) {
        String lowerPattern = pattern.toLowerCase();
        return users.values().stream()
                .filter(user -> user.getUsername().toLowerCase().contains(lowerPattern))
                .toList();
    }

    public List<UserProfile> getUsersByRank(String rank) {
        return users.values().stream()
                .filter(user -> rank.equals(user.getUserRank()))
                .toList();
    }


    private void initializeDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            if (dataDir.mkdirs()) {
                System.out.println("Created server data directory: " + DATA_DIR);
            } else {
                System.err.println("Failed to create server data directory: " + DATA_DIR);
            }
        }
    }


    private void loadData() {
        try {
            // Load users
            File usersFile = new File(DATA_DIR, USERS_FILE);
            if (usersFile.exists()) {
                users = objectMapper.readValue(usersFile,
                        objectMapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, UserProfile.class));
                System.out.println("Loaded " + users.size() + " user profiles");
            }

            // Load user scores
            File scoresFile = new File(DATA_DIR, SCORES_FILE);
            if (scoresFile.exists()) {
                userScores = objectMapper.readValue(scoresFile,
                        objectMapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, List.class));
                System.out.println("Loaded scores for " + userScores.size() + " users");
            }

            // Load top scores
            File leaderboardFile = new File(DATA_DIR, LEADERBOARD_FILE);
            if (leaderboardFile.exists()) {
                topScores = objectMapper.readValue(leaderboardFile,
                        objectMapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, ScoreRecord.class));
                System.out.println("Loaded " + topScores.size() + " top scores");
            }

        } catch (Exception e) {
            System.err.println("Error loading data: " + e.getMessage());
            // Initialize with empty maps if loading fails
            users = new ConcurrentHashMap<>();
            userScores = new ConcurrentHashMap<>();
            topScores = new ConcurrentHashMap<>();
        }
    }


    private void saveData() {
        try {
            // Save users
            File usersFile = new File(DATA_DIR, USERS_FILE);
            objectMapper.writeValue(usersFile, users);

            // Save user scores
            File scoresFile = new File(DATA_DIR, SCORES_FILE);
            objectMapper.writeValue(scoresFile, userScores);

            // Save top scores
            File leaderboardFile = new File(DATA_DIR, LEADERBOARD_FILE);
            objectMapper.writeValue(leaderboardFile, topScores);

        } catch (Exception e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }


    public void backupData(String backupName) {
        try {
            String backupDir = DATA_DIR + "_backup_" + backupName;
            File backupDirFile = new File(backupDir);
            if (!backupDirFile.exists()) {
                backupDirFile.mkdirs();
            }

            // Copy all data files
            File[] dataFiles = new File(DATA_DIR).listFiles();
            if (dataFiles != null) {
                for (File file : dataFiles) {
                    if (file.isFile()) {
                        File backupFile = new File(backupDir, file.getName());
                        objectMapper.writeValue(backupFile,
                                objectMapper.readValue(file, Object.class));
                    }
                }
            }

            System.out.println("Data backup completed: " + backupDir);

        } catch (Exception e) {
            System.err.println("Error creating backup: " + e.getMessage());
        }
    }


    public int getUserCount() {
        return users.size();
    }

    public int getTotalScoreCount() {
        return userScores.values().stream().mapToInt(List::size).sum();
    }
}


