package com.kisansarthi.farmer;

import com.kisansarthi.auth.User;
import com.kisansarthi.auth.UserRepository;
import com.kisansarthi.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final UserRepository userRepository;

    public FarmerService(FarmerRepository farmerRepository, UserRepository userRepository) {
        this.farmerRepository = farmerRepository;
        this.userRepository = userRepository;
    }

    public FarmerDto getFarmerByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Farmer farmer = farmerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));
        return FarmerDto.fromEntity(farmer);
    }

    @Transactional
    public FarmerDto updateFarmer(String username, UpdateFarmerRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Farmer farmer = farmerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));

        if (request.getName() != null) farmer.setName(request.getName());
        if (request.getHindiName() != null) farmer.setHindiName(request.getHindiName());
        if (request.getDistrict() != null) farmer.setDistrict(request.getDistrict());
        if (request.getVillage() != null) farmer.setVillage(request.getVillage());
        if (request.getLandSizeAcres() != null) farmer.setLandSizeAcres(request.getLandSizeAcres());
        farmer.setUpdatedAt(Instant.now());

        return FarmerDto.fromEntity(farmerRepository.save(farmer));
    }

    public List<FarmerDto> getAllFarmers() {
        return farmerRepository.findAll().stream()
                .map(FarmerDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Farmer getFarmerEntityById(UUID farmerId) {
        return farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerId));
    }
}
