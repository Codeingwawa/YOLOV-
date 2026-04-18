package com.parking.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicle_info")
public class VehicleInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 20, unique = true)
    private String plateNumber;
    
    @Column(length = 20)
    private String plateColor;
    
    @Column(length = 50)
    private String vehicleType;
    
    @Column(length = 50)
    private String vehicleBrand;
    
    @Column(length = 20)
    private String vehicleColor;
    
    @Column(length = 500)
    private String imageUrl;
    
    @Column(length = 500)
    private String thumbnailUrl;
    
    private Double confidence;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Enumerated(EnumType.STRING)
    private VehicleCategory category;
    
    private String remarks;
    
    private Long parkingSpaceId;
    
    private String parkingSpaceNumber;
    
    private LocalDateTime parkingStartTime;
    
    private LocalDateTime parkingEndTime;
    
    public enum VehicleCategory {
        NEW_ENERGY,
        FUEL,
        TRUCK,
        BUS,
        OTHER
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
