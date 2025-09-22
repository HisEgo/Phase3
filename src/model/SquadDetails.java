package model;

import java.io.Serializable;
import java.util.*;
import java.lang.System;

public class SquadDetails implements Serializable {
    private String squadName;
    private List<SquadMember> members;
    private List<BattleRecord> battleRecords;
    private int totalBattles;
    private int victories;
    private int defeats;
    private long lastBattleTime;

    public SquadDetails() {
        this.squadName = "Default Squad";
        this.members = new ArrayList<>();
        this.battleRecords = new ArrayList<>();
        this.totalBattles = 0;
        this.victories = 0;
        this.defeats = 0;
        this.lastBattleTime = 0;

        // Add default squad member
        this.members.add(new SquadMember("Commander", "Leader", 100));
    }

    public SquadDetails(String squadName) {
        this();
        this.squadName = squadName;
    }


    public void addMember(SquadMember member) {
        this.members.add(member);
    }


    public void recordBattle(boolean victory, int xpEarned) {
        BattleRecord record = new BattleRecord(victory, xpEarned);
        this.battleRecords.add(record);
        this.totalBattles++;

        if (victory) {
            this.victories++;
        } else {
            this.defeats++;
        }

        this.lastBattleTime = System.currentTimeMillis();
    }


    public double getWinRate() {
        if (totalBattles == 0) return 0.0;
        return (double) victories / totalBattles * 100.0;
    }


    public int getTotalSquadXP() {
        return members.stream()
                .mapToInt(SquadMember::getXp)
                .sum();
    }

    // Getters and setters
    public String getSquadName() { return squadName; }
    public void setSquadName(String squadName) { this.squadName = squadName; }

    public List<SquadMember> getMembers() { return members; }
    public void setMembers(List<SquadMember> members) { this.members = members; }

    public List<BattleRecord> getBattleRecords() { return battleRecords; }
    public void setBattleRecords(List<BattleRecord> battleRecords) { this.battleRecords = battleRecords; }

    public int getTotalBattles() { return totalBattles; }
    public void setTotalBattles(int totalBattles) { this.totalBattles = totalBattles; }

    public int getVictories() { return victories; }
    public void setVictories(int victories) { this.victories = victories; }

    public int getDefeats() { return defeats; }
    public void setDefeats(int defeats) { this.defeats = defeats; }

    public long getLastBattleTime() { return lastBattleTime; }
    public void setLastBattleTime(long lastBattleTime) { this.lastBattleTime = lastBattleTime; }

    public static class SquadMember implements Serializable {
        private String name;
        private String role;
        private int xp;

        public SquadMember(String name, String role, int xp) {
            this.name = name;
            this.role = role;
            this.xp = xp;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public int getXp() { return xp; }
        public void setXp(int xp) { this.xp = xp; }
    }

    public static class BattleRecord implements Serializable {
        private boolean victory;
        private int xpEarned;
        private long timestamp;

        public BattleRecord(boolean victory, int xpEarned) {
            this.victory = victory;
            this.xpEarned = xpEarned;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public boolean isVictory() { return victory; }
        public void setVictory(boolean victory) { this.victory = victory; }

        public int getXpEarned() { return xpEarned; }
        public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}


