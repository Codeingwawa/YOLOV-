package com.parking.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleRecordResponse {
    private Long id;
    private String plateNumber;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String entryImage;
    private String exitImage;
    private String status;
    private Double fee;
    private String vehicleType;
    private String plateColor;
    private Integer parkingDuration;
}
