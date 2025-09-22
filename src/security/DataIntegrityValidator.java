package security;

import model.UserData;
import leaderboard.ScoreRecord;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class DataIntegrityValidator {
    private static final int MAX_XP_PER_GAME = 10000;
    private static final int MIN_GAME_DURATION = 30; // seconds
    private static final int MAX_GAME_DURATION = 3600; // 1 hour
    private static final double MAX_COMPLETION_TIME_RATIO = 0.1; // 10% of world record

    // Track suspicious patterns
    private Map<String, List<ScoreRecord>> userScoreHistory;
    private Map<String, Integer> userViolationCount;
    private Set<String> blacklistedUsers;

    public DataIntegrityValidator() {
        this.userScoreHistory = new ConcurrentHashMap<>();
        this.userViolationCount = new ConcurrentHashMap<>();
        this.blacklistedUsers = ConcurrentHashMap.newKeySet();
    }

    public ValidationResult validateScoreSubmission(ScoreRecord score, String userId) {
        ValidationResult result = new ValidationResult();

        try {
            // 1. Basic bounds checking
            if (!validateBasicBounds(score)) {
                result.addViolation("Score values outside acceptable bounds");
                result.setValid(false);
            }

            // 2. Realistic completion time validation
            if (!validateCompletionTime(score)) {
                result.addViolation("Unrealistic completion time");
                result.setValid(false);
            }

            // 3. Pattern detection
            if (!validateScorePatterns(score, userId)) {
                result.addViolation("Suspicious score pattern detected");
                result.setValid(false);
            }

            // 4. Cryptographic validation (if hash is provided)
            if (score.getHash() != null && !validateCryptographicHash(score)) {
                result.addViolation("Data integrity hash validation failed");
                result.setValid(false);
            }

            // 5. Rate limiting
            if (!validateSubmissionRate(userId)) {
                result.addViolation("Submission rate too high");
                result.setValid(false);
            }

            // If validation passes, update user history
            if (result.isValid()) {
                updateUserHistory(userId, score);
            } else {
                incrementViolationCount(userId);
            }

        } catch (Exception e) {
            result.addViolation("Validation error: " + e.getMessage());
            result.setValid(false);
        }

        return result;
    }


    private boolean validateBasicBounds(ScoreRecord score) {
        // XP validation
        if (score.getXpEarned() < 0 || score.getXpEarned() > MAX_XP_PER_GAME) {
            return false;
        }

        // Completion time validation
        if (score.getCompletionTime() < 0 || score.getCompletionTime() > MAX_GAME_DURATION) {
            return false;
        }

        // Level ID validation
        if (score.getLevelId() == null || score.getLevelId().trim().isEmpty()) {
            return false;
        }

        return true;
    }


    private boolean validateCompletionTime(ScoreRecord score) {
        // Check if completion time is too fast (suspicious)
        if (score.getCompletionTime() < MIN_GAME_DURATION) {
            return false;
        }

        // Check if completion time is unrealistically fast compared to world records
        // This would require access to world record data
        double worldRecordTime = getWorldRecordTime(score.getLevelId());
        if (worldRecordTime > 0 && score.getCompletionTime() < worldRecordTime * MAX_COMPLETION_TIME_RATIO) {
            return false;
        }

        return true;
    }


    private boolean validateScorePatterns(ScoreRecord score, String userId) {
        List<ScoreRecord> history = userScoreHistory.get(userId);
        if (history == null || history.isEmpty()) {
            return true; // First submission
        }

        // Check for impossible score improvements
        ScoreRecord lastScore = history.get(history.size() - 1);
        if (score.getXpEarned() > lastScore.getXpEarned() * 2) {
            return false; // Suspicious XP jump
        }

        // Check for completion time anomalies
        if (lastScore.getCompletionTime() > 0 && score.getCompletionTime() > 0) {
            double improvementRatio = lastScore.getCompletionTime() / score.getCompletionTime();
            if (improvementRatio > 5.0) { // More than 5x improvement
                return false;
            }
        }

        // Check for submission frequency
        if (history.size() >= 2) {
            ScoreRecord secondLast = history.get(history.size() - 2);
            long timeDiff = score.getTimestamp() - secondLast.getTimestamp();
            if (timeDiff < 5000) { // Less than 5 seconds between submissions
                return false;
            }
        }

        return true;
    }


    private boolean validateCryptographicHash(ScoreRecord score) {
        if (score.getHash() == null) {
            return true; // No hash provided, skip validation
        }

        try {
            // Generate hash from score data
            String calculatedHash = generateScoreHash(score);
            return calculatedHash.equals(score.getHash());
        } catch (Exception e) {
            return false;
        }
    }


    private boolean validateSubmissionRate(String userId) {
        List<ScoreRecord> history = userScoreHistory.get(userId);
        if (history == null || history.size() < 3) {
            return true;
        }

        // Check last 3 submissions
        long currentTime = System.currentTimeMillis();
        int recentSubmissions = 0;

        for (int i = history.size() - 1; i >= 0 && i >= history.size() - 3; i--) {
            if (currentTime - history.get(i).getTimestamp() < 60000) { // Last minute
                recentSubmissions++;
            }
        }

        return recentSubmissions <= 2; // Max 2 submissions per minute
    }


    public String generateScoreHash(ScoreRecord score) throws NoSuchAlgorithmException {
        String dataToHash = score.getPlayerId() + score.getLevelId() + score.getXpEarned() +
                score.getCompletionTime() + score.getTimestamp();

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(dataToHash.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }


    private void updateUserHistory(String userId, ScoreRecord score) {
        userScoreHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(score);

        // Keep only last 20 scores for memory management
        List<ScoreRecord> history = userScoreHistory.get(userId);
        if (history.size() > 20) {
            history.remove(0);
        }
    }


    private void incrementViolationCount(String userId) {
        int violations = userViolationCount.getOrDefault(userId, 0) + 1;
        userViolationCount.put(userId, violations);

        // Blacklist user after 5 violations
        if (violations >= 5) {
            blacklistedUsers.add(userId);
            System.err.println("User " + userId + " blacklisted due to multiple violations");
        }
    }


    private double getWorldRecordTime(String levelId) {
        // This would typically query a database of world records
        // For now, return reasonable default values
        Map<String, Double> worldRecords = new HashMap<>();
        worldRecords.put("level_1", 120.0); // 2 minutes
        worldRecords.put("level_2", 180.0); // 3 minutes
        worldRecords.put("level_3", 240.0); // 4 minutes

        return worldRecords.getOrDefault(levelId, 180.0);
    }

    public boolean isUserBlacklisted(String userId) {
        return blacklistedUsers.contains(userId);
    }


    public int getUserViolationCount(String userId) {
        return userViolationCount.getOrDefault(userId, 0);
    }


    public static class ValidationResult {
        private boolean valid;
        private List<String> violations;
        private long validationTime;

        public ValidationResult() {
            this.valid = true;
            this.violations = new ArrayList<>();
            this.validationTime = System.currentTimeMillis();
        }

        public void addViolation(String violation) {
            this.violations.add(violation);
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public List<String> getViolations() { return violations; }
        public void setViolations(List<String> violations) { this.violations = violations; }

        public long getValidationTime() { return validationTime; }
        public void setValidationTime(long validationTime) { this.validationTime = validationTime; }
    }
}


