package org.odema.posnew.service.impl;

import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.design.builder.impl.InvoiceDocumentBuilder;
import org.odema.posnew.design.decorator.QRCodeDecorator;
import org.odema.posnew.design.decorator.WatermarkDecorator;
import org.odema.posnew.design.event.InvoiceGeneratedEvent;
import org.odema.posnew.design.facade.DocumentGenerationFacade;
import org.odema.posnew.dto.response.InvoiceResponse;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.InvoiceStatus;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.InvoiceMapper;
import org.odema.posnew.repository.InvoiceRepository;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.service.FileStorageService;
import org.odema.posnew.service.InvoiceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final InvoiceMapper invoiceMapper;
    private final FileStorageService fileStorageService;
    private final DocumentGenerationFacade documentFacade;
    private final ApplicationEventPublisher eventPublisher; // ✅ AJOUT

    @Value("${app.file.directories.invoices:invoices}")
    private String invoicesDirectory;

    @Value("${app.invoice.qrcode.enabled:true}")
    private boolean qrCodeEnabled;

    @Value("${app.invoice.watermark.draft:BROUILLON}")
    private String draftWatermark;

    // ============================================================================
    // GÉNÉRATION DE FACTURE
    // ============================================================================

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')") // ✅ AJOUT
    public InvoiceResponse generateInvoice(UUID orderId) throws IOException, DocumentException {
        log.info("Génération facture pour commande {}", orderId);

        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // ✅ AJOUT : Validations métier
        validateOrderForInvoicing(order);

        // Générer numéro facture
        String invoiceNumber = generateInvoiceNumber();

        // Calculer montants
        List<Payment> payments = order.getPayments();
        BigDecimal totalPaid = calculateTotalPaid(payments);
        BigDecimal creditAmount = calculateCreditAmount(payments);

        // Déterminer statut
        InvoiceStatus status = determineInvoiceStatus(totalPaid, creditAmount, order.getTotalAmount());

        // Date d'échéance
        LocalDateTime dueDate = extractDueDateFromPayments(payments);

        // Créer facture
        Invoice invoice = buildInvoice(order, invoiceNumber, status, dueDate,
                totalPaid, creditAmount, payments);

        // Sauvegarder
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // ✅ AJOUT : Publier événement
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(this, savedInvoice));

        // Générer PDF avec decorators
        byte[] pdfBytes = generateInvoicePdfWithDecorators(savedInvoice);
        saveInvoicePdf(savedInvoice, pdfBytes);

        log.info("Facture {} générée avec succès pour commande {}",
                invoiceNumber, order.getOrderNumber());

        return invoiceMapper.toResponse(savedInvoice);
    }

    /**
     * ✅ AJOUT : Validation centralisée
     */
    private void validateOrderForInvoicing(Order order) {
        // Vérifier statut commande
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Impossible de générer une facture pour une commande annulée"
            );
        }

        // Vérifier si déjà facturé
        invoiceRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(invoice -> {
            throw new BadRequestException(
                    "Une facture existe déjà: " + invoice.getInvoiceNumber()
            );
        });

        // Client obligatoire
        if (order.getCustomer() == null) {
            throw new BadRequestException(
                    "Un client est requis pour générer une facture"
            );
        }
    }

    /**
     * ✅ AMÉLIORATION : Construction de l'invoice
     */
    private Invoice buildInvoice(Order order, String invoiceNumber,
                                 InvoiceStatus status, LocalDateTime dueDate,
                                 BigDecimal totalPaid, BigDecimal creditAmount,
                                 List<Payment> payments) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .customer(order.getCustomer())
                .store(order.getStore())
                .invoiceDate(LocalDateTime.now())
                .paymentDueDate(dueDate)
                .status(status)
                .paymentMethod(order.getPaymentMethod().name())
                .notes(buildInvoiceNotes(order, payments))
                .isActive(true)
                .build();

        // Définir montants
        invoice.setSubtotal(order.getSubtotal());
        invoice.setTaxAmount(order.getTaxAmount());
        invoice.setDiscountAmount(order.getDiscountAmount());
        invoice.setTotalAmount(order.getTotalAmount());
        invoice.setAmountPaid(totalPaid);
        invoice.setAmountDue(
                order.getTotalAmount()
                        .subtract(totalPaid)
                        .subtract(creditAmount)
        );

        return invoice;
    }

    /**
     * ✅ AMÉLIORATION : Calculs séparés
     */
    private BigDecimal calculateTotalPaid(List<Payment> payments) {
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateCreditAmount(List<Payment> payments) {
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private InvoiceStatus determineInvoiceStatus(BigDecimal totalPaid,
                                                 BigDecimal creditAmount,
                                                 BigDecimal totalAmount) {
        if (totalPaid.compareTo(totalAmount) >= 0) {
            return InvoiceStatus.PAID;
        } else if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            return InvoiceStatus.ISSUED;
        } else {
            return InvoiceStatus.DRAFT;
        }
    }

    /**
     * Génère le PDF avec les decorators appropriés
     */
    private byte[] generateInvoicePdfWithDecorators(Invoice invoice) throws DocumentException {
        InvoiceDocumentBuilder baseBuilder = new InvoiceDocumentBuilder(invoice.getOrder());

        // Appliquer decorators selon contexte
        DocumentBuilder builder = baseBuilder;

        // Watermark si brouillon
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            builder = new WatermarkDecorator(builder, draftWatermark);
        }

        // QR code si activé
        if (qrCodeEnabled) {
            String qrData = buildQRCodeData(invoice);
            builder = new QRCodeDecorator(builder, qrData);
        }

        // Construire le document
        return builder
                .initialize()
                .addHeader()
                .addMainInfo()
                .addItemsTable()
                .addTotals()
                .addFooter()
                .build();
    }

    private String buildQRCodeData(Invoice invoice) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return String.format("INVOICE|%s|%s|%s",
                invoice.getInvoiceNumber(),
                invoice.getTotalAmount().toString(),
                invoice.getInvoiceDate().format(formatter)
        );
    }

    // ============================================================================
    // GESTION DES FACTURES
    // ============================================================================

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')") // ✅ AJOUT
    public byte[] generateInvoicePdf(UUID invoiceId) throws IOException, DocumentException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        return generateInvoicePdfWithDecorators(invoice);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')") // ✅ AJOUT
    public String getInvoicePdfUrl(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        if (invoice.getPdfFilename() == null) {
            throw new BadRequestException("Aucun PDF généré pour cette facture");
        }

        return fileStorageService.getFileUrl(invoice.getPdfFilename(), invoicesDirectory);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public InvoiceResponse updateInvoiceStatus(UUID invoiceId, String status) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        try {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            invoice.setStatus(invoiceStatus);
            invoice.setUpdatedAt(LocalDateTime.now());

            Invoice updatedInvoice = invoiceRepository.save(invoice);

            log.info("Statut facture {} mis à jour: {}",
                    invoice.getInvoiceNumber(), invoiceStatus);

            return invoiceMapper.toResponse(updatedInvoice);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de facture invalide: " + status);
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')") // ✅ AJOUT
    public InvoiceResponse markInvoiceAsPaid(UUID invoiceId, String paymentMethod) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        if (invoice.isPaid()) {
            throw new BadRequestException("La facture est déjà payée");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(paymentMethod);
        invoice.setAmountPaid(invoice.getTotalAmount());
        invoice.setAmountDue(BigDecimal.ZERO);
        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        log.info("Facture {} marquée comme payée", invoice.getInvoiceNumber());

        return invoiceMapper.toResponse(updatedInvoice);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public InvoiceResponse cancelInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Impossible d'annuler une facture payée");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        log.info("Facture {} annulée", invoice.getInvoiceNumber());

        return invoiceMapper.toResponse(updatedInvoice);
    }

    // ============================================================================
    // CONSULTATION
    // ============================================================================

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')") // ✅ AJOUT
    public InvoiceResponse getInvoiceById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')") // ✅ AJOUT
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')") // ✅ AJOUT
    public InvoiceResponse getInvoiceByOrder(UUID orderId) {
        Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucune facture trouvée pour cette commande"
                ));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public List<InvoiceResponse> getInvoicesByCustomer(UUID customerId) {
        return invoiceRepository.findByCustomer_CustomerId(customerId).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public List<InvoiceResponse> getInvoicesByStore(UUID storeId) {
        return invoiceRepository.findByStore_StoreId(storeId.toString()).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public List<InvoiceResponse> getInvoicesByStatus(String status) {
        try {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            return invoiceRepository.findByStatus(invoiceStatus).stream()
                    .map(invoiceMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de facture invalide: " + status);
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public List<InvoiceResponse> getInvoicesByDateRange(LocalDate startDate,
                                                        LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        return invoiceRepository.findByDateRange(startDateTime, endDateTime).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public List<InvoiceResponse> getOverdueInvoices() {
        LocalDateTime now = LocalDateTime.now();
        return invoiceRepository.findOverdueInvoices(now).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public Double getTotalOutstandingAmount() {
        return invoiceRepository.getTotalOutstandingAmount();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')") // ✅ AJOUT
    public void sendInvoiceByEmail(UUID invoiceId, String email) throws Exception {
        log.info("Envoi facture {} à {}", invoiceId, email);
        // TODO: Implémenter envoi email avec JavaMail ou service externe
    }

    // ============================================================================
    // MÉTHODES UTILITAIRES
    // ============================================================================

    private String generateInvoiceNumber() {
        String prefix = "INV";
        String year = String.valueOf(Year.now().getValue());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String sequence = String.format("%04d", getNextInvoiceSequence());

        return prefix + year + month + sequence;
    }

    private Long getNextInvoiceSequence() {
        long count = invoiceRepository.count();
        return count + 1;
    }

    private void saveInvoicePdf(Invoice invoice, byte[] pdfBytes) throws IOException {
        String filename = invoice.getInvoiceNumber() + ".pdf";

        fileStorageService.storeFileFromBytes(pdfBytes, filename, invoicesDirectory);

        invoice.setPdfFilename(filename);
        invoice.setPdfPath(invoicesDirectory + "/" + filename);
        invoiceRepository.save(invoice);
    }

    private String buildInvoiceNotes(Order order, List<Payment> payments) {
        StringBuilder notes = new StringBuilder();

        // Notes crédit
        payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .filter(p -> p.getNotes() != null && p.getNotes().contains("Échéance:"))
                .findFirst()
                .ifPresent(payment -> {
                    notes.append("Vente à crédit\n");
                    notes.append(payment.getNotes()).append("\n");
                });

        // Notes commande
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            notes.append(order.getNotes());
        }

        return notes.toString().trim();
    }

    /**
     * ✅ AMÉLIORATION : Extraction date d'échéance
     */
    private LocalDateTime extractDueDateFromPayments(List<Payment> payments) {
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .filter(p -> p.getNotes() != null && p.getNotes().contains("Échéance:"))
                .findFirst()
                .map(this::parseDueDateFromNotes)
                .orElseGet(() -> LocalDateTime.now().plusDays(30));
    }

    private LocalDateTime parseDueDateFromNotes(Payment payment) {
        try {
            String notes = payment.getNotes();
            String dateStr = notes.substring(notes.indexOf("Échéance:") + 10)
                    .split("\n")[0]
                    .trim();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(dateStr, formatter).atStartOfDay();
        } catch (Exception e) {
            log.warn("Impossible de parser la date d'échéance: {}", payment.getNotes());
            return LocalDateTime.now().plusDays(30);
        }
    }
}