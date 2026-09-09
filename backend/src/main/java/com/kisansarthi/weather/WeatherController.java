package com.kisansarthi.weather;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Agri Weather", description = "District-wise weather advisories and transit risk evaluation")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{district}")
    @Operation(summary = "Get agricultural weather report and transit advisory for a district")
    public ResponseEntity<ApiResponse<WeatherDto>> getWeather(@PathVariable String district) {
        WeatherDto weather = weatherService.getWeatherForDistrict(district);
        return ResponseEntity.ok(ApiResponse.ok(weather));
    }
}
