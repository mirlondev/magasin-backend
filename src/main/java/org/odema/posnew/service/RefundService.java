package org.odema.posnew.service;

import org.odema.posnew.dto.request.RefundRequest;
import org.odema.posnew.dto.response.RefundResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RefundService {
    RefundResponse createRefund(RefundRequest request, UUID cashierId);

    RefundResponse getRefundById(UUID refundId);

    RefundResponse getRefundByNumber(String refundNumber);

    RefundResponse updateRefund(UUID refundId, RefundRequest request);

    void cancelRefund(UUID refundId);

    List<RefundResponse> getAllRefunds();

    List<RefundResponse> getRefundsByOrder(UUID orderId);

    List<RefundResponse> getRefundsByStore(UUID storeId);

    List<RefundResponse> getRefundsByCashier(UUID cashierId);

    List<RefundResponse> getRefundsByStatus(String status);

    RefundResponse approveRefund(UUID refundId);

    RefundResponse rejectRefund(UUID refundId, String reason);

    RefundResponse completeRefund(UUID refundId);

    boolean canOrderBeRefunded(UUID orderId);

    BigDecimal getRefundableAmount(UUID orderId);
}
