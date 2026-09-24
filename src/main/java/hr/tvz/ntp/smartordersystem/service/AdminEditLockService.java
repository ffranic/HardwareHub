package hr.tvz.ntp.smartordersystem.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AdminEditLockService {

    private static final int LOCK_TIMEOUT_MINUTES = 10;

    private final ReentrantLock registryLock = new ReentrantLock();
    private final Map<String, EditLockInfo> activeLocks = new HashMap<>();

    public void acquireLock(String resourceType, Long resourceId, String username) {
        registryLock.lock();

        try {
            removeExpiredLocks();

            String key = buildKey(resourceType, resourceId);
            EditLockInfo existingLock = activeLocks.get(key);

            if (existingLock != null && !existingLock.username().equals(username)) {
                throw new IllegalStateException(
                        "This " + resourceType + " is currently being edited by " + existingLock.username()
                );
            }

            activeLocks.put(
                    key,
                    new EditLockInfo(username, LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES))
            );

        } finally {
            registryLock.unlock();
        }
    }

    public void releaseLock(String resourceType, Long resourceId, String username) {
        registryLock.lock();

        try {
            String key = buildKey(resourceType, resourceId);
            EditLockInfo existingLock = activeLocks.get(key);

            if (existingLock != null && existingLock.username().equals(username)) {
                activeLocks.remove(key);
            }

        } finally {
            registryLock.unlock();
        }
    }

    private void removeExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();

        activeLocks.entrySet().removeIf(entry ->
                entry.getValue().expiresAt().isBefore(now)
        );
    }

    private String buildKey(String resourceType, Long resourceId) {
        return resourceType.toLowerCase() + ":" + resourceId;
    }

    private record EditLockInfo(String username, LocalDateTime expiresAt) {
    }
}