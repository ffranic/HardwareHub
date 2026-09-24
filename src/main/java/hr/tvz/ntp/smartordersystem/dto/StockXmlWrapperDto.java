package hr.tvz.ntp.smartordersystem.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "productStock")
@XmlAccessorType(XmlAccessType.FIELD)
public class StockXmlWrapperDto {

    @XmlElement(name = "stockItem")
    private List<StockXmlItemDto> items = new ArrayList<>();
}
