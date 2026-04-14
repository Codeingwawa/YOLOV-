package com.parking.service;

import com.parking.entity.ParkingSpace;
import com.parking.entity.VehicleInfo;
import com.parking.repository.ParkingSpaceRepository;
import com.parking.repository.VehicleInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSpaceScheduler {
    
    private final VehicleInfoRepository vehicleInfoRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredParkingSpaces() {
        log.debug("Checking for expired parking spaces...");
        
        LocalDateTime now = LocalDateTime.now();
        
        List<VehicleInfo> expiredVehicles = vehicleInfoRepository.findByParkingEndTimeBeforeAndParkingSpaceIdIsNotNull(now);
        
        for (VehicleInfo vehicle : expiredVehicles) {
            Long spaceId = vehicle.getParkingSpaceId();
            if (spaceId != null) {
                parkingSpaceRepository.findById(spaceId).ifPresent(space -> {
                    space.setStatus(ParkingSpace.SpaceStatus.AVAILABLE);
                    space.setCurrentPlate(null);
                    space.setOccupiedTime(null);
                    parkingSpaceRepository.save(space);
                    
                    log.info("车位 {} 已自动释放，车辆 {} 停放时间已到期", 
                        space.getSpaceNumber(), vehicle.getPlateNumber());
                });
                
                vehicle.setParkingSpaceId(null);
                vehicle.setParkingSpaceNumber(null);
                vehicle.setParkingStartTime(null);
                vehicle.setParkingEndTime(null);
                vehicleInfoRepository.save(vehicle);
            }
        }
        
        if (!expiredVehicles.isEmpty()) {
            log.info("已释放 {} 个过期车位", expiredVehicles.size());
        }
    }
}
