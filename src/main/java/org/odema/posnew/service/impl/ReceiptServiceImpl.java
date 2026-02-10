package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.response.ReceiptResponse;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.service.ReceiptService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final OrderRepository orderRepository;

    @Override
    public ReceiptResponse generateReceipt(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // Regrouper les paiements par méthode
        Map<PaymentMethod, BigDecimal> paymentsByMethod = new HashMap<>();
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        for (Payment payment : order.getPayments()) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                paymentsByMethod.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add);
                totalPaid = totalPaid.add(payment.getAmount());
            } else if (payment.getStatus() == PaymentStatus.CREDIT) {
                creditAmount = creditAmount.add(payment.getAmount());
            }
        }

        return ReceiptResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomer() != null ?
                        order.getCustomer().getFullName() : "Client non enregistré")
                .cashierName(order.getCashier().getUsername())
                .storeName(order.getStore().getName())
                .storeAddress(order.getStore().getAddress())
                .storePhone(order.getStore().getPhone())
                .items(order.getItems())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .totalPaid(totalPaid)
                .remainingAmount(order.getRemainingAmount())
                .changeAmount(order.getChangeAmount())
                .paymentsByMethod(paymentsByMethod)
                .creditAmount(creditAmount)
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    public String getReceiptText(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        StringBuilder receipt = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // En-tête
        receipt.append("=================================\n");
        receipt.append("         ").append(order.getStore().getName()).append("\n");
        if (order.getStore().getAddress() != null) {
            receipt.append("    ").append(order.getStore().getAddress()).append("\n");
        }
        if (order.getStore().getPhone() != null) {
            receipt.append("    Tél: ").append(order.getStore().getPhone()).append("\n");
        }
        receipt.append("=================================\n");
        receipt.append("Ticket de caisse\n");
        receipt.append("Commande #").append(order.getOrderNumber()).append("\n");
        receipt.append("Date: ").append(order.getCreatedAt().format(formatter)).append("\n");
        receipt.append("Caissier: ").append(order.getCashier().getUsername()).append("\n");

        if (order.getCustomer() != null) {
            receipt.append("Client: ").append(order.getCustomer().getFullName()).append("\n");
        }
        receipt.append("---------------------------------\n");

        // Articles
        receipt.append(String.format("%-20s %6s %10s\n", "Article", "Qté", "Total"));
        receipt.append("---------------------------------\n");

        for (OrderItem item : order.getItems()) {
            String productName = item.getProduct().getName();
            if (productName.length() > 20) {
                productName = productName.substring(0, 17) + "...";
            }

            receipt.append(String.format("%-20s %6d %10.2f\n",
                    productName,
                    item.getQuantity(),
                    item.getFinalPrice()));
        }

        // Totaux
        receipt.append("---------------------------------\n");
        receipt.append(String.format("%-20s %16.2f\n", "Sous-total:", order.getSubtotal()));

        if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("%-20s %16.2f\n", "Taxe:", order.getTaxAmount()));
        }

        if (order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("%-20s %16.2f\n", "Remise:", order.getDiscountAmount()));
        }

        receipt.append(String.format("%-20s %16.2f\n", "TOTAL:", order.getTotalAmount()));
        receipt.append("---------------------------------\n");

        // Paiements
        receipt.append("Paiements:\n");

        Map<PaymentMethod, BigDecimal> paymentsByMethod = new HashMap<>();
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        for (Payment payment : order.getPayments()) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                paymentsByMethod.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add);
                totalPaid = totalPaid.add(payment.getAmount());
            } else if (payment.getStatus() == PaymentStatus.CREDIT) {
                creditAmount = creditAmount.add(payment.getAmount());
            }
        }

        for (Map.Entry<PaymentMethod, BigDecimal> entry : paymentsByMethod.entrySet()) {
            String methodName = getPaymentMethodName(entry.getKey());
            receipt.append(String.format("  %-15s %10.2f\n", methodName + ":", entry.getValue()));
        }

        if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("  %-15s %10.2f\n", "Crédit:", creditAmount));
        }

        receipt.append("---------------------------------\n");
        receipt.append(String.format("%-20s %16.2f\n", "Total payé:", totalPaid));
        receipt.append(String.format("%-20s %16.2f\n", "Reste dû:", order.getRemainingAmount()));

        if (order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("%-20s %16.2f\n", "Monnaie:", order.getChangeAmount()));
        }

        // Pied de page
        receipt.append("=================================\n");
        receipt.append("Merci de votre visite !\n");
        receipt.append("=================================\n");

        return receipt.toString();
    }

    @Override
    public byte[] generateReceiptPdf(UUID orderId) {
        // Implémentation PDF avec iText ou autre bibliothèque
        // Pour l'instant, retourner le texte en bytes
        String receiptText = getReceiptText(orderId);
        return receiptText.getBytes();
    }

    private String getPaymentMethodName(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Espèces";
            case MOBILE_MONEY -> "Mobile Money";
            case CREDIT_CARD -> "Carte";
            case CREDIT -> "Crédit";
            case BANK_TRANSFER -> "Virement";
            case CHECK -> "Chèque";
            case VOUCHER -> "Bon";
            default -> method.name();
        };
    }
}
