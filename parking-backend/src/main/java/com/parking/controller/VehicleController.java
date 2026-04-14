package com.parking.controller;

import com.parking.dto.PlateRecognitionResult;
import com.parking.entity.VehicleRecord;
import com.parking.service.VehicleRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
@Tag(name = "车辆管理", description = "车辆进出管理接口")
public class VehicleController {
    
    private final VehicleRecordService vehicleRecordService;
    
    @PostMapping("/entry")
    @Operation(summary = "车辆入场", description = "上传入场图片进行车牌识别并记录入场")
    public ResponseEntity<VehicleRecord> vehicleEntry(@RequestParam("image") MultipartFile image) throws IOException {
        VehicleRecord record = vehicleRecordService.vehicleEntry(image);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/exit")
    @Operation(summary = "车辆出场", description = "上传出场图片进行车牌识别并计算费用")
    public ResponseEntity<VehicleRecord> vehicleExit(@RequestParam("image") MultipartFile image) throws IOException {
        VehicleRecord record = vehicleRecordService.vehicleExit(image);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/recognize")
    @Operation(summary = "车牌识别", description = "仅进行车牌识别，不记录入场出场")
    public ResponseEntity<PlateRecognitionResult> recognizePlate(@RequestParam("image") MultipartFile image) throws IOException {
        String imagePath = saveTempImage(image);
        PlateRecognitionResult result = vehicleRecordService.recognizePlate(imagePath);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/records")
    @Operation(summary = "获取车辆记录", description = "分页获取所有车辆进出记录")
    public ResponseEntity<Page<VehicleRecord>> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleRecord> records = vehicleRecordService.getVehicleRecords(
            PageRequest.of(page, size, Sort.by("entryTime").descending())
        );
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/parked")
    @Operation(summary = "获取在场车辆", description = "获取当前在停车场内的车辆")
    public ResponseEntity<Page<VehicleRecord>> getParkedVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleRecord> records = vehicleRecordService.getParkedVehicles(
            PageRequest.of(page, size, Sort.by("entryTime").descending())
        );
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/records/{id}")
    @Operation(summary = "获取记录详情", description = "根据ID获取车辆记录详情")
    public ResponseEntity<VehicleRecord> getRecordById(@PathVariable Long id) {
        return vehicleRecordService.getRecordById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/records/time-range")
    @Operation(summary = "按时间范围查询", description = "获取指定时间范围内的车辆记录")
    public ResponseEntity<List<VehicleRecord>> getRecordsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<VehicleRecord> records = vehicleRecordService.getRecordsByTimeRange(start, end);
        return ResponseEntity.ok(records);
    }
    
    private String saveTempImage(MultipartFile file) throws IOException {
        java.nio.file.Path tempPath = java.nio.file.Files.createTempFile("plate_", "_" + file.getOriginalFilename());
        file.transferTo(tempPath.toFile());
        return tempPath.toString();
    }
}
