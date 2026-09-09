package com.kisansarthi.ai;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Advisory", description = "Smart procurement logistics, wait-time prediction, and yield economics")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/slot-recommendation")
    @Operation(summary = "Get AI recommendation for optimal mandi slot and arrival window")
    public ResponseEntity<ApiResponse<SlotRecommendationResponse>> recommendSlot(
            @RequestBody SlotRecommendationRequest req
    ) {
        SlotRecommendationResponse resp = aiService.recommendSlot(req);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PostMapping("/yield-advisor")
    @Operation(summary = "Get AI analysis on crop yield estimates, gross revenue, and profit margins")
    public ResponseEntity<ApiResponse<YieldAdvisorResponse>> analyzeYield(
            @RequestBody YieldAdvisorRequest req
    ) {
        YieldAdvisorResponse resp = aiService.analyzeYield(req);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }
}
