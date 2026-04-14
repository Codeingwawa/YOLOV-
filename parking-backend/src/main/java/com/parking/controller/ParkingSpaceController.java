package com.parking.controller;

import com.parking.entity.ParkingSpace;
import com.parking.repository.ParkingSpaceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking-space")
@RequiredArgsConstructor
@Tag(name = "车位管理", description = "车位信息管理接口")
public class ParkingSpaceController {
    
    private final ParkingSpaceRepository parkingSpaceRepository;
    
    @GetMapping("/list")
    @Operation(summary = "获取车位列表", description = "获取所有车位信息")
    public ResponseEntity<List<ParkingSpace>> getAllSpaces() {
        List<ParkingSpace> spaces = parkingSpaceRepository.findAll();
        return ResponseEntity.ok(spaces);
    }
    
    @GetMapping("/available")
    @Operation(summary = "获取可用车位", description = "获取所有可用车位")
    public ResponseEntity<List<ParkingSpace>> getAvailableSpaces() {
        List<ParkingSpace> spaces = parkingSpaceRepository.findByStatus(ParkingSpace.SpaceStatus.AVAILABLE);
        return ResponseEntity.ok(spaces);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取车位详情", description = "根据ID获取车位详情")
    public ResponseEntity<ParkingSpace> getSpaceById(@PathVariable Long id) {
        return parkingSpaceRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/release")
    @Operation(summary = "释放车位", description = "管理员手动释放车位")
    public ResponseEntity<ParkingSpace> releaseSpace(@PathVariable Long id) {
        return parkingSpaceRepository.findById(id)
            .map(space -> {
                space.setStatus(ParkingSpace.SpaceStatus.AVAILABLE);
                space.setCurrentPlate(null);
                space.setOccupiedTime(null);
                ParkingSpace updated = parkingSpaceRepository.save(space);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
