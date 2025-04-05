package com.eams.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eams.model.Alert;
import com.eams.model.User;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUser(User user);
    List<Alert> findByResponder(User responder);
    Alert findByAnonymousToken(String token);
}

