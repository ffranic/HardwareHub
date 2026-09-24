package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "stockItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class StockXmlItemDto {

    @XmlElement
    private Long productId;

    @XmlElement
    private String productName;

    @XmlElement
    private String brand;

    @XmlElement
    private String categoryName;

    @XmlElement
    private BigDecimal price;

    @XmlElement
    private int stock;
}
