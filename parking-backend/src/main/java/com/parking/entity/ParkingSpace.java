package com.parking.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "parking_space")
public class ParkingSpace {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 20)
    private String spaceNumber;
    
    @Enumerated(EnumType.STRING)
    private SpaceStatus status;
    
    @Enumerated(EnumType.STRING)
    private SpaceType type;
    
    private LocalDateTime occupiedTime;
    
    @Column(length = 20)
    private String currentPlate;
    
    public enum SpaceStatus {
        AVAILABLE,
        OCCUPIED,
        RESERVED,
        MAINTENANCE
    }
    
    public enum SpaceType {
        STANDARD,
        LARGE,
        DISABLED,
        VIP
    }
}
