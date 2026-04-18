package com.parking.controller;

import com.parking.entity.DetectionRecord;
import com.parking.entity.VehicleInfo;
import com.parking.service.DetectionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/detection-record")
@RequiredArgsConstructor
@Tag(name = "检测记录管理", description = "检测记录增删改查接口")
public class DetectionRecordController {
    
    private final DetectionRecordService detectionRecordService;
    
    @PostMapping("/detect")
    @Operation(summary = "检测并保存记录", description = "上传图片进行车牌识别并保存检测记录")
    public ResponseEntity<DetectionRecord> detectAndSave(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "source", defaultValue = "图片检测") String source) throws IOException {
        DetectionRecord record = detectionRecordService.detectAndSaveRecord(image, source);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/add")
    @Operation(summary = "添加检测记录", description = "手动添加检测记录")
    public ResponseEntity<DetectionRecord> addRecord(
            @RequestParam("plateNumber") String plateNumber,
            @RequestParam(value = "plateColor", required = false) String plateColor,
            @RequestParam(value = "plateType", required = false) String plateType,
            @RequestParam(value = "vehicleType", required = false) String vehicleType,
            @RequestParam(value = "confidence", required = false) Double confidence,
            @RequestParam(value = "source", defaultValue = "手动录入") String source,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        
        DetectionRecord record = detectionRecordService.saveDetectionRecord(
            plateNumber, plateColor, plateType, vehicleType, confidence, source, image
        );
        return ResponseEntity.ok(record);
    }
    
    @GetMapping("/list")
    @Operation(summary = "获取检测记录列表", description = "分页获取所有检测记录")
    public ResponseEntity<Page<DetectionRecord>> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DetectionRecord> records = detectionRecordService.getAllRecords(
            PageRequest.of(page, size, Sort.by("detectionTime").descending())
        );
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/unsaved")
    @Operation(summary = "获取未保存记录", description = "获取未保存到车辆库的检测记录")
    public ResponseEntity<Page<DetectionRecord>> getUnsavedRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DetectionRecord> records = detectionRecordService.getUnsavedRecords(
            PageRequest.of(page, size, Sort.by("detectionTime").descending())
        );
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取记录详情", description = "根据ID获取检测记录详情")
    public ResponseEntity<DetectionRecord> getRecordById(@PathVariable Long id) {
        return detectionRecordService.getAllRecords(PageRequest.of(0, 1))
            .stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/save-to-vehicle")
    @Operation(summary = "保存到车辆库", description = "将检测记录保存到车辆信息库")
    public ResponseEntity<VehicleInfo> saveToVehicleInfo(
            @PathVariable Long id,
            @RequestParam(value = "category", required = false) String category) {
        VehicleInfo vehicleInfo = detectionRecordService.saveRecordToVehicleInfo(id, category);
        return ResponseEntity.ok(vehicleInfo);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除记录", description = "删除检测记录")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        detectionRecordService.deleteRecord(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/plate/{plateNumber}")
    @Operation(summary = "按车牌号查询", description = "根据车牌号查询检测记录")
    public ResponseEntity<List<DetectionRecord>> getByPlateNumber(@PathVariable String plateNumber) {
        List<DetectionRecord> records = detectionRecordService.getRecordsByPlateNumber(plateNumber);
        return ResponseEntity.ok(records);
    }
}
