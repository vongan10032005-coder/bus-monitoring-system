package com.busmonitor.server4.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "round_logs")
public class RoundLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int roundNumber;
    private int passengersBoarded;
    private int passengersAlighted;
    private double revenue;
    private String stationName;
    private LocalDateTime timestamp;
    public RoundLog() {}
    public RoundLog(int roundNumber, int passengersBoarded, int passengersAlighted, double revenue, String stationName) {
        this.roundNumber = roundNumber; this.passengersBoarded = passengersBoarded;
        this.passengersAlighted = passengersAlighted; this.revenue = revenue;
        this.stationName = stationName; this.timestamp = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int r) { this.roundNumber = r; }
    public int getPassengersBoarded() { return passengersBoarded; }
    public void setPassengersBoarded(int b) { this.passengersBoarded = b; }
    public int getPassengersAlighted() { return passengersAlighted; }
    public void setPassengersAlighted(int a) { this.passengersAlighted = a; }
    public double getRevenue() { return revenue; }
    public void setRevenue(double r) { this.revenue = r; }
    public String getStationName() { return stationName; }
    public void setStationName(String s) { this.stationName = s; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime t) { this.timestamp = t; }
}
