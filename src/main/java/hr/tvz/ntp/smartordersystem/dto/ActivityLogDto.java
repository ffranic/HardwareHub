package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLogDto {

    private String id;
    private String timestamp;
    private String username;
    private String role;
    private String action;
    private String details;
    private String ipAddress;

}
