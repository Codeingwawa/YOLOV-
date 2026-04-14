package com.parking.service;

import com.parking.dto.PlateRecognitionResult;
import com.parking.entity.DetectionRecord;
import com.parking.entity.ParkingSpace;
import com.parking.entity.VehicleInfo;
import com.parking.entity.VehicleInfo.VehicleCategory;
import com.parking.repository.DetectionRecordRepository;
import com.parking.repository.ParkingSpaceRepository;
import com.parking.repository.VehicleInfoRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionRecordService {
    
    private final DetectionRecordRepository detectionRecordRepository;
    private final VehicleInfoRepository vehicleInfoRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final RestTemplate restTemplate;
    
    @Value("${plate-recognition.service-url}")
    private String recognitionServiceUrl;
    
    private final Path uploadPath = Paths.get("uploads/detections");
    
    @Transactional
    public DetectionRecord saveDetectionRecord(String plateNumber, String plateColor, 
                                                String plateType, Double confidence, 
                                                String source, MultipartFile image) throws IOException {
        
        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = saveImage(image);
        }
        
        DetectionRecord record = new DetectionRecord();
        record.setPlateNumber(plateNumber);
        record.setPlateColor(plateColor);
        record.setPlateType(plateType);
        record.setConfidence(confidence);
        record.setSource(source);
        record.setImageUrl(imagePath);
        record.setSaved(false);
        
        return detectionRecordRepository.save(record);
    }
    
    @Transactional
    public DetectionRecord detectAndSaveRecord(MultipartFile image, String source) throws IOException {
        String imagePath = saveImage(image);
        
        PlateRecognitionResult recognitionResult = recognizePlate(imagePath);
        
        if (recognitionResult == null || recognitionResult.getPlateNumber() == null) {
            throw new RuntimeException("无法识别车牌");
        }
        
        DetectionRecord record = new DetectionRecord();
        record.setPlateNumber(recognitionResult.getPlateNumber());
        record.setPlateColor(recognitionResult.getPlateColor());
        record.setPlateType(recognitionResult.getPlateType());
        record.setConfidence(recognitionResult.getConfidence());
        record.setSource(source);
        record.setImageUrl(imagePath);
        record.setSaved(false);
        
        return detectionRecordRepository.save(record);
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
        
        return "uploads/detections/" + fileName;
    }
    
    public Page<DetectionRecord> getAllRecords(Pageable pageable) {
        return detectionRecordRepository.findAll(pageable);
    }
    
    public Page<DetectionRecord> getUnsavedRecords(Pageable pageable) {
        return detectionRecordRepository.findBySavedFalse(pageable);
    }
    
    public List<DetectionRecord> getRecordsByPlateNumber(String plateNumber) {
        return detectionRecordRepository.findByPlateNumber(plateNumber);
    }
    
    @Transactional
    public VehicleInfo saveRecordToVehicleInfo(Long recordId, String category) {
        DetectionRecord record = detectionRecordRepository.findById(recordId)
            .orElseThrow(() -> new RuntimeException("检测记录不存在: " + recordId));
        
        Optional<VehicleInfo> existing = vehicleInfoRepository.findByPlateNumber(record.getPlateNumber());
        
        VehicleInfo vehicleInfo;
        if (existing.isPresent()) {
            vehicleInfo = existing.get();
            if (record.getImageUrl() != null) {
                vehicleInfo.setImageUrl(record.getImageUrl());
            }
            vehicleInfo.setConfidence(record.getConfidence());
        } else {
            vehicleInfo = new VehicleInfo();
            vehicleInfo.setPlateNumber(record.getPlateNumber());
            vehicleInfo.setPlateColor(record.getPlateColor());
            vehicleInfo.setVehicleType(record.getPlateType());
            vehicleInfo.setImageUrl(record.getImageUrl());
            vehicleInfo.setConfidence(record.getConfidence());
            
            if (category != null && !category.isEmpty()) {
                vehicleInfo.setCategory(VehicleCategory.valueOf(category));
            } else {
                vehicleInfo.setCategory(determineCategory(record.getPlateColor()));
            }
            
            ParkingSpace availableSpace = findAvailableSpace(vehicleInfo.getCategory());
            if (availableSpace != null) {
                availableSpace.setStatus(ParkingSpace.SpaceStatus.OCCUPIED);
                availableSpace.setCurrentPlate(record.getPlateNumber());
                availableSpace.setOccupiedTime(LocalDateTime.now());
                parkingSpaceRepository.save(availableSpace);
                
                vehicleInfo.setParkingSpaceId(availableSpace.getId());
                vehicleInfo.setParkingSpaceNumber(availableSpace.getSpaceNumber());
                vehicleInfo.setParkingStartTime(LocalDateTime.now());
                vehicleInfo.setParkingEndTime(LocalDateTime.now().plusDays(1));
                
                log.info("车辆 {} 已分配车位 {}", record.getPlateNumber(), availableSpace.getSpaceNumber());
            } else {
                log.warn("没有可用车位，车辆 {} 未分配车位", record.getPlateNumber());
            }
        }
        
        VehicleInfo saved = vehicleInfoRepository.save(vehicleInfo);
        
        record.setSaved(true);
        detectionRecordRepository.save(record);
        
        return saved;
    }
    
    private ParkingSpace findAvailableSpace(VehicleCategory category) {
        List<ParkingSpace> availableSpaces = parkingSpaceRepository.findByStatus(ParkingSpace.SpaceStatus.AVAILABLE);
        
        if (availableSpaces.isEmpty()) {
            return null;
        }
        
        if (category == VehicleCategory.NEW_ENERGY) {
            Optional<ParkingSpace> vipSpace = availableSpaces.stream()
                .filter(s -> s.getType() == ParkingSpace.SpaceType.VIP)
                .findFirst();
            if (vipSpace.isPresent()) {
                return vipSpace.get();
            }
        }
        
        if (category == VehicleCategory.TRUCK || category == VehicleCategory.BUS) {
            Optional<ParkingSpace> largeSpace = availableSpaces.stream()
                .filter(s -> s.getType() == ParkingSpace.SpaceType.LARGE)
                .findFirst();
            if (largeSpace.isPresent()) {
                return largeSpace.get();
            }
        }
        
        return availableSpaces.stream()
            .filter(s -> s.getType() == ParkingSpace.SpaceType.STANDARD)
            .findFirst()
            .orElse(availableSpaces.get(0));
    }
    
    private VehicleCategory determineCategory(String plateColor) {
        if (plateColor == null) return VehicleCategory.OTHER;
        if (plateColor.contains("绿")) return VehicleCategory.NEW_ENERGY;
        return VehicleCategory.SEDAN;
    }
    
    @Transactional
    public void deleteRecord(Long id) {
        detectionRecordRepository.deleteById(id);
    }
}
