package server;

import database.HibernateUtil;
import database.entity.*;
import model.UserProfile;
import leaderboard.ScoreRecord;
import model.UserData;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class DatabaseManager {

    private SessionFactory sessionFactory;
    private Map<String, UserProfile> userCache; // Cache for frequently accessed users

    public DatabaseManager() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
        this.userCache = new ConcurrentHashMap<>();

        System.out.println("DatabaseManager initialized with Hibernate ORM");
    }

    public void storeUserProfile(UserProfile profile) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                // Convert UserProfile to UserProfileEntity
                UserProfileEntity entity = convertToEntity(profile);

                // Save or update the entity
                session.saveOrUpdate(entity);

                // Update cache
                userCache.put(profile.getUserId(), profile);

                transaction.commit();
                System.out.println("Stored user profile in database: " + profile.getUserId());

            } catch (Exception e) {
                transaction.rollback();
                System.err.println("Error storing user profile: " + e.getMessage());
                throw e;
            }
        }
    }

    public UserProfile getUserProfile(String userId) {
        // Check cache first
        UserProfile cached = userCache.get(userId);
        if (cached != null) {
            return cached;
        }

        try (Session session = sessionFactory.openSession()) {
            UserProfileEntity entity = session.get(UserProfileEntity.class, userId);

            if (entity != null) {
                UserProfile profile = convertFromEntity(entity);
                userCache.put(userId, profile);
                return profile;
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error retrieving user profile: " + e.getMessage());
            return null;
        }
    }

    public UserProfile getUserProfileByMacAddress(String macAddress) {
        try (Session session = sessionFactory.openSession()) {
            Query<UserProfileEntity> query = session.createQuery(
                    "FROM UserProfileEntity WHERE macAddress = :macAddress",
                    UserProfileEntity.class
            );
            query.setParameter("macAddress", macAddress);

            UserProfileEntity entity = query.uniqueResult();

            if (entity != null) {
                UserProfile profile = convertFromEntity(entity);
                userCache.put(profile.getUserId(), profile);
                return profile;
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error retrieving user profile by MAC: " + e.getMessage());
            return null;
        }
    }

    public void storeScore(String userId, ScoreRecord score) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                // Get user profile entity
                UserProfileEntity userEntity = session.get(UserProfileEntity.class, userId);
                if (userEntity == null) {
                    throw new RuntimeException("User not found: " + userId);
                }

                // Convert ScoreRecord to ScoreRecordEntity
                ScoreRecordEntity scoreEntity = convertToEntity(score, userEntity);

                // Save the score record
                session.save(scoreEntity);

                // Update user's total XP and statistics
                updateUserStatistics(userEntity, score);
                session.update(userEntity);

                transaction.commit();
                System.out.println("Stored score record in database for user: " + userId);

            } catch (Exception e) {
                transaction.rollback();
                System.err.println("Error storing score record: " + e.getMessage());
                throw e;
            }
        }
    }


    public List<ScoreRecord> getUserScores(String userId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScoreRecordEntity> query = session.createQuery(
                    "FROM ScoreRecordEntity WHERE userProfile.userId = :userId ORDER BY timestamp DESC",
                    ScoreRecordEntity.class
            );
            query.setParameter("userId", userId);

            List<ScoreRecordEntity> entities = query.list();
            return entities.stream()
                    .map(this::convertFromEntity)
                    .toList();

        } catch (Exception e) {
            System.err.println("Error retrieving user scores: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<ScoreRecord> getTopScores(int limit) {
        try (Session session = sessionFactory.openSession()) {
            Query<ScoreRecordEntity> query = session.createQuery(
                    "FROM ScoreRecordEntity ORDER BY score DESC",
                    ScoreRecordEntity.class
            );
            query.setMaxResults(limit);

            List<ScoreRecordEntity> entities = query.list();
            return entities.stream()
                    .map(this::convertFromEntity)
                    .toList();

        } catch (Exception e) {
            System.err.println("Error retrieving top scores: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public List<UserProfile> getAllUserProfiles() {
        try (Session session = sessionFactory.openSession()) {
            Query<UserProfileEntity> query = session.createQuery(
                    "FROM UserProfileEntity",
                    UserProfileEntity.class
            );

            List<UserProfileEntity> entities = query.list();
            return entities.stream()
                    .map(this::convertFromEntity)
                    .toList();

        } catch (Exception e) {
            System.err.println("Error retrieving all user profiles: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<UserProfile> getUsersByRank(String rank) {
        try (Session session = sessionFactory.openSession()) {
            Query<UserProfileEntity> query = session.createQuery(
                    "FROM UserProfileEntity WHERE userRank = :rank",
                    UserProfileEntity.class
            );
            query.setParameter("rank", rank);

            List<UserProfileEntity> entities = query.list();
            return entities.stream()
                    .map(this::convertFromEntity)
                    .toList();

        } catch (Exception e) {
            System.err.println("Error retrieving users by rank: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void updateUserData(String userId, UserData userData) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                UserProfileEntity entity = session.get(UserProfileEntity.class, userId);
                if (entity != null) {
                    // Update entity with new data
                    entity.setTotalXP(entity.getTotalXP() + userData.getXpEarned());
                    entity.setLastLogin(LocalDateTime.now());

                    session.update(entity);

                    // Update cache
                    UserProfile profile = convertFromEntity(entity);
                    userCache.put(userId, profile);

                    transaction.commit();
                    System.out.println("Updated user data in database: " + userId);
                }

            } catch (Exception e) {
                transaction.rollback();
                System.err.println("Error updating user data: " + e.getMessage());
                throw e;
            }
        }
    }

    public void deleteUserProfile(String userId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                UserProfileEntity entity = session.get(UserProfileEntity.class, userId);
                if (entity != null) {
                    session.delete(entity);
                    userCache.remove(userId);
                    transaction.commit();
                    System.out.println("Deleted user profile from database: " + userId);
                }

            } catch (Exception e) {
                transaction.rollback();
                System.err.println("Error deleting user profile: " + e.getMessage());
                throw e;
            }
        }
    }

    public void backupData(String backupName) {
        System.out.println("Database backup requested: " + backupName);
        System.out.println("All data is already stored in the database with Hibernate ORM");
        System.out.println("Database info: " + HibernateUtil.getDatabaseInfo());
    }

    public void clearCache() {
        userCache.clear();
        System.out.println("User cache cleared");
    }

    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();

        try (Session session = sessionFactory.openSession()) {
            // Count users
            Query<Long> userCountQuery = session.createQuery(
                    "SELECT COUNT(*) FROM UserProfileEntity", Long.class
            );
            stats.put("totalUsers", userCountQuery.uniqueResult());

            // Count score records
            Query<Long> scoreCountQuery = session.createQuery(
                    "SELECT COUNT(*) FROM ScoreRecordEntity", Long.class
            );
            stats.put("totalScores", scoreCountQuery.uniqueResult());

            // Cache size
            stats.put("cacheSize", userCache.size());

        } catch (Exception e) {
            System.err.println("Error getting database stats: " + e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // Conversion methods between domain objects and entities

    private UserProfileEntity convertToEntity(UserProfile profile) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setUserId(profile.getUserId());
        entity.setMacAddress(profile.getMacAddress());
        entity.setUsername(profile.getUsername());
        entity.setTotalXP(profile.getTotalXP());
        // Set default values for fields that don't exist in UserProfile
        entity.setUserRank("Beginner");
        entity.setBestScore(0);
        entity.setAverageScore(0.0);
        entity.setLevelsCompleted(0);
        entity.setAverageCompletionTime(0.0);
        entity.setUnlockedAbilities(new ArrayList<>(profile.getUnlockedAbilities()));
        entity.setLastLogin(LocalDateTime.now());
        return entity;
    }

    private UserProfile convertFromEntity(UserProfileEntity entity) {
        UserProfile profile = new UserProfile(entity.getUserId(), entity.getMacAddress());
        profile.setUsername(entity.getUsername());
        profile.setTotalXP(entity.getTotalXP());
        profile.setUnlockedAbilities(new HashSet<>(entity.getUnlockedAbilities()));
        profile.setLastUpdated(entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        return profile;
    }

    private ScoreRecordEntity convertToEntity(ScoreRecord score, UserProfileEntity userEntity) {
        ScoreRecordEntity entity = new ScoreRecordEntity();
        entity.setUserProfile(userEntity);
        entity.setLevelId(score.getLevelId());
        entity.setScore(score.getScore());
        entity.setXpEarned(score.getXpEarned());
        entity.setCompletionTime(score.getCompletionTime());
        entity.setMultiplayer(false); // Default value
        entity.setSessionId("session_" + System.currentTimeMillis());
        entity.setDifficulty("Normal"); // Default value
        entity.setPacketsDelivered(0); // Default value
        entity.setPacketsFailed(0); // Default value
        entity.setAccuracyPercentage(100.0); // Default value
        return entity;
    }

    private ScoreRecord convertFromEntity(ScoreRecordEntity entity) {
        ScoreRecord score = new ScoreRecord();
        score.setLevelId(entity.getLevelId());
        score.setScore(entity.getScore());
        score.setXpEarned(entity.getXpEarned());
        score.setCompletionTime(entity.getCompletionTime());
        score.setTimestamp(entity.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        return score;
    }

    private void updateUserStatistics(UserProfileEntity userEntity, ScoreRecord score) {
        // Update total XP
        userEntity.setTotalXP(userEntity.getTotalXP() + score.getXpEarned());

        // Update best score
        if (score.getScore() > userEntity.getBestScore()) {
            userEntity.setBestScore(score.getScore());
        }

        // Update levels completed
        userEntity.setLevelsCompleted(userEntity.getLevelsCompleted() + 1);

        // Update average completion time (simplified calculation)
        double currentAvg = userEntity.getAverageCompletionTime();
        int levelsCompleted = userEntity.getLevelsCompleted();
        double newAvg = ((currentAvg * (levelsCompleted - 1)) + score.getCompletionTime()) / levelsCompleted;
        userEntity.setAverageCompletionTime(newAvg);
    }

    public void clearAllData() {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                // Clear all tables in the correct order to respect foreign key constraints
                session.createQuery("DELETE FROM ScoreRecordEntity").executeUpdate();
                session.createQuery("DELETE FROM LevelRecordEntity").executeUpdate();
                session.createQuery("DELETE FROM PlayerRecordEntity").executeUpdate();
                session.createQuery("DELETE FROM MultiplayerSessionEntity").executeUpdate();
                session.createQuery("DELETE FROM GameSessionEntity").executeUpdate();
                session.createQuery("DELETE FROM WireConnectionEntity").executeUpdate();
                session.createQuery("DELETE FROM PortEntity").executeUpdate();
                session.createQuery("DELETE FROM SystemEntity").executeUpdate();
                session.createQuery("DELETE FROM UserProfileEntity").executeUpdate();

                transaction.commit();
                System.out.println("All database data cleared successfully");

            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                System.err.println("Error clearing database data: " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Error clearing database data: " + e.getMessage());
        }
    }
}


