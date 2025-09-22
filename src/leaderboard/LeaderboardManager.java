package leaderboard;

import security.MacAddressManager;
import security.DataIntegrityValidator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LeaderboardManager {
    private static final String LEADERBOARD_FILE = "leaderboard.json";
    private static final String OFFLINE_SCORES_FILE = "offline_scores.json";

    private Map<String, PlayerRecord> playerRecords;
    private Map<String, LevelRecord> levelRecords;
    private List<ScoreRecord> topScores;
    private ObjectMapper objectMapper;
    private DataIntegrityValidator dataIntegrityValidator;

    public LeaderboardManager() {
        this.playerRecords = new ConcurrentHashMap<>();
        this.levelRecords = new ConcurrentHashMap<>();
        this.topScores = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
        this.dataIntegrityValidator = new DataIntegrityValidator();

        loadLeaderboardData();

        // Initialize with default data if leaderboard is completely empty
        if (isLeaderboardEmpty()) {
            initializeDefaultData();
        }
    }

    /**
     * Records a player's completion of a level with data integrity validation.
     */
    public boolean recordLevelCompletionWithValidation(String playerId, String levelId, double completionTime, int xpEarned) {
        // Create score record for validation
        ScoreRecord score = new ScoreRecord(playerId, levelId, xpEarned, completionTime,
                java.time.LocalDate.now().toString());

        // Validate the score submission
        DataIntegrityValidator.ValidationResult validationResult = dataIntegrityValidator.validateScoreSubmission(score, playerId);

        if (!validationResult.isValid()) {
            System.err.println("Score validation failed for player " + playerId + ": " + validationResult.getViolations());
            return false;
        }

        // If validation passes, record the completion
        recordLevelCompletion(playerId, levelId, completionTime, xpEarned);
        return true;
    }

    public void recordLevelCompletion(String playerId, String levelId, double completionTime, int xpEarned) {
        // Update player record
        PlayerRecord playerRecord = playerRecords.computeIfAbsent(playerId, k -> new PlayerRecord(playerId));
        playerRecord.addLevelCompletion(levelId, completionTime, xpEarned);

        // Update level record
        LevelRecord levelRecord = levelRecords.computeIfAbsent(levelId, k -> new LevelRecord(levelId));
        levelRecord.addCompletion(playerId, completionTime);

        // Update top scores
        updateTopScores(playerId, xpEarned);

        // Save data
        saveLeaderboardData();
    }


    public void recordLevelCompletionWithMac(String playerName, String levelId, double completionTime, int xpEarned) {
        String systemId = MacAddressManager.getSystemMacAddress();
        String playerId = MacAddressManager.createPlayerId(systemId, playerName);
        recordLevelCompletion(playerId, levelId, completionTime, xpEarned);
    }

    public void recordOfflineScore(String playerId, String levelId, double completionTime, int xpEarned) {
        OfflineScore offlineScore = new OfflineScore(playerId, levelId, completionTime, xpEarned, System.currentTimeMillis());

        try {
            List<OfflineScore> offlineScores = loadOfflineScores();
            offlineScores.add(offlineScore);
            saveOfflineScores(offlineScores);
        } catch (Exception e) {
            System.err.println("Failed to save offline score: " + e.getMessage());
        }
    }

    public void recordOfflineScoreWithMac(String playerName, String levelId, double completionTime, int xpEarned) {
        String systemId = MacAddressManager.getSystemMacAddress();
        String playerId = MacAddressManager.createPlayerId(systemId, playerName);
        recordOfflineScore(playerId, levelId, completionTime, xpEarned);
    }

    public void synchronizeOfflineScores() {
        try {
            List<OfflineScore> offlineScores = loadOfflineScores();

            for (OfflineScore offlineScore : offlineScores) {
                recordLevelCompletion(
                        offlineScore.getPlayerId(),
                        offlineScore.getLevelId(),
                        offlineScore.getCompletionTime(),
                        offlineScore.getXpEarned()
                );
            }

            // Clear offline scores after successful synchronization
            saveOfflineScores(new ArrayList<>());

        } catch (Exception e) {
            System.err.println("Failed to synchronize offline scores: " + e.getMessage());
        }
    }


    public double getLevelBestTime(String levelId) {
        LevelRecord levelRecord = levelRecords.get(levelId);
        return levelRecord != null ? levelRecord.getBestTime() : Double.MAX_VALUE;
    }


    public String getLevelBestPlayer(String levelId) {
        LevelRecord levelRecord = levelRecords.get(levelId);
        return levelRecord != null ? levelRecord.getBestPlayer() : null;
    }


    public int getHighestSingleGameXP() {
        if (topScores.isEmpty()) return 0;
        return topScores.get(0).getXpEarned();
    }


    public String getHighestSingleGameXPPlayer() {
        if (topScores.isEmpty()) return null;
        return topScores.get(0).getPlayerId();
    }


    public List<ScoreRecord> getTopScores(int count) {
        return topScores.subList(0, Math.min(count, topScores.size()));
    }

    public PlayerRecord getPlayerRecord(String playerId) {
        return playerRecords.get(playerId);
    }


    public Map<String, LevelRecord> getLevelRecords() {
        return new HashMap<>(levelRecords);
    }


    private void updateTopScores(String playerId, int xpEarned) {
        ScoreRecord newScore = new ScoreRecord(playerId, xpEarned, System.currentTimeMillis());

        // Find insertion point to maintain sorted order
        int insertIndex = 0;
        for (int i = 0; i < topScores.size(); i++) {
            if (xpEarned > topScores.get(i).getXpEarned()) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }

        topScores.add(insertIndex, newScore);

        // Keep only top 100 scores
        if (topScores.size() > 100) {
            topScores = topScores.subList(0, 100);
        }
    }


    private void loadLeaderboardData() {
        try {
            File file = new File(LEADERBOARD_FILE);
            if (file.exists()) {
                LeaderboardData data = objectMapper.readValue(file, LeaderboardData.class);
                this.playerRecords = new ConcurrentHashMap<>(data.getPlayerRecords());
                this.levelRecords = new ConcurrentHashMap<>(data.getLevelRecords());
                this.topScores = new ArrayList<>(data.getTopScores());
            }
        } catch (Exception e) {
            System.err.println("Failed to load leaderboard data: " + e.getMessage());
        }
    }

    private void saveLeaderboardData() {
        try {
            LeaderboardData data = new LeaderboardData(
                    new HashMap<>(playerRecords),
                    new HashMap<>(levelRecords),
                    new ArrayList<>(topScores)
            );

            objectMapper.writeValue(new File(LEADERBOARD_FILE), data);
        } catch (Exception e) {
            System.err.println("Failed to save leaderboard data: " + e.getMessage());
        }
    }


    private List<OfflineScore> loadOfflineScores() throws IOException {
        File file = new File(OFFLINE_SCORES_FILE);
        if (file.exists()) {
            return objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OfflineScore.class));
        }
        return new ArrayList<>();
    }


    private void saveOfflineScores(List<OfflineScore> offlineScores) throws IOException {
        objectMapper.writeValue(new File(OFFLINE_SCORES_FILE), offlineScores);
    }


    public LeaderboardInfo getLeaderboardInfo() {
        return new LeaderboardInfo(
                new HashMap<>(playerRecords),
                new HashMap<>(levelRecords),
                new ArrayList<>(topScores)
        );
    }


    public int getOfflineScoreCount() {
        try {
            List<OfflineScore> offlineScores = loadOfflineScores();
            return offlineScores.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getCurrentSystemId() {
        return MacAddressManager.getSystemMacAddress();
    }


    public String getCurrentSystemDisplayName() {
        return MacAddressManager.getDisplayName(getCurrentSystemId());
    }


    public static class LeaderboardInfo {
        private Map<String, PlayerRecord> playerRecords;
        private Map<String, LevelRecord> levelRecords;
        private List<ScoreRecord> topScores;

        public LeaderboardInfo(Map<String, PlayerRecord> playerRecords,
                               Map<String, LevelRecord> levelRecords,
                               List<ScoreRecord> topScores) {
            this.playerRecords = playerRecords;
            this.levelRecords = levelRecords;
            this.topScores = topScores;
        }

        // Getters
        public Map<String, PlayerRecord> getPlayerRecords() { return playerRecords; }
        public Map<String, LevelRecord> getLevelRecords() { return levelRecords; }
        public List<ScoreRecord> getTopScores() { return topScores; }
    }

    private static class LeaderboardData {
        private Map<String, PlayerRecord> playerRecords;
        private Map<String, LevelRecord> levelRecords;
        private List<ScoreRecord> topScores;

        public LeaderboardData() {}

        public LeaderboardData(Map<String, PlayerRecord> playerRecords,
                               Map<String, LevelRecord> levelRecords,
                               List<ScoreRecord> topScores) {
            this.playerRecords = playerRecords;
            this.levelRecords = levelRecords;
            this.topScores = topScores;
        }

        // Getters and setters for Jackson
        public Map<String, PlayerRecord> getPlayerRecords() { return playerRecords; }
        public void setPlayerRecords(Map<String, PlayerRecord> playerRecords) { this.playerRecords = playerRecords; }
        public Map<String, LevelRecord> getLevelRecords() { return levelRecords; }
        public void setLevelRecords(Map<String, LevelRecord> levelRecords) { this.levelRecords = levelRecords; }
        public List<ScoreRecord> getTopScores() { return topScores; }
        public void setTopScores(List<ScoreRecord> topScores) { this.topScores = topScores; }
    }


    public boolean isUserBlacklisted(String userId) {
        return dataIntegrityValidator.isUserBlacklisted(userId);
    }


    public int getUserViolationCount(String userId) {
        return dataIntegrityValidator.getUserViolationCount(userId);
    }

    public String generateScoreHash(ScoreRecord score) throws Exception {
        return dataIntegrityValidator.generateScoreHash(score);
    }

    private boolean isLeaderboardEmpty() {
        return playerRecords.isEmpty() && levelRecords.isEmpty() && topScores.isEmpty();
    }


    private void initializeDefaultData() {
        try {
            System.out.println("Initializing leaderboard with default data for new installation...");

            // Create some example level records
            LevelRecord level1 = new LevelRecord("level1");
            level1.addCompletion("demo_player", 45.2);
            levelRecords.put("level1", level1);

            LevelRecord level2 = new LevelRecord("level2");
            level2.addCompletion("demo_player", 78.3);
            levelRecords.put("level2", level2);

            // Create a demo player record
            PlayerRecord demoPlayer = new PlayerRecord("demo_player");
            demoPlayer.addLevelCompletion("level1", 45.2, 100);
            demoPlayer.addLevelCompletion("level2", 78.3, 120);
            playerRecords.put("demo_player", demoPlayer);

            // Create some example top scores
            ScoreRecord score1 = new ScoreRecord("demo_player", "level1", 100, 45.2,
                    java.time.LocalDate.now().minusDays(1).toString());
            ScoreRecord score2 = new ScoreRecord("demo_player", "level2", 120, 78.3,
                    java.time.LocalDate.now().toString());

            topScores.add(score1);
            topScores.add(score2);

            // Sort top scores by XP (highest first)
            topScores.sort((a, b) -> Integer.compare(b.getXpEarned(), a.getXpEarned()));

            // Save the initialized data
            saveLeaderboardData();

            System.out.println("Leaderboard initialized with " + playerRecords.size() + " players, " +
                    levelRecords.size() + " levels, and " + topScores.size() + " scores");

        } catch (Exception e) {
            System.err.println("Failed to initialize default leaderboard data: " + e.getMessage());
        }
    }
}


