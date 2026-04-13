package com.busmonitor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BusToken {
    private int currentPassengers;
    private int totalRounds;
    private double totalRevenue;
    private List<String> activeServers;
    private String lastStation;
    private String currentLeader;
    private int epoch;
    private String targetStation;
    private List<Map<String, Object>> roundEntries;

    public BusToken() {
        this.activeServers  = new ArrayList<>();
        this.roundEntries   = new ArrayList<>();
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
    public int getEpoch() { return epoch; }
    public void setEpoch(int e) { this.epoch = e; }
    public String getTargetStation() { return targetStation; }
    public void setTargetStation(String t) { this.targetStation = t; }
    public List<Map<String, Object>> getRoundEntries() { return roundEntries; }
    public void setRoundEntries(List<Map<String, Object>> r) { this.roundEntries = r; }
}
