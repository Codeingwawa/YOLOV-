package com.parking.dto;

import lombok.Data;

@Data
public class PlateRecognitionResult {
    private String plateNumber;
    private Double confidence;
    private String plateColor;
    private String plateType;
    private String vehicleType;
    private String vehicleCategory;
    private Double vehicleConfidence;
}
