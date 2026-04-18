package com.parking.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "detection_record")
public class DetectionRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 20)
    private String plateNumber;
    
    @Column(length = 20)
    private String plateColor;
    
    @Column(length = 50)
    private String plateType;
    
    @Column(length = 20)
    private String vehicleType;
    
    private Double confidence;
    
    @Column(length = 20)
    private String source;
    
    @Column(nullable = false)
    private LocalDateTime detectionTime;
    
    @Column(length = 500)
    private String imageUrl;
    
    private Boolean saved;
    
    @PrePersist
    protected void onCreate() {
        detectionTime = LocalDateTime.now();
        if (saved == null) {
            saved = false;
        }
    }
}
