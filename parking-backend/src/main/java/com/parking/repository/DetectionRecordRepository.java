package com.parking.repository;

import com.parking.entity.DetectionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectionRecordRepository extends JpaRepository<DetectionRecord, Long> {
    
    Page<DetectionRecord> findBySavedFalse(Pageable pageable);
    
    List<DetectionRecord> findByPlateNumber(String plateNumber);
    
    boolean existsByPlateNumberAndSavedTrue(String plateNumber);
}
