package com.kisansarthi.crop;

import com.kisansarthi.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CropService {

    private final CropRepository cropRepository;

    public CropService(CropRepository cropRepository) {
        this.cropRepository = cropRepository;
    }

    public List<CropDto> getAllCrops() {
        return cropRepository.findAll().stream()
                .map(CropDto::fromEntity)
                .collect(Collectors.toList());
    }

    public CropDto getCropById(String id) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + id));
        return CropDto.fromEntity(crop);
    }

    @Transactional
    public CropDto updateCropMsp(String id, UpdateMspRequest request) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + id));

        crop.setStandardMspPerQuintal(request.getStandardMsp());
        if (request.getBonus() != null) {
            crop.setMpBonusPerQuintal(request.getBonus());
        }
        crop.setTotalMsp(crop.getStandardMspPerQuintal().add(crop.getMpBonusPerQuintal()));
        crop.setUpdatedAt(Instant.now());

        return CropDto.fromEntity(cropRepository.save(crop));
    }

    public Crop getCropEntity(String id) {
        return cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + id));
    }
}
