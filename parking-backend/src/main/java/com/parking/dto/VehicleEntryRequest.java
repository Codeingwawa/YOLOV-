package com.parking.dto;

import lombok.Data;

@Data
public class VehicleEntryRequest {
    private String plateNumber;
    private String entryImage;
    private String vehicleType;
    private String plateColor;
}
