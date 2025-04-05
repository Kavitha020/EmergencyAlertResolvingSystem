package com.eams.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private String status;
    private double latitude;
    private double longitude;
    private String location; // New field for user-provided location
    private String anonymousToken;
    @ManyToOne
    private User user;
    @ManyToOne
    private User responder;
    @ManyToOne
    private AlertType alertType;

    public Alert() {}

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAnonymousToken() { return anonymousToken; }
    public void setAnonymousToken(String anonymousToken) { this.anonymousToken = anonymousToken; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getResponder() { return responder; }
    public void setResponder(User responder) { this.responder = responder; }
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
}