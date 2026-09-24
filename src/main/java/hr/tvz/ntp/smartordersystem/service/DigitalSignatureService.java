package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.DigitalSignatureBatchResultDto;
import hr.tvz.ntp.smartordersystem.dto.DigitalSignatureResultDto;
import hr.tvz.ntp.smartordersystem.dto.SignatureVerificationReportDto;

public interface DigitalSignatureService {
    DigitalSignatureResultDto signOrder(Long orderId);
    DigitalSignatureResultDto verifyOrderSignature(Long orderId);
    DigitalSignatureBatchResultDto verifyAllOrderSignatures();

    SignatureVerificationReportDto readVerificationReport();
    void deleteVerificationReport();
}