package com.eams.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eams.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.role = 'RESPONDER' AND u.isAvailable = true AND EXISTS (SELECT 1 FROM u.alertTypes at WHERE at.id = ?1)")
    List<User> findAvailableRespondersByAlertType(Long alertTypeId);
}
