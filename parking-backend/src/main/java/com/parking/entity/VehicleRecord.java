package com.parking.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicle_record")
public class VehicleRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 20)
    private String plateNumber;
    
    @Column(nullable = false)
    private LocalDateTime entryTime;
    
    private LocalDateTime exitTime;
    
    @Column(length = 500)
    private String entryImage;
    
    @Column(length = 500)
    private String exitImage;
    
    @Enumerated(EnumType.STRING)
    private VehicleStatus status;
    
    private Double fee;
    
    @Column(length = 50)
    private String vehicleType;
    
    @Column(length = 20)
    private String plateColor;
    
    private Integer parkingDuration;
    
    public enum VehicleStatus {
        PARKED,
        EXITED
    }
}
