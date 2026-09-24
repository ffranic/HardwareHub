package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.AppSettingsDTO;

public interface SettingsService {
    AppSettingsDTO getSettings();
    AppSettingsDTO updateSettings(AppSettingsDTO settings);
}
