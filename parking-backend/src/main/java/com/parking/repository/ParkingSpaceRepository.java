package com.parking.repository;

import com.parking.entity.ParkingSpace;
import com.parking.entity.ParkingSpace.SpaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {
    
    List<ParkingSpace> findByStatus(SpaceStatus status);
    
    Optional<ParkingSpace> findBySpaceNumber(String spaceNumber);
    
    Optional<ParkingSpace> findByCurrentPlate(String plateNumber);
    
    @Query("SELECT COUNT(p) FROM ParkingSpace p WHERE p.status = :status")
    long countByStatus(@Param("status") SpaceStatus status);
    
    @Query("SELECT COUNT(p) FROM ParkingSpace p")
    long countTotal();
}
