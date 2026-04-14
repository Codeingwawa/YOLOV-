package com.parking.service;

import com.parking.dto.PlateRecognitionResult;
import com.parking.entity.VehicleRecord;
import com.parking.entity.VehicleRecord.VehicleStatus;
import com.parking.repository.VehicleRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleRecordService {
    
    private final VehicleRecordRepository vehicleRecordRepository;
    private final RestTemplate restTemplate;
    
    @Value("${plate-recognition.service-url}")
    private String recognitionServiceUrl;
    
    @Value("${parking.hourly-rate}")
    private Double hourlyRate;
    
    @Value("${parking.max-daily-fee}")
    private Double maxDailyFee;
    
    private final Path uploadPath = Paths.get("uploads");
    
    @Transactional
    public VehicleRecord vehicleEntry(MultipartFile image) throws IOException {
        String imagePath = saveImage(image);
        
        PlateRecognitionResult recognitionResult = recognizePlate(imagePath);
        
        Optional<VehicleRecord> existingRecord = vehicleRecordRepository
            .findByPlateNumberAndStatus(recognitionResult.getPlateNumber(), VehicleStatus.PARKED);
        
        if (existingRecord.isPresent()) {
            throw new RuntimeException("车辆已在停车场内：" + recognitionResult.getPlateNumber());
        }
        
        VehicleRecord record = new VehicleRecord();
        record.setPlateNumber(recognitionResult.getPlateNumber());
        record.setEntryTime(LocalDateTime.now());
        record.setEntryImage(imagePath);
        record.setStatus(VehicleStatus.PARKED);
        record.setVehicleType(recognitionResult.getPlateType());
        record.setPlateColor(recognitionResult.getPlateColor());
        
        return vehicleRecordRepository.save(record);
    }
    
    @Transactional
    public VehicleRecord vehicleExit(MultipartFile image) throws IOException {
        String imagePath = saveImage(image);
        
        PlateRecognitionResult recognitionResult = recognizePlate(imagePath);
        
        VehicleRecord record = vehicleRecordRepository
            .findByPlateNumberAndStatus(recognitionResult.getPlateNumber(), VehicleStatus.PARKED)
            .orElseThrow(() -> new RuntimeException("未找到入场记录：" + recognitionResult.getPlateNumber()));
        
        record.setExitTime(LocalDateTime.now());
        record.setExitImage(imagePath);
        record.setStatus(VehicleStatus.EXITED);
        
        Duration duration = Duration.between(record.getEntryTime(), record.getExitTime());
        int hours = (int) Math.ceil(duration.toMinutes() / 60.0);
        record.setParkingDuration((int) duration.toMinutes());
        
        double fee = Math.min(hours * hourlyRate, maxDailyFee);
        record.setFee(fee);
        
        return vehicleRecordRepository.save(record);
    }
    
    public PlateRecognitionResult recognizePlate(String imagePath) {
        try {
            String url = recognitionServiceUrl + "/api/recognize";
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("image", new org.springframework.core.io.FileSystemResource(imagePath));
            
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity =
                new org.springframework.http.HttpEntity<>(body, headers);
            
            org.springframework.http.ResponseEntity<PlateRecognitionResult> response = 
                restTemplate.postForEntity(url, requestEntity, PlateRecognitionResult.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("车牌识别服务调用失败", e);
            throw new RuntimeException("车牌识别服务调用失败: " + e.getMessage());
        }
    }
    
    private String saveImage(MultipartFile file) throws IOException {
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        
        return filePath.toString();
    }
    
    public Page<VehicleRecord> getVehicleRecords(Pageable pageable) {
        return vehicleRecordRepository.findAll(pageable);
    }
    
    public Page<VehicleRecord> getParkedVehicles(Pageable pageable) {
        return vehicleRecordRepository.findByStatus(VehicleStatus.PARKED, pageable);
    }
    
    public Optional<VehicleRecord> getRecordById(Long id) {
        return vehicleRecordRepository.findById(id);
    }
    
    public List<VehicleRecord> getRecordsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return vehicleRecordRepository.findByEntryTimeBetween(start, end);
    }
}
