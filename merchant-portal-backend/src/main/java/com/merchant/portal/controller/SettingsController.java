package com.merchant.portal.controller;

import com.merchant.portal.model.SystemSetting;
import com.merchant.portal.repository.SystemSettingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:4200")
public class SettingsController {

    private final SystemSettingRepository settingRepository;

    public SettingsController(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * GET /api/settings/facial-thresholds
     * Returns the current High and Medium thresholds.
     * Guidance for admin:
     *   - Below 0.55 = Low confidence (likely different people)
     *   - 0.55 to 0.70 = Medium confidence (needs manual review)
     *   - Above 0.70 = High confidence (very likely same person)
     */
    @GetMapping("/facial-thresholds")
    public ResponseEntity<?> getThresholds() {
        double high = getSettingDouble("THRESHOLD_HIGH", 0.70);
        double medium = getSettingDouble("THRESHOLD_MEDIUM", 0.55);

        return ResponseEntity.ok(Map.of(
                "thresholdHigh", high,
                "thresholdMedium", medium,
                "guidance", Map.of(
                        "high", "Score >= thresholdHigh → High confidence (very likely same person)",
                        "medium", "Score >= thresholdMedium → Medium confidence (needs manual review)",
                        "low", "Score < thresholdMedium → Low confidence (likely different people)",
                        "recommendedHigh", 0.70,
                        "recommendedMedium", 0.55
                )
        ));
    }

    /**
     * PUT /api/settings/facial-thresholds
     * Body: { "thresholdHigh": 0.70, "thresholdMedium": 0.55 }
     */
    @PutMapping("/facial-thresholds")
    public ResponseEntity<?> updateThresholds(@RequestBody Map<String, Double> payload) {
        Double high = payload.get("thresholdHigh");
        Double medium = payload.get("thresholdMedium");

        if (high == null || medium == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Both thresholdHigh and thresholdMedium are required."));
        }
        if (medium >= high) {
            return ResponseEntity.badRequest().body(Map.of("message", "thresholdMedium must be less than thresholdHigh."));
        }
        if (medium < 0 || high > 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thresholds must be between 0 and 1."));
        }

        saveSetting("THRESHOLD_HIGH", String.valueOf(high), "Facial similarity score threshold for High confidence");
        saveSetting("THRESHOLD_MEDIUM", String.valueOf(medium), "Facial similarity score threshold for Medium confidence");

        return ResponseEntity.ok(Map.of(
                "message", "Thresholds updated successfully.",
                "thresholdHigh", high,
                "thresholdMedium", medium
        ));
    }

    private double getSettingDouble(String key, double defaultValue) {
        return settingRepository.findBySettingKey(key)
                .map(s -> Double.parseDouble(s.getSettingValue()))
                .orElse(defaultValue);
    }

    private void saveSetting(String key, String value, String description) {
        SystemSetting setting = settingRepository.findBySettingKey(key).orElse(new SystemSetting());
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);
        settingRepository.save(setting);
    }
}

