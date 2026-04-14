package com.parking.service;

import com.parking.entity.ParkingSpace;
import com.parking.entity.ParkingSpace.SpaceStatus;
import com.parking.entity.VehicleInfo;
import com.parking.repository.ParkingSpaceRepository;
import com.parking.repository.VehicleInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final VehicleInfoRepository vehicleInfoRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalSpaces = parkingSpaceRepository.countTotal();
        long availableSpaces = parkingSpaceRepository.countByStatus(SpaceStatus.AVAILABLE);
        long occupiedSpaces = totalSpaces - availableSpaces;
        
        stats.put("totalSpaces", totalSpaces);
        stats.put("availableSpaces", availableSpaces);
        stats.put("occupiedSpaces", occupiedSpaces);
        stats.put("occupancyRate", totalSpaces > 0 ? (double) occupiedSpaces / totalSpaces * 100 : 0);
        
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        
        List<VehicleInfo> todayVehicles = vehicleInfoRepository.findByCreatedAtBetween(todayStart, todayEnd);
        long todayEntries = todayVehicles.size();
        double todayRevenue = todayEntries * 5.0;
        
        stats.put("todayEntries", todayEntries);
        stats.put("todayRevenue", todayRevenue);
        
        return stats;
    }
    
    public Map<String, Object> getRevenueStats(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> stats = new HashMap<>();
        
        List<VehicleInfo> vehicles = vehicleInfoRepository.findByCreatedAtBetween(start, end);
        long totalVehicles = vehicles.size();
        double totalRevenue = totalVehicles * 5.0;
        
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalVehicles", totalVehicles);
        stats.put("averageFee", totalVehicles > 0 ? totalRevenue / totalVehicles : 0);
        
        return stats;
    }
    
    public List<Object[]> getTopFrequentVehicles(int limit) {
        return vehicleInfoRepository.countByCategory();
    }
    
    public List<Map<String, Object>> getRevenueTrend(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        List<VehicleInfo> vehicles = vehicleInfoRepository.findByCreatedAtBetween(start, end);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            long count = vehicles.stream()
                .filter(v -> v.getCreatedAt() != null
                    && !v.getCreatedAt().isBefore(dayStart)
                    && v.getCreatedAt().isBefore(dayEnd))
                .count();

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(formatter));
            dayData.put("revenue", count * 5.0);
            dayData.put("count", count);
            trend.add(dayData);
        }

        return trend;
    }

    public Map<String, Object> getHourlyStats(LocalDateTime date) {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime dayStart = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = dayStart.plusDays(1);
        
        List<VehicleInfo> dayVehicles = vehicleInfoRepository.findByCreatedAtBetween(dayStart, dayEnd);
        
        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime hourStart = dayStart.withHour(hour);
            LocalDateTime hourEnd = hourStart.plusHours(1);
            
            long count = dayVehicles.stream()
                .filter(v -> v.getCreatedAt() != null 
                    && !v.getCreatedAt().isBefore(hourStart) 
                    && v.getCreatedAt().isBefore(hourEnd))
                .count();
            stats.put("hour_" + hour, count);
        }
        
        return stats;
    }
}
