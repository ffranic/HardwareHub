package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.service.AdminEditLockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin-edit-locks")
public class AdminEditLockController {

    private final AdminEditLockService adminEditLockService;

    public AdminEditLockController(AdminEditLockService adminEditLockService) {
        this.adminEditLockService = adminEditLockService;
    }

    @PostMapping("/{resourceType}/{resourceId}/acquire")
    public ResponseEntity<?> acquireLock(@PathVariable String resourceType,
                                         @PathVariable Long resourceId,
                                         Authentication authentication) {
        String username = authentication.getName();

        adminEditLockService.acquireLock(resourceType, resourceId, username);

        return ResponseEntity.ok(Map.of(
                "locked", true,
                "resourceType", resourceType,
                "resourceId", resourceId,
                "lockedBy", username
        ));
    }

    @DeleteMapping("/{resourceType}/{resourceId}/release")
    public ResponseEntity<Void> releaseLock(@PathVariable String resourceType,
                                            @PathVariable Long resourceId,
                                            Authentication authentication) {
        String username = authentication.getName();

        adminEditLockService.releaseLock(resourceType, resourceId, username);

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleLockedResource(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}