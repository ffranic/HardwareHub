package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.ActivityLogDto;

import java.util.List;
import java.util.Map;

public interface ActivityLogService {

    List<ActivityLogDto> findAll();

    List<ActivityLogDto> findCurrent();

    Map<String, List<ActivityLogDto>> findArchived();

    Map<String, Object> verifyIntegrity();

    ActivityLogDto findById(String id);

    ActivityLogDto create(ActivityLogDto log);

    void log(String username, String role, String action, String details, String ipAddress);
}