package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.StockXmlItemDto;

import java.util.List;

public interface StockXmlService {

    List<StockXmlItemDto> exportFromDatabaseToXml();

    List<StockXmlItemDto> readFromXml();

    StockXmlItemDto updateXmlItem(Long productId, StockXmlItemDto updatedItem);

    void deleteXmlItem(Long productId);
}
