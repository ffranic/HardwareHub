package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRateDto {

    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private String date;

}
