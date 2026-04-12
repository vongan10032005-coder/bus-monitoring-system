package com.busmonitor.server3.model;

import java.util.ArrayList;
import java.util.List;

public class BusToken {
    private int currentPassengers;
    private int totalRounds;
    private double totalRevenue;
    private List<String> activeServers;
    private String lastStation;
    private String currentLeader;
    private long timestamp;

    public BusToken() {
        this.activeServers = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public int getCurrentPassengers() { return currentPassengers; }
    public void setCurrentPassengers(int p) { this.currentPassengers = p; }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int r) { this.totalRounds = r; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double r) { this.totalRevenue = r; }
    public List<String> getActiveServers() { return activeServers; }
    public void setActiveServers(List<String> s) { this.activeServers = s; }
    public String getLastStation() { return lastStation; }
    public void setLastStation(String s) { this.lastStation = s; }
    public String getCurrentLeader() { return currentLeader; }
    public void setCurrentLeader(String l) { this.currentLeader = l; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }
}
