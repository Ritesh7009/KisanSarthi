package com.kisansarthi.notification;

import com.kisansarthi.auth.User;
import com.kisansarthi.auth.UserRepository;
import com.kisansarthi.common.ResourceNotFoundException;
import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FarmerRepository farmerRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            FarmerRepository farmerRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.farmerRepository = farmerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForFarmer(String phone) {
        Farmer farmer = farmerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found for phone: " + phone));

        return notificationRepository.findByFarmerIdOrderByCreatedAtDesc(farmer.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setHindiTitle(n.getHindiTitle());
        dto.setMessage(n.getMessage());
        dto.setHindiMessage(n.getHindiMessage());
        dto.setCategory(n.getCategory());
        dto.setSenderTag(n.getSenderTag());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
