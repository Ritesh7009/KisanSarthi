package com.kisansarthi.mandi;

import com.kisansarthi.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MandiService {

    private final MandiRepository mandiRepository;

    public MandiService(MandiRepository mandiRepository) {
        this.mandiRepository = mandiRepository;
    }

    public List<MandiDto> getAllMandis(String district) {
        List<Mandi> mandis = (district != null && !district.isBlank())
                ? mandiRepository.findByDistrictIgnoreCase(district)
                : mandiRepository.findAll();

        return mandis.stream().map(MandiDto::fromEntity).collect(Collectors.toList());
    }

    public MandiDto getMandiById(String id) {
        Mandi mandi = mandiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + id));
        return MandiDto.fromEntity(mandi);
    }

    public MandiStatusDto getMandiStatus(String id) {
        Mandi mandi = mandiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + id));

        int waitMins = mandi.getActiveTokensWaiting() * mandi.getAverageProcessingMins();
        return new MandiStatusDto(
                mandi.getId(),
                mandi.getName(),
                mandi.getCurrentTokenServing(),
                mandi.getTotalTokensToday(),
                mandi.getActiveTokensWaiting(),
                mandi.getGateStatus(),
                waitMins,
                "Kanta Bay 1"
        );
    }

    public Mandi getMandiEntity(String id) {
        return mandiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + id));
    }
}
