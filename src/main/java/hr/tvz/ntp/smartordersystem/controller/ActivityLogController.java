package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.ActivityLogDto;
import hr.tvz.ntp.smartordersystem.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activity-logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityLogDto>> getAllLogs() {
        return ResponseEntity.ok(activityLogService.findCurrent());
    }

    @GetMapping("/current")
    public ResponseEntity<List<ActivityLogDto>> getCurrentLogs() {
        return ResponseEntity.ok(activityLogService.findCurrent());
    }

    @GetMapping("/archive")
    public ResponseEntity<Map<String, List<ActivityLogDto>>> getArchivedLogs() {
        return ResponseEntity.ok(activityLogService.findArchived());
    }

    @GetMapping("/integrity")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        return ResponseEntity.ok(activityLogService.verifyIntegrity());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityLogDto> getLogById(@PathVariable String id) {
        return ResponseEntity.ok(activityLogService.findById(id));
    }
}