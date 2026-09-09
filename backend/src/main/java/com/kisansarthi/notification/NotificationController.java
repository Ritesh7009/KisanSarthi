package com.kisansarthi.notification;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farmers/me/notifications")
@Tag(name = "Farmer Notifications", description = "In-app procurement, token, and weather alert stream")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('FARMER')")
    @Operation(summary = "Get notification stream for authenticated farmer")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(Authentication authentication) {
        String phone = authentication.getName();
        List<NotificationDto> notifications = notificationService.getNotificationsForFarmer(phone);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }
}
