package com.parking.controller;

import com.parking.entity.VehicleInfo;
import com.parking.entity.VehicleInfo.VehicleCategory;
import com.parking.service.VehicleInfoService;
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
@RequestMapping("/api/vehicle-info")
@RequiredArgsConstructor
@Tag(name = "车辆信息管理", description = "车辆信息增删改查接口")
public class VehicleInfoController {
    
    private final VehicleInfoService vehicleInfoService;
    
    @PostMapping("/detect")
    @Operation(summary = "检测并保存车辆", description = "上传图片进行车牌识别并保存到数据库")
    public ResponseEntity<VehicleInfo> detectAndSave(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "category", required = false) String category) throws IOException {
        VehicleInfo vehicleInfo = vehicleInfoService.detectAndSave(image, category);
        return ResponseEntity.ok(vehicleInfo);
    }
    
    @GetMapping("/list")
    @Operation(summary = "获取车辆列表", description = "分页获取所有车辆信息")
    public ResponseEntity<Page<VehicleInfo>> getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleInfo> vehicles = vehicleInfoService.getAllVehicles(
            PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(vehicles);
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索车辆", description = "按条件搜索车辆")
    public ResponseEntity<Page<VehicleInfo>> searchVehicles(
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String plateColor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (plateNumber != null && plateNumber.isEmpty()) {
            plateNumber = null;
        }
        if (vehicleType != null && vehicleType.isEmpty()) {
            vehicleType = null;
        }
        if (plateColor != null && plateColor.isEmpty()) {
            plateColor = null;
        }
        
        VehicleCategory categoryEnum = null;
        if (category != null && !category.isEmpty()) {
            categoryEnum = VehicleCategory.valueOf(category);
        }
        
        Page<VehicleInfo> vehicles = vehicleInfoService.searchVehicles(
            plateNumber, vehicleType, categoryEnum, plateColor,
            PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(vehicles);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取车辆详情", description = "根据ID获取车辆详细信息")
    public ResponseEntity<VehicleInfo> getVehicleById(@PathVariable Long id) {
        return vehicleInfoService.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/plate/{plateNumber}")
    @Operation(summary = "按车牌号查询", description = "根据车牌号获取车辆信息")
    public ResponseEntity<VehicleInfo> getByPlateNumber(@PathVariable String plateNumber) {
        return vehicleInfoService.getByPlateNumber(plateNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新车辆信息", description = "更新车辆信息")
    public ResponseEntity<VehicleInfo> updateVehicle(
            @PathVariable Long id,
            @RequestBody VehicleInfo vehicleInfo) {
        VehicleInfo updated = vehicleInfoService.updateVehicle(id, vehicleInfo);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除车辆", description = "根据ID删除车辆")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleInfoService.deleteVehicle(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/plate/{plateNumber}")
    @Operation(summary = "按车牌号删除", description = "根据车牌号删除车辆")
    public ResponseEntity<Void> deleteByPlateNumber(@PathVariable String plateNumber) {
        vehicleInfoService.deleteByPlateNumber(plateNumber);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/stats/type")
    @Operation(summary = "车型统计", description = "按车型统计车辆数量")
    public ResponseEntity<List<Object[]>> getVehicleTypeStats() {
        return ResponseEntity.ok(vehicleInfoService.getVehicleTypeStats());
    }
    
    @GetMapping("/stats/category")
    @Operation(summary = "分类统计", description = "按车辆分类统计数量")
    public ResponseEntity<List<Object[]>> getCategoryStats() {
        return ResponseEntity.ok(vehicleInfoService.getCategoryStats());
    }
}
