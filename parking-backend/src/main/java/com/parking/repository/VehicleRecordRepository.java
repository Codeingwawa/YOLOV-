package com.parking.repository;

import com.parking.entity.VehicleRecord;
import com.parking.entity.VehicleRecord.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRecordRepository extends JpaRepository<VehicleRecord, Long> {
    
    Optional<VehicleRecord> findByPlateNumberAndStatus(String plateNumber, VehicleStatus status);
    
    Page<VehicleRecord> findByStatus(VehicleStatus status, Pageable pageable);
    
    List<VehicleRecord> findByEntryTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(v) FROM VehicleRecord v WHERE v.status = :status")
    long countByStatus(@Param("status") VehicleStatus status);
    
    @Query("SELECT SUM(v.fee) FROM VehicleRecord v WHERE v.exitTime BETWEEN :start AND :end")
    Double sumFeeByExitTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(v) FROM VehicleRecord v WHERE v.entryTime BETWEEN :start AND :end")
    Long countByEntryTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT v.plateNumber, COUNT(v) as count FROM VehicleRecord v GROUP BY v.plateNumber ORDER BY count DESC")
    List<Object[]> findTopFrequentVehicles(Pageable pageable);
}
