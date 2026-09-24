package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.ExchangeRateDto;

public interface ExternalRestService {
    ExchangeRateDto getEurToUsdRate();
}
