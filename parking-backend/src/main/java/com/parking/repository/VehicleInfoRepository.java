package com.parking.repository;

import com.parking.entity.VehicleInfo;
import com.parking.entity.VehicleInfo.VehicleCategory;
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
public interface VehicleInfoRepository extends JpaRepository<VehicleInfo, Long> {
    
    Optional<VehicleInfo> findByPlateNumber(String plateNumber);
    
    boolean existsByPlateNumber(String plateNumber);
    
    Page<VehicleInfo> findByVehicleType(String vehicleType, Pageable pageable);
    
    Page<VehicleInfo> findByCategory(VehicleCategory category, Pageable pageable);
    
    Page<VehicleInfo> findByPlateColor(String plateColor, Pageable pageable);
    
    @Query("SELECT v FROM VehicleInfo v WHERE " +
           "(:plateNumber IS NULL OR v.plateNumber LIKE %:plateNumber%) AND " +
           "(:vehicleType IS NULL OR v.vehicleType = :vehicleType) AND " +
           "(:category IS NULL OR v.category = :category) AND " +
           "(:plateColor IS NULL OR v.plateColor = :plateColor)")
    Page<VehicleInfo> searchVehicles(
        @Param("plateNumber") String plateNumber,
        @Param("vehicleType") String vehicleType,
        @Param("category") VehicleCategory category,
        @Param("plateColor") String plateColor,
        Pageable pageable
    );
    
    List<VehicleInfo> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(v) FROM VehicleInfo v")
    long countTotal();
    
    @Query("SELECT v.vehicleType, COUNT(v) FROM VehicleInfo v GROUP BY v.vehicleType")
    List<Object[]> countByVehicleType();
    
    @Query("SELECT v.category, COUNT(v) FROM VehicleInfo v GROUP BY v.category")
    List<Object[]> countByCategory();
    
    List<VehicleInfo> findByParkingEndTimeBeforeAndParkingSpaceIdIsNotNull(LocalDateTime parkingEndTime);
    
    List<VehicleInfo> findByParkingSpaceIdIsNotNull();
}
