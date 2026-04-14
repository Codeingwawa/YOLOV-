package com.parking.service;

import com.parking.dto.PlateRecognitionResult;
import com.parking.entity.VehicleInfo;
import com.parking.entity.VehicleInfo.VehicleCategory;
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
public class VehicleInfoService {
    
    private final VehicleInfoRepository vehicleInfoRepository;
    private final RestTemplate restTemplate;
    
    @Value("${plate-recognition.service-url}")
    private String recognitionServiceUrl;
    
    private final Path uploadPath = Paths.get("uploads/vehicles");
    
    @Transactional
    public VehicleInfo detectAndSave(MultipartFile image, String category) throws IOException {
        String imagePath = saveImage(image);
        
        PlateRecognitionResult recognitionResult = recognizePlate(imagePath);
        
        if (recognitionResult == null || recognitionResult.getPlateNumber() == null) {
            throw new RuntimeException("无法识别车牌");
        }
        
        Optional<VehicleInfo> existing = vehicleInfoRepository.findByPlateNumber(recognitionResult.getPlateNumber());
        
        VehicleInfo vehicleInfo;
        if (existing.isPresent()) {
            vehicleInfo = existing.get();
            vehicleInfo.setImageUrl(imagePath);
            vehicleInfo.setConfidence(recognitionResult.getConfidence());
            vehicleInfo.setUpdatedAt(LocalDateTime.now());
        } else {
            vehicleInfo = new VehicleInfo();
            vehicleInfo.setPlateNumber(recognitionResult.getPlateNumber());
            vehicleInfo.setPlateColor(recognitionResult.getPlateColor());
            vehicleInfo.setVehicleType(recognitionResult.getPlateType());
            vehicleInfo.setImageUrl(imagePath);
            vehicleInfo.setConfidence(recognitionResult.getConfidence());
            
            if (category != null && !category.isEmpty()) {
                vehicleInfo.setCategory(VehicleCategory.valueOf(category));
            } else {
                vehicleInfo.setCategory(determineCategory(recognitionResult.getPlateColor()));
            }
        }
        
        return vehicleInfoRepository.save(vehicleInfo);
    }
    
    private VehicleCategory determineCategory(String plateColor) {
        if (plateColor == null) return VehicleCategory.OTHER;
        if (plateColor.contains("绿")) return VehicleCategory.NEW_ENERGY;
        return VehicleCategory.SEDAN;
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
    
    public Page<VehicleInfo> getAllVehicles(Pageable pageable) {
        return vehicleInfoRepository.findAll(pageable);
    }
    
    public Page<VehicleInfo> searchVehicles(String plateNumber, String vehicleType, 
                                            VehicleCategory category, String plateColor, 
                                            Pageable pageable) {
        return vehicleInfoRepository.searchVehicles(plateNumber, vehicleType, category, plateColor, pageable);
    }
    
    public Optional<VehicleInfo> getByPlateNumber(String plateNumber) {
        return vehicleInfoRepository.findByPlateNumber(plateNumber);
    }
    
    public Optional<VehicleInfo> getById(Long id) {
        return vehicleInfoRepository.findById(id);
    }
    
    @Transactional
    public VehicleInfo updateVehicle(Long id, VehicleInfo updatedInfo) {
        return vehicleInfoRepository.findById(id).map(vehicle -> {
            if (updatedInfo.getVehicleType() != null) {
                vehicle.setVehicleType(updatedInfo.getVehicleType());
            }
            if (updatedInfo.getVehicleBrand() != null) {
                vehicle.setVehicleBrand(updatedInfo.getVehicleBrand());
            }
            if (updatedInfo.getVehicleColor() != null) {
                vehicle.setVehicleColor(updatedInfo.getVehicleColor());
            }
            if (updatedInfo.getCategory() != null) {
                vehicle.setCategory(updatedInfo.getCategory());
            }
            if (updatedInfo.getRemarks() != null) {
                vehicle.setRemarks(updatedInfo.getRemarks());
            }
            return vehicleInfoRepository.save(vehicle);
        }).orElseThrow(() -> new RuntimeException("车辆不存在: " + id));
    }
    
    @Transactional
    public void deleteVehicle(Long id) {
        if (!vehicleInfoRepository.existsById(id)) {
            throw new RuntimeException("车辆不存在: " + id);
        }
        vehicleInfoRepository.deleteById(id);
    }
    
    @Transactional
    public void deleteByPlateNumber(String plateNumber) {
        vehicleInfoRepository.findByPlateNumber(plateNumber)
            .ifPresent(vehicle -> vehicleInfoRepository.delete(vehicle));
    }
    
    public List<Object[]> getVehicleTypeStats() {
        return vehicleInfoRepository.countByVehicleType();
    }
    
    public List<Object[]> getCategoryStats() {
        return vehicleInfoRepository.countByCategory();
    }
}
