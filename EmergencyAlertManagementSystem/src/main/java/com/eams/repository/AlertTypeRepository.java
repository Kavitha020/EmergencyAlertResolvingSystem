package com.eams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.eams.model.AlertType;

public interface AlertTypeRepository extends JpaRepository<AlertType, Long> {
	
}
