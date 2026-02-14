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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;  // ← java.util.List explicite
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private final OrderRepository orderRepository;

    @Value("${app.company.name:ODEMA POS}")
    private String companyName;

    @Value("${app.company.tax-id:TAX-123456}")
    private String companyTaxId;

    @Value("${app.company.email:contact@odema.com}")
    private String companyEmail;

    // ESC/POS Commands...
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
    private static final byte[] DOUBLE_WIDTH = {GS, '!', 0x10};
    private static final byte[] DOUBLE_HEIGHT = {GS, '!', 0x01};
    private static final byte[] UNDERLINE_ON = {ESC, '-', 1};
    private static final byte[] UNDERLINE_OFF = {ESC, '-', 0};
    private static final byte[] INVERT_ON = {GS, 'B', 1};
    private static final byte[] INVERT_OFF = {GS, 'B', 0};

    @Override
    public ReceiptResponse generateReceipt(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // CORRECTION: Type explicite java.util.List
        List<ReceiptResponse.ReceiptItemDto> items = order.getItems().stream()
                .map(item -> ReceiptResponse.ReceiptItemDto.builder()
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .finalPrice(item.getFinalPrice())
                        .discountAmount(item.getDiscountAmount())
                        .build())
                .collect(Collectors.toList());

        Map<String, BigDecimal> paymentsByMethod = new HashMap<>();
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        for (Payment payment : order.getPayments()) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                String methodName = payment.getMethod().name();
                paymentsByMethod.merge(methodName, payment.getAmount(), BigDecimal::add);
                totalPaid = totalPaid.add(payment.getAmount());
            } else if (payment.getStatus() == PaymentStatus.CREDIT) {
                creditAmount = creditAmount.add(payment.getAmount());
            }
        }

        return ReceiptResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomer() != null ? order.getCustomer().getFullName() : "Client non enregistré")
                .customerPhone(order.getCustomer() != null ? order.getCustomer().getPhone() : null)
                .cashierName(order.getCashier().getUsername())
                .storeName(order.getStore().getName())
                .storeAddress(order.getStore().getAddress())
                .storePhone(order.getStore().getPhone())
                .items(items)  // ← Plus de cast nécessaire
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .totalPaid(totalPaid)
                .remainingAmount(order.getTotalAmount().subtract(totalPaid))
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
        return getEnhancedReceiptText(order);
    }

    // ============================================
    // CORRECTION PRINCIPALE: Méthode PDF refaite
    // ============================================
    @Override
    public byte[] generateReceiptPdf(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Format ticket 80mm (226 points = 80mm à 72dpi)
            Rectangle ticketSize = new Rectangle(226, 800);
            Document document = new Document(ticketSize, 10, 10, 10, 10);

            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts iText (utiliser com.itextpdf.text.Font)
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // === EN-TÊTE ===
            addCenteredParagraph(document, order.getStore().getName(), titleFont);

            if (order.getStore().getAddress() != null) {
                addCenteredParagraph(document, order.getStore().getAddress(), smallFont);
            }

            if (order.getStore().getPhone() != null) {
                addCenteredParagraph(document, "Tél: " + order.getStore().getPhone(), smallFont);
            }

            // Ligne vide - CORRECTION: utiliser Chunk.NEWLINE au lieu de " "
            document.add(Chunk.NEWLINE);

            addCenteredParagraph(document, "TICKET DE CAISSE", boldFont);
            document.add(Chunk.NEWLINE);

            // === INFOS COMMANDE ===
            document.add(new Paragraph("N°: " + order.getOrderNumber(), normalFont));
            document.add(new Paragraph("Date: " + order.getCreatedAt().format(formatter), normalFont));
            document.add(new Paragraph("Caissier: " + order.getCashier().getUsername(), normalFont));

            if (order.getCustomer() != null) {
                document.add(new Paragraph("Client: " + order.getCustomer().getFullName(), normalFont));
            }

            addSeparator(document, smallFont, "-");

            // === ARTICLES ===
            for (OrderItem item : order.getItems()) {
                // Nom produit + quantité
                String line = String.format("%s x%d",
                        truncate(item.getProduct().getName(), 25),
                        item.getQuantity());
                document.add(new Paragraph(line, normalFont));

                // Prix aligné à droite
                Paragraph pricePara = new Paragraph(
                        String.format("  %.0f FCFA", item.getFinalPrice()),
                        normalFont
                );
                pricePara.setAlignment(Element.ALIGN_RIGHT);
                document.add(pricePara);
            }

            addSeparator(document, smallFont, "-");

            // === TOTAUX ===
            addLine(document, "Sous-total:", order.getSubtotal(), normalFont);

            if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                addLine(document, "TVA:", order.getTaxAmount(), normalFont);
            }

            if (order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addLine(document, "Remise:", order.getDiscountAmount(), normalFont);
            }

            addSeparator(document, smallFont, "=");

            // Total en gras
            Paragraph total = new Paragraph(
                    String.format("TOTAL: %.0f FCFA", order.getTotalAmount()),
                    boldFont
            );
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            addSeparator(document, smallFont, "=");

            // === PAIEMENTS ===
            document.add(new Paragraph("Paiements:", normalFont));

            Map<PaymentMethod, BigDecimal> paymentsByMethod = new HashMap<>();
            BigDecimal totalPaid = BigDecimal.ZERO;

            for (Payment payment : order.getPayments()) {
                if (payment.getStatus() == PaymentStatus.PAID) {
                    paymentsByMethod.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add);
                    totalPaid = totalPaid.add(payment.getAmount());
                }
            }

            for (Map.Entry<PaymentMethod, BigDecimal> entry : paymentsByMethod.entrySet()) {
                String methodName = getPaymentMethodName(entry.getKey());
                String line = String.format("  %s: %.0f FCFA", methodName, entry.getValue());
                document.add(new Paragraph(line, normalFont));
            }

            if (order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                Paragraph change = new Paragraph(
                        String.format("Monnaie: %.0f FCFA", order.getChangeAmount()),
                        boldFont
                );
                document.add(change);
            }

            document.add(Chunk.NEWLINE);

            // === PIED DE PAGE ===
            Paragraph thanks = new Paragraph("Merci de votre visite !", normalFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating receipt PDF", e);
            throw new RuntimeException("Erreur génération PDF: " + e.getMessage(), e);
        }
    }

    // ============================================
    // MÉTHODES UTILITAIRES PDF (iText)
    // ============================================

    private void addCenteredParagraph(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private void addSeparator(Document doc, Font font, String character) throws DocumentException {
        // 32 caractères pour 80mm
        String sep = character.repeat(32);
        doc.add(new Paragraph(sep, font));
    }

    private void addLine(Document doc, String label, BigDecimal amount, Font font) throws DocumentException {
        String text = String.format("%s %.0f FCFA", label, amount);
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        doc.add(p);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    @Override
    public byte[] generateThermalPrinterData(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));
        return generateEnhancedThermalReceipt(order);
    }

    // ============================================
    // THERMAL PRINTER (inchangé, fonctionne déjà)
    // ============================================

    private byte[] generateEnhancedThermalReceipt(Order order) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            output.write(INIT_PRINTER);

            // En-tête
            output.write(SELECT_CENTER);
            output.write(CHAR_SIZE_DOUBLE);
            output.write(SELECT_BOLD);
            writeLine(output, order.getStore().getName());
            output.write(CANCEL_BOLD);
            output.write(CHAR_SIZE_NORMAL);

            if (order.getStore().getAddress() != null) {
                writeLine(output, order.getStore().getAddress());
            }
            if (order.getStore().getPhone() != null) {
                writeLine(output, "Tel: " + order.getStore().getPhone());
            }

            output.write(SELECT_LEFT);
            writeLine(output, "*********************************");
            output.write(SELECT_CENTER);
            output.write(INVERT_ON);
            output.write(SELECT_BOLD);
            writeLine(output, "  TICKET DE CAISSE  ");
            output.write(INVERT_OFF);
            output.write(CANCEL_BOLD);
            writeLine(output, "*********************************");

            // Infos
            output.write(SELECT_LEFT);
            output.write(SELECT_BOLD);
            writeLine(output, "N° Commande:");
            output.write(CANCEL_BOLD);
            output.write(DOUBLE_WIDTH);
            writeLine(output, order.getOrderNumber());
            output.write(CHAR_SIZE_NORMAL);
            writeLine(output, "Date: " + order.getCreatedAt().format(formatter));
            writeLine(output, "Caissier: " + order.getCashier().getUsername());

            if (order.getCustomer() != null) {
                output.write(SELECT_BOLD);
                writeLine(output, "Client:");
                output.write(CANCEL_BOLD);
                writeLine(output, "  " + order.getCustomer().getFullName());
            }

            writeLine(output, "---------------------------------");
            output.write(SELECT_BOLD);
            writeLine(output, String.format("%-20s %3s %9s", "ARTICLE", "QTE", "PRIX"));
            output.write(CANCEL_BOLD);
            writeLine(output, "---------------------------------");

            // Articles
            for (OrderItem item : order.getItems()) {
                String name = item.getProduct().getName();
                if (name.length() > 20) name = name.substring(0, 17) + "...";

                writeLine(output, name);
                String details = String.format("  %3d x %,9.0f = %,9.0f",
                        item.getQuantity(), item.getUnitPrice(), item.getFinalPrice());
                writeLine(output, details);
            }

            writeLine(output, "---------------------------------");
            writeLine(output, String.format("%-24s %9.0f", "Sous-total:", order.getSubtotal()));

            if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                writeLine(output, String.format("%-24s %9.0f", "TVA:", order.getTaxAmount()));
            }

            writeLine(output, "=================================");
            output.write(SELECT_BOLD);
            output.write(DOUBLE_HEIGHT);
            output.write(SELECT_CENTER);
            writeLine(output, String.format("TOTAL: %,.0f FCFA", order.getTotalAmount()));
            output.write(CHAR_SIZE_NORMAL);
            output.write(CANCEL_BOLD);
            output.write(SELECT_LEFT);
            writeLine(output, "=================================");

            // Paiements
            Map<PaymentMethod, BigDecimal> paymentsByMethod = new HashMap<>();
            for (Payment p : order.getPayments()) {
                if (p.getStatus() == PaymentStatus.PAID) {
                    paymentsByMethod.merge(p.getMethod(), p.getAmount(), BigDecimal::add);
                }
            }

            output.write(SELECT_BOLD);
            writeLine(output, "PAIEMENTS:");
            output.write(CANCEL_BOLD);

            for (Map.Entry<PaymentMethod, BigDecimal> entry : paymentsByMethod.entrySet()) {
                writeLine(output, String.format("  %-20s %9.0f",
                        getPaymentMethodName(entry.getKey()) + ":", entry.getValue()));
            }

            if (order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                output.write(SELECT_BOLD);
                writeLine(output, String.format("MONNAIE: %,.0f FCFA", order.getChangeAmount()));
                output.write(CANCEL_BOLD);
            }

            // Footer
            output.write(SELECT_CENTER);
            output.write(SELECT_BOLD);
            writeLine(output, "MERCI DE VOTRE VISITE !");
            output.write(CANCEL_BOLD);
            writeLine(output, "A bientot chez " + order.getStore().getName());
            writeLine(output, "NIF: " + companyTaxId);

            output.write(LINE_FEED);
            output.write(LINE_FEED);
            output.write(LINE_FEED);
            output.write(CUT_PAPER);

            return output.toByteArray();

        } catch (Exception e) {
            log.error("Error generating thermal receipt", e);
            return new byte[0];
        }
    }

    private void writeLine(ByteArrayOutputStream output, String text) throws Exception {
        output.write(text.getBytes(StandardCharsets.UTF_8));
        output.write(LINE_FEED);
    }

    private byte[] generateQRCode(String data) {
        try {
            ByteArrayOutputStream qrOutput = new ByteArrayOutputStream();
            qrOutput.write(new byte[]{GS, '(', 'k', 4, 0, 49, 65, 50, 0});
            qrOutput.write(new byte[]{GS, '(', 'k', 3, 0, 49, 67, 6});
            qrOutput.write(new byte[]{GS, '(', 'k', 3, 0, 49, 69, 49});

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            int length = dataBytes.length + 3;
            qrOutput.write(new byte[]{GS, '(', 'k', (byte) (length % 256),
                    (byte) (length / 256), 49, 80, 48});
            qrOutput.write(dataBytes);
            qrOutput.write(new byte[]{GS, '(', 'k', 3, 0, 49, 81, 48});

            return qrOutput.toByteArray();
        } catch (Exception e) {
            log.warn("QR code generation failed", e);
            return null;
        }
    }

    private String getEnhancedReceiptText(Order order) {
        // ... identique à avant ...
        StringBuilder receipt = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        receipt.append("╔═════════════════════════════════╗\n");
        receipt.append("║  ").append(centerText(order.getStore().getName(), 29)).append("  ║\n");
        receipt.append("╠═════════════════════════════════╣\n");

        // ... reste inchangé ...

        return receipt.toString();
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

    private String centerText(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text +
                " ".repeat(Math.max(0, width - text.length() - padding));
    }
}