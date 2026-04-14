package com.parking.controller;

import com.parking.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "停车场统计报表接口")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping("/dashboard")
    @Operation(summary = "仪表盘数据", description = "获取停车场实时统计数据")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(statisticsService.getDashboardStats());
    }
    
    @GetMapping("/revenue")
    @Operation(summary = "收入统计", description = "获取指定时间范围内的收入统计")
    public ResponseEntity<Map<String, Object>> getRevenueStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(statisticsService.getRevenueStats(start, end));
    }
    
    @GetMapping("/revenue-trend")
    @Operation(summary = "收入趋势", description = "获取指定时间范围内每日收入趋势数据")
    public ResponseEntity<List<Map<String, Object>>> getRevenueTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(statisticsService.getRevenueTrend(start, end));
    }

    @GetMapping("/top-vehicles")
    @Operation(summary = "高频车辆", description = "获取进出频率最高的车辆")
    public ResponseEntity<List<Object[]>> getTopFrequentVehicles(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statisticsService.getTopFrequentVehicles(limit));
    }
    
    @GetMapping("/hourly")
    @Operation(summary = "小时统计", description = "获取指定日期每小时的车辆进出统计")
    public ResponseEntity<Map<String, Object>> getHourlyStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime date) {
        if (date == null) {
            date = LocalDateTime.now();
        }
        return ResponseEntity.ok(statisticsService.getHourlyStats(date));
    }
}
