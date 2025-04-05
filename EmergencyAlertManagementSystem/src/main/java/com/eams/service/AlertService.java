package com.eams.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.eams.model.Alert;
import com.eams.model.User;
import com.eams.repository.AlertRepository;
import com.eams.repository.UserRepository;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private UserRepository userRepository;

    private static final double EARTH_RADIUS = 6371; // km

    @Value("${google.maps.api.key}")
    private String googleApiKey;

    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    private final GeoApiContext geoApiContext;

    @Autowired
    public AlertService(@Value("${google.maps.api.key}") String googleApiKey) {
        this.googleApiKey = googleApiKey;
        this.geoApiContext = new GeoApiContext.Builder()
            .apiKey(googleApiKey)
            .build();
    }

    @Autowired
    public AlertService(@Value("${google.maps.api.key}") String googleApiKey,
                        @Value("${twilio.account.sid}") String twilioAccountSid,
                        @Value("${twilio.auth.token}") String twilioAuthToken,
                        @Value("${twilio.phone.number}") String twilioPhoneNumber) {
        this.googleApiKey = googleApiKey;
        this.twilioAccountSid = twilioAccountSid;
        this.twilioAuthToken = twilioAuthToken;
        this.twilioPhoneNumber = twilioPhoneNumber;
        Twilio.init(twilioAccountSid, twilioAuthToken);
        this.geoApiContext = new GeoApiContext.Builder()
            .apiKey(googleApiKey)
            .build();
    }

    public Alert createAlert(Alert alert, User user) {
        alert.setStatus("PENDING");
        if (user != null) {
            alert.setUser(user);
        } else {
            alert.setAnonymousToken(UUID.randomUUID().toString());
        }
        // Ensure alertType is set (assumed passed via controller)
        if (alert.getAlertType() == null) {
            throw new IllegalArgumentException("Alert type is required");
        }

        // Convert location string to coordinates
        try {
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, alert.getLocation()).await();
            if (results.length > 0) {
                alert.setLatitude(results[0].geometry.location.lat);
                alert.setLongitude(results[0].geometry.location.lng);
            } else {
                throw new IllegalArgumentException("Unable to geocode location: " + alert.getLocation());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Geocoding failed: " + e.getMessage());
        }

        alert = alertRepository.save(alert);
        assignNearestResponder(alert); // Assign responder immediately
        return alert;
    }

    private void assignNearestResponder(Alert alert) {
        if (alert.getAlertType() == null) return;

        // Fetch available responders based on alert type
        List<User> responders = userRepository.findAvailableRespondersByAlertType(alert.getAlertType().getId());
        User nearestResponder = null;
        double minDistance = Double.MAX_VALUE;

        for (User responder : responders) {
            double distance = calculateDistance(
                alert.getLatitude(), alert.getLongitude(),
                responder.getLatitude(), responder.getLongitude()
            );
            if (distance < minDistance) {
                minDistance = distance;
                nearestResponder = responder;
            }
        }

        if (nearestResponder != null) {
            alert.setResponder(nearestResponder);
            alert.setStatus("ASSIGNED");
            nearestResponder.setAvailable(false);
            userRepository.save(nearestResponder);
            alertRepository.save(alert);
            sendSmsNotification(nearestResponder, alert); // Send SMS notification
        } else {
            // Log or handle no available responders
            System.err.println("No available responders for alert ID: " + alert.getId());
        }
    }

    private void sendSmsNotification(User responder, Alert alert) {
        if (responder.getPhoneNumber() == null || responder.getPhoneNumber().isEmpty()) {
            System.err.println("No phone number for responder: " + responder.getName());
            return;
        }

        String messageBody = String.format(
            "Emergency Alert Assigned!\nType: %s\nLocation: %s\nDescription: %s\nPlease log in to respond.",
            alert.getAlertType().getName(), alert.getLocation(), alert.getDescription()
        );

        try {
            Message.creator(
                new PhoneNumber(responder.getPhoneNumber()),
                new PhoneNumber(twilioPhoneNumber),
                messageBody
            ).create();
            System.out.println("SMS sent to " + responder.getPhoneNumber());
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public void acceptAlert(Long alertId, User responder) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        if (alert.getResponder() != null && alert.getResponder().getId().equals(responder.getId())) {
            alert.setStatus("ACCEPTED");
            alertRepository.save(alert);
        } else {
            throw new IllegalArgumentException("You are not assigned to this alert");
        }
    }

    public void rejectAlert(Long alertId, User responder) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        if (alert.getResponder() != null && alert.getResponder().getId().equals(responder.getId())) {
            alert.setStatus("REJECTED");
            alert.getResponder().setAvailable(true);
            userRepository.save(alert.getResponder());
            alert.setResponder(null);
            alertRepository.save(alert);
            assignNearestResponder(alert); // Reassign if possible
        } else {
            throw new IllegalArgumentException("You are not assigned to this alert");
        }
    }

    public void resolveAlert(Long alertId, User responder) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        if (alert.getResponder() != null && alert.getResponder().getId().equals(responder.getId())) {
            alert.setStatus("RESOLVED");
            alert.getResponder().setAvailable(true);
            userRepository.save(alert.getResponder());
            alertRepository.save(alert);
        } else {
            throw new IllegalArgumentException("You are not assigned to this alert");
        }
    }

    public void adminAssignAlert(Long alertId, Long responderId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        User responder = userRepository.findById(responderId)
            .orElseThrow(() -> new IllegalArgumentException("Responder not found"));
        if ("RESPONDER".equals(responder.getRole()) && responder.isAvailable()) {
            if (alert.getResponder() != null) {
                alert.getResponder().setAvailable(true);
                userRepository.save(alert.getResponder());
            }
            alert.setResponder(responder);
            alert.setStatus("ASSIGNED");
            responder.setAvailable(false);
            userRepository.save(responder);
            alertRepository.save(alert);
            sendSmsNotification(responder, alert); // Send SMS notification
        } else {
            throw new IllegalArgumentException("Responder is unavailable or not a valid role");
        }
    }

    public List<Alert> getUserAlerts(User user) {
        return alertRepository.findByUser(user);
    }

    public List<Alert> getResponderAlerts(User responder) {
        return alertRepository.findByResponder(responder);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public Alert getAlertByToken(String token) {
        return alertRepository.findByAnonymousToken(token);
    }
}