package com.kisansarthi.queue;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mandis/{mandiId}/queue")
@Tag(name = "Queue Management", description = "Authoritative real-time token queue state and gate calls")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    @Operation(summary = "Get current authoritative queue state for a Mandi")
    public ResponseEntity<ApiResponse<QueueState>> getQueueState(@PathVariable String mandiId) {
        QueueState state = queueService.getQueueState(mandiId);
        return ResponseEntity.ok(ApiResponse.ok(state));
    }

    @GetMapping("/events")
    @Operation(summary = "Get audit trail of recent queue events for a Mandi")
    public ResponseEntity<ApiResponse<List<QueueEvent>>> getRecentEvents(@PathVariable String mandiId) {
        List<QueueEvent> events = queueService.getRecentEvents(mandiId);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @PostMapping("/next")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER')")
    @Operation(summary = "Advance token queue and dispatch real-time WebSocket event + SMS alert")
    public ResponseEntity<ApiResponse<QueueEventDto>> advanceQueue(
            @PathVariable String mandiId,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "Mandi Operator";
        QueueEventDto result = queueService.advanceQueue(mandiId, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Token " + result.getTokenNumber() + " called successfully"));
    }
}
