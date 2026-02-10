package org.odema.posnew.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private final OrderRepository orderRepository;

    // ESC/POS Commands
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte[] INIT_PRINTER = {ESC, '@'};
    private static final byte[] SELECT_BOLD = {ESC, 'E', 1};
    private static final byte[] CANCEL_BOLD = {ESC, 'E', 0};
    private static final byte[] SELECT_CENTER = {ESC, 'a', 1};
    private static final byte[] SELECT_LEFT = {ESC, 'a', 0};
    private static final byte[] CUT_PAPER = {GS, 'V', 66, 0};
    private static final byte[] LINE_FEED = {0x0A};
    private static final byte[] CHAR_SIZE_NORMAL = {GS, '!', 0};
    private static final byte[] CHAR_SIZE_DOUBLE = {GS, '!', 0x11};

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
        receipt.append("       TICKET DE CAISSE\n");
        receipt.append("=================================\n");
        receipt.append("N° Commande: ").append(order.getOrderNumber()).append("\n");
        receipt.append("Date: ").append(order.getCreatedAt().format(formatter)).append("\n");
        receipt.append("Caissier: ").append(order.getCashier().getUsername()).append("\n");

        if (order.getCustomer() != null) {
            receipt.append("Client: ").append(order.getCustomer().getFullName()).append("\n");
        }
        receipt.append("---------------------------------\n");

        // Articles
        receipt.append(String.format("%-20s %3s %9s\n", "Article", "Qté", "Total"));
        receipt.append("---------------------------------\n");

        for (OrderItem item : order.getItems()) {
            String productName = item.getProduct().getName();
            if (productName.length() > 20) {
                productName = productName.substring(0, 17) + "...";
            }

            receipt.append(String.format("%-20s %3d %9.0f\n",
                    productName,
                    item.getQuantity(),
                    item.getFinalPrice()));
        }

        // Totaux
        receipt.append("---------------------------------\n");
        receipt.append(String.format("%-24s %9.0f\n", "Sous-total:", order.getSubtotal()));

        if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("%-24s %9.0f\n", "Taxe:", order.getTaxAmount()));
        }

        if (order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("%-24s %9.0f\n", "Remise:", order.getDiscountAmount()));
        }

        receipt.append("=================================\n");
        receipt.append(String.format("%-24s %9.0f\n", "TOTAL:", order.getTotalAmount()));
        receipt.append("=================================\n");

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
            receipt.append(String.format("  %-20s %9.0f\n", methodName + ":", entry.getValue()));
        }

        if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("  %-20s %9.0f\n", "Crédit:", creditAmount));
        }

        receipt.append("---------------------------------\n");
        receipt.append(String.format("%-24s %9.0f\n", "Total payé:", totalPaid));

        if (order.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("%-24s %9.0f\n", "Reste dû:", order.getRemainingAmount()));
        }

        if (order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("%-24s %9.0f\n", "Monnaie:", order.getChangeAmount()));
        }

        // Pied de page
        receipt.append("=================================\n");
        receipt.append("    Merci de votre visite !\n");
        receipt.append("=================================\n");
        receipt.append("\n\n\n");

        return receipt.toString();
    }

    @Override
    public byte[] generateReceiptPdf(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Format ticket 80mm (226 points de largeur)
            Document document = new Document(new Rectangle(226, 800), 10, 10, 10, 10);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 8);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 7);

            // En-tête
            Paragraph storeName = new Paragraph(order.getStore().getName(), titleFont);
            storeName.setAlignment(Element.ALIGN_CENTER);
            document.add(storeName);

            if (order.getStore().getAddress() != null) {
                Paragraph address = new Paragraph(order.getStore().getAddress(), smallFont);
                address.setAlignment(Element.ALIGN_CENTER);
                document.add(address);
            }

            if (order.getStore().getPhone() != null) {
                Paragraph phone = new Paragraph("Tél: " + order.getStore().getPhone(), smallFont);
                phone.setAlignment(Element.ALIGN_CENTER);
                document.add(phone);
            }

            document.add(new Paragraph(" "));

            Paragraph ticketTitle = new Paragraph("TICKET DE CAISSE", boldFont);
            ticketTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(ticketTitle);

            document.add(new Paragraph(" "));

            // Informations commande
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("N°: " + order.getOrderNumber(), normalFont));
            document.add(new Paragraph("Date: " + order.getCreatedAt().format(formatter), normalFont));
            document.add(new Paragraph("Caissier: " + order.getCashier().getUsername(), normalFont));

            if (order.getCustomer() != null) {
                document.add(new Paragraph("Client: " + order.getCustomer().getFullName(), normalFont));
            }

            document.add(new Paragraph("--------------------------------", smallFont));

            // Articles
            for (OrderItem item : order.getItems()) {
                String line = String.format("%s x%d",
                        item.getProduct().getName(),
                        item.getQuantity());
                document.add(new Paragraph(line, normalFont));

                String priceLine = String.format("  %.0f FCFA", item.getFinalPrice());
                Paragraph price = new Paragraph(priceLine, normalFont);
                price.setAlignment(Element.ALIGN_RIGHT);
                document.add(price);
            }

            document.add(new Paragraph("--------------------------------", smallFont));

            // Totaux
            document.add(new Paragraph(String.format("Sous-total: %.0f FCFA", order.getSubtotal()), normalFont));

            if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                document.add(new Paragraph(String.format("Taxe: %.0f FCFA", order.getTaxAmount()), normalFont));
            }

            if (order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                document.add(new Paragraph(String.format("Remise: %.0f FCFA", order.getDiscountAmount()), normalFont));
            }

            document.add(new Paragraph("================================", smallFont));

            Paragraph total = new Paragraph(String.format("TOTAL: %.0f FCFA", order.getTotalAmount()), boldFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(new Paragraph("================================", smallFont));

            // Paiements
            Map<PaymentMethod, BigDecimal> paymentsByMethod = new HashMap<>();
            BigDecimal totalPaid = BigDecimal.ZERO;

            for (Payment payment : order.getPayments()) {
                if (payment.getStatus() == PaymentStatus.PAID) {
                    paymentsByMethod.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add);
                    totalPaid = totalPaid.add(payment.getAmount());
                }
            }

            document.add(new Paragraph("Paiements:", normalFont));
            for (Map.Entry<PaymentMethod, BigDecimal> entry : paymentsByMethod.entrySet()) {
                String methodName = getPaymentMethodName(entry.getKey());
                document.add(new Paragraph(String.format("  %s: %.0f FCFA", methodName, entry.getValue()), normalFont));
            }

            if (order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                Paragraph change = new Paragraph(String.format("Monnaie: %.0f FCFA", order.getChangeAmount()), boldFont);
                document.add(change);
            }

            document.add(new Paragraph(" "));

            Paragraph thanks = new Paragraph("Merci de votre visite !", normalFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating receipt PDF", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] generateThermalPrinterData(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Initialize printer
            output.write(INIT_PRINTER);

            // Store name (centered, double size)
            output.write(SELECT_CENTER);
            output.write(CHAR_SIZE_DOUBLE);
            output.write(SELECT_BOLD);
            writeLine(output, order.getStore().getName());
            output.write(CANCEL_BOLD);
            output.write(CHAR_SIZE_NORMAL);

            // Store details
            if (order.getStore().getAddress() != null) {
                writeLine(output, order.getStore().getAddress());
            }
            if (order.getStore().getPhone() != null) {
                writeLine(output, "Tel: " + order.getStore().getPhone());
            }

            // Separator
            output.write(SELECT_LEFT);
            writeLine(output, "=================================");

            // Title
            output.write(SELECT_CENTER);
            output.write(SELECT_BOLD);
            writeLine(output, "TICKET DE CAISSE");
            output.write(CANCEL_BOLD);
            writeLine(output, "=================================");

            // Order info
            output.write(SELECT_LEFT);
            writeLine(output, "N° Cmd: " + order.getOrderNumber());
            writeLine(output, "Date: " + order.getCreatedAt().format(formatter));
            writeLine(output, "Caissier: " + order.getCashier().getUsername());

            if (order.getCustomer() != null) {
                writeLine(output, "Client: " + order.getCustomer().getFullName());
            }

            writeLine(output, "---------------------------------");

            // Items header
            writeLine(output, String.format("%-20s %3s %9s", "Article", "Qté", "Total"));
            writeLine(output, "---------------------------------");

            // Items
            for (OrderItem item : order.getItems()) {
                String productName = item.getProduct().getName();
                if (productName.length() > 20) {
                    productName = productName.substring(0, 17) + "...";
                }

                writeLine(output, String.format("%-20s %3d %9.0f",
                        productName,
                        item.getQuantity(),
                        item.getFinalPrice()));
            }

            // Totals
            writeLine(output, "---------------------------------");
            writeLine(output, String.format("%-24s %9.0f", "Sous-total:", order.getSubtotal()));

            if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                writeLine(output, String.format("%-24s %9.0f", "Taxe:", order.getTaxAmount()));
            }

            if (order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                writeLine(output, String.format("%-24s %9.0f", "Remise:", order.getDiscountAmount()));
            }

            writeLine(output, "=================================");
            output.write(SELECT_BOLD);
            writeLine(output, String.format("%-24s %9.0f", "TOTAL:", order.getTotalAmount()));
            output.write(CANCEL_BOLD);
            writeLine(output, "=================================");

            // Payments
            writeLine(output, "Paiements:");

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
                writeLine(output, String.format("  %-20s %9.0f", methodName + ":", entry.getValue()));
            }

            if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
                writeLine(output, String.format("  %-20s %9.0f", "Crédit:", creditAmount));
            }

            writeLine(output, "---------------------------------");
            writeLine(output, String.format("%-24s %9.0f", "Total payé:", totalPaid));

            if (order.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                writeLine(output, String.format("%-24s %9.0f", "Reste dû:", order.getRemainingAmount()));
            }

            if (order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                output.write(SELECT_BOLD);
                writeLine(output, String.format("%-24s %9.0f", "Monnaie:", order.getChangeAmount()));
                output.write(CANCEL_BOLD);
            }

            // Footer
            writeLine(output, "=================================");
            output.write(SELECT_CENTER);
            writeLine(output, "Merci de votre visite !");
            writeLine(output, "=================================");

            // Feed and cut
            output.write(LINE_FEED);
            output.write(LINE_FEED);
            output.write(LINE_FEED);
            output.write(CUT_PAPER);

            return output.toByteArray();

        } catch (Exception e) {
            log.error("Error generating thermal printer data", e);
            return new byte[0];
        }
    }

    private void writeLine(ByteArrayOutputStream output, String text) throws Exception {
        output.write(text.getBytes(StandardCharsets.UTF_8));
        output.write(LINE_FEED);
    }

    private String getPaymentMethodName(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Espèces";
            case MOBILE_MONEY -> "Mobile Money";
            case CREDIT_CARD -> "Carte";
            case DEBIT_CARD -> "Carte Débit";
            case CREDIT -> "Crédit";
            case BANK_TRANSFER -> "Virement";
            case CHECK -> "Chèque";
            case LOYALTY_POINTS -> "Points Fidélité";
            default -> method.name();
        };
    }
}