package hr.tvz.ntp.smartordersystem.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface LogHelperService {

    void log(Authentication authentication,
             HttpServletRequest request,
             String action,
             String details);

}
