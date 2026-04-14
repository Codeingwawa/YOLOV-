package com.parking.dto;

import lombok.Data;

@Data
public class VehicleExitRequest {
    private String plateNumber;
    private String exitImage;
}
