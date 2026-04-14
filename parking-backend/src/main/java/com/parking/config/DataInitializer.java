package com.parking.config;

import com.parking.entity.ParkingSpace;
import com.parking.entity.User;
import com.parking.repository.ParkingSpaceRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        initAdminUser();
        initParkingSpaces();
    }
    
    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("Default admin user created: admin / admin123");
        }
    }
    
    private void initParkingSpaces() {
        if (parkingSpaceRepository.count() == 0) {
            List<ParkingSpace> spaces = new ArrayList<>();
            
            for (int i = 1; i <= 10; i++) {
                ParkingSpace space = new ParkingSpace();
                space.setSpaceNumber("A" + String.format("%02d", i));
                space.setStatus(ParkingSpace.SpaceStatus.AVAILABLE);
                space.setType(ParkingSpace.SpaceType.STANDARD);
                spaces.add(space);
            }
            
            for (int i = 1; i <= 5; i++) {
                ParkingSpace space = new ParkingSpace();
                space.setSpaceNumber("B" + String.format("%02d", i));
                space.setStatus(ParkingSpace.SpaceStatus.AVAILABLE);
                space.setType(ParkingSpace.SpaceType.LARGE);
                spaces.add(space);
            }
            
            for (int i = 1; i <= 5; i++) {
                ParkingSpace space = new ParkingSpace();
                space.setSpaceNumber("V" + String.format("%02d", i));
                space.setStatus(ParkingSpace.SpaceStatus.AVAILABLE);
                space.setType(ParkingSpace.SpaceType.VIP);
                spaces.add(space);
            }
            
            parkingSpaceRepository.saveAll(spaces);
            log.info("Initialized {} parking spaces", spaces.size());
        }
    }
}
