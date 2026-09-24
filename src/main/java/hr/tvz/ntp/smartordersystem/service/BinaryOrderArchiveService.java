package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.ArchivedOrderBinaryRecordDto;

import java.util.List;
import java.util.Map;

public interface BinaryOrderArchiveService {

    void archiveOrderIfTerminal(Long orderId);

    Map<String, List<ArchivedOrderBinaryRecordDto>> readArchivedOrders();
}