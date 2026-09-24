package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.AesFileResultDto;
import hr.tvz.ntp.smartordersystem.dto.FileHashDto;
import hr.tvz.ntp.smartordersystem.dto.SecureArchiveResultDto;

public interface CryptoService {

    FileHashDto hashActivityLogs();
    FileHashDto hashProductStockXml();
    FileHashDto hashFile(String fileName);
    AesFileResultDto encryptActivityLogs();
    String decryptActivityLogs();

    SecureArchiveResultDto createSecureLogArchive();
    String decryptSecureLogArchive();
}
