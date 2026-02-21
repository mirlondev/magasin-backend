package org.odema.posnew.application.serviceImpl;

import lombok.extern.slf4j.Slf4j;

import org.odema.posnew.application.dto.InvoiceResponse;
import org.odema.posnew.application.mapper.InvoiceMapper;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.design.builder.impl.InvoiceDocumentBuilder;
import org.odema.posnew.design.context.DocumentBuildContext;
import org.odema.posnew.design.decorator.QRCodeDecorator;
import org.odema.posnew.design.decorator.WatermarkDecorator;
import org.odema.posnew.design.event.InvoiceGeneratedEvent;
import org.odema.posnew.design.factory.DocumentBuilderFactory;
import org.odema.posnew.design.factory.DocumentStrategyFactory;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.template.DocumentServiceTemplate;

import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.domain.model.Invoice;

import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.Payment;
import org.odema.posnew.domain.model.enums.*;
import org.odema.posnew.domain.repository.InvoiceRepository;
import org.odema.posnew.domain.repository.OrderRepository;
import org.odema.posnew.domain.service.DocumentNumberService;
import org.odema.posnew.domain.service.FileStorageService;
import org.odema.posnew.domain.service.InvoiceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class InvoiceServiceImpl
        extends DocumentServiceTemplate<Invoice, InvoiceResponse>
        implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @Value("${app.file.directories.invoices:invoices}")
    private String invoicesDirectory;

    @Value("${app.invoice.qrcode.enabled:true}")
    private boolean qrCodeEnabled;

    @Value("${app.invoice.watermark.draft:BROUILLON}")
    private String draftWatermark;

    @Value("${app.invoice.default-validity-days:30}")
    private int defaultValidityDays;

    // Base path for file storage (configure this in application.properties)
    @Value("${app.file.storage.base-path:./uploads}")
    private String storageBasePath;

    public InvoiceServiceImpl(
            OrderRepository orderRepository,
            DocumentStrategyFactory strategyFactory,
            DocumentNumberService documentNumberService,
            FileStorageService fileStorageService,
            DocumentBuilderFactory builderFactory,        // ✅ AJOUTÉ
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        super(orderRepository, strategyFactory, documentNumberService,
                fileStorageService, builderFactory);
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper     = invoiceMapper;
        this.eventPublisher    = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    // =========================================================================
    // PDF GENERATION WITH FILE CACHING - NEW METHOD
    // =========================================================================

    /**
     * Get or generate PDF invoice with file-based caching.
     * Checks disk first, generates only if file doesn't exist.
     */
    @Override
    protected DocumentBuildContext buildContext(Invoice invoice) {
        return DocumentBuildContext.forOrder(invoice.getOrder());
    }
    @Override
    public byte[] getOrGenerateInvoicePdf(UUID orderId) throws IOException {
        // 1. Check if invoice exists in database
        Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucune facture trouvée pour la commande: " + orderId
                ));

        // 2. Check if PDF file already exists on disk
        if (invoice.getPdfFilename() != null && !invoice.getPdfFilename().isBlank()) {
            Path pdfPath = getInvoicePdfPath(invoice.getPdfFilename());

            if (Files.exists(pdfPath)) {
                log.info("PDF trouvé sur disque pour facture {}: {}",
                        invoice.getInvoiceNumber(), pdfPath);

                // Increment print count for tracking
                invoice.incrementPrintCount();
                invoiceRepository.save(invoice);

                // Read and return existing file
                return Files.readAllBytes(pdfPath);
            }
        }

        // 3. File doesn't exist - generate new PDF
        log.info("PDF non trouvé sur disque, génération pour facture {}",
                invoice.getInvoiceNumber());

        byte[] pdfBytes = buildPdfWithDecorators(invoice);

        // 4. Save to disk
        String filename = generatePdfFilename(invoice);
        Path pdfPath = getInvoicePdfPath(filename);

        // Ensure directory exists
        Files.createDirectories(pdfPath.getParent());
        Files.write(pdfPath, pdfBytes);

        // 5. Update invoice with file path
        invoice.setPdfFilename(filename);
        invoice.setPdfPath(pdfPath.toString());
        invoice.incrementPrintCount();
        invoiceRepository.save(invoice);

        log.info("PDF généré et sauvegardé: {} ({} bytes)", pdfPath, pdfBytes.length);

        return pdfBytes;
    }

    /**
     * Force regeneration of PDF (for reprint with updated data)
     */
    @Override
    public byte[] regenerateInvoicePdf(UUID orderId) throws IOException {
        Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucune facture trouvée pour la commande: " + orderId
                ));

        // Delete old file if exists
        if (invoice.getPdfFilename() != null) {
            Path oldPath = getInvoicePdfPath(invoice.getPdfFilename());
            try {
                Files.deleteIfExists(oldPath);
                log.debug("Ancien PDF supprimé: {}", oldPath);
            } catch (IOException e) {
                log.warn("Impossible de supprimer l'ancien PDF: {}", oldPath);
            }
        }

        // Generate new PDF
        byte[] pdfBytes = buildPdfWithDecorators(invoice);

        // Save with new filename (include timestamp to avoid cache issues)
        String filename = generatePdfFilename(invoice) + "_ regenerated_" +
                System.currentTimeMillis();
        Path pdfPath = getInvoicePdfPath(filename);

        Files.createDirectories(pdfPath.getParent());
        Files.write(pdfPath, pdfBytes);

        invoice.setPdfFilename(filename);
        invoice.setPdfPath(pdfPath.toString());
        invoice.incrementPrintCount();
        invoiceRepository.save(invoice);

        return pdfBytes;
    }

    /**
     * Get PDF file as Resource for streaming (memory efficient for large files)
     */
    @Override
    public Resource getInvoicePdfResource(UUID orderId) throws IOException {
        Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucune facture trouvée pour la commande: " + orderId
                ));

        if (invoice.getPdfFilename() == null) {
            throw new NotFoundException("PDF non généré pour la facture: " + invoice.getInvoiceNumber());
        }

        Path pdfPath = getInvoicePdfPath(invoice.getPdfFilename());

        if (!Files.exists(pdfPath)) {
            throw new NotFoundException("Fichier PDF non trouvé: " + pdfPath);
        }

        return new PathResource(pdfPath);
    }

    // =========================================================================
    // EXISTING METHODS (keep for backward compatibility)
    // =========================================================================

    @Override
    public byte[] generateInvoicePdf(UUID invoiceId) throws IOException {
        Invoice invoice = loadDocument(invoiceId);

        // Check disk first
        if (invoice.getPdfFilename() != null) {
            Path pdfPath = getInvoicePdfPath(invoice.getPdfFilename());
            if (Files.exists(pdfPath)) {
                invoice.incrementPrintCount();
                invoiceRepository.save(invoice);
                return Files.readAllBytes(pdfPath);
            }
        }

        // Generate and save
        byte[] pdfBytes = buildPdfWithDecorators(invoice);
        savePdfToDisk(invoice, pdfBytes);

        invoice.incrementPrintCount();
        invoiceRepository.save(invoice);

        return pdfBytes;
    }

    // =========================================================================
    // INTERFACE METHODS - InvoiceService
    // =========================================================================

    @Override
    public InvoiceResponse generateInvoice(UUID orderId) {
        try {
            return generateDocument(orderId);
        } catch (Exception e) {
            log.error("Erreur génération facture pour commande {}", orderId, e);
            throw new RuntimeException("Erreur génération facture: " + e.getMessage(), e);
        }
    }

    @Override
    public InvoiceResponse getInvoiceById(UUID invoiceId) {
        Invoice invoice = loadDocument(invoiceId);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Facture non trouvée avec le numéro: " + invoiceNumber
                ));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByOrder(UUID orderId) {
        Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucune facture trouvée pour la commande: " + orderId
                ));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public List<InvoiceResponse> getInvoicesByCustomer(UUID customerId) {
        return invoiceRepository.findByCustomer_CustomerId(customerId)
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public List<InvoiceResponse> getInvoicesByStore(UUID storeId) {
        return invoiceRepository.findByStore_StoreId(storeId)
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public List<InvoiceResponse> getInvoicesByStatus(String status) {
        try {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            return invoiceRepository.findByStatus(invoiceStatus)
                    .stream()
                    .map(invoiceMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de facture invalide: " + status +
                    ". Valeurs acceptées: " + List.of(InvoiceStatus.values()));
        }
    }

    @Override
    public List<InvoiceResponse> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début doit être avant la date de fin");
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime   = endDate.atTime(23, 59, 59);

        return invoiceRepository.findByDateRange(startDateTime, endDateTime)
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public String getInvoicePdfUrl(UUID invoiceId) {
        Invoice invoice = loadDocument(invoiceId);
        if (invoice.getPdfFilename() == null || invoice.getPdfFilename().isBlank()) {
            throw new BadRequestException(
                    "Aucun PDF généré pour la facture: " + invoice.getInvoiceNumber()
            );
        }
        return fileStorageService.getFileUrl(invoice.getPdfFilename(), invoicesDirectory);
    }

    @Override
    @Transactional
    public InvoiceResponse updateInvoiceStatus(UUID invoiceId, String status) {
        Invoice invoice = loadDocument(invoiceId);

        try {
            InvoiceStatus newStatus = InvoiceStatus.valueOf(status.toUpperCase());

            // Règles métier sur les transitions de statut
            validateStatusTransition(invoice.getStatus(), newStatus);

            invoice.setStatus(newStatus);
            invoice.setUpdatedAt(LocalDateTime.now());

            // Actions spécifiques selon nouveau statut
            if (newStatus == InvoiceStatus.PAID) {
                invoice.setAmountPaid(invoice.getTotalAmount());
                invoice.setAmountDue(BigDecimal.ZERO);
            } else if (newStatus == InvoiceStatus.OVERDUE) {
                log.warn("Facture {} marquée comme en retard", invoice.getInvoiceNumber());
            }

            Invoice updated = invoiceRepository.save(invoice);
            log.info("Statut facture {} mis à jour: {} → {}",
                    invoice.getInvoiceNumber(), invoice.getStatus(), newStatus);

            return invoiceMapper.toResponse(updated);

        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut invalide: " + status +
                    ". Valeurs acceptées: " + List.of(InvoiceStatus.values()));
        }
    }

    @Override
    @Transactional
    public InvoiceResponse markInvoiceAsPaid(UUID invoiceId, String paymentMethod) {
        Invoice invoice = loadDocument(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException(
                    "La facture " + invoice.getInvoiceNumber() + " est déjà payée"
            );
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException(
                    "Impossible de marquer une facture annulée comme payée"
            );
        }

        // Valider la méthode de paiement
        try {
            PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Méthode de paiement invalide: " + paymentMethod);
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(paymentMethod.toUpperCase());
        invoice.setAmountPaid(invoice.getTotalAmount());
        invoice.setAmountDue(BigDecimal.ZERO);
        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice updated = invoiceRepository.save(invoice);
        log.info("Facture {} marquée comme payée via {}",
                invoice.getInvoiceNumber(), paymentMethod);

        return invoiceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(UUID invoiceId) {
        Invoice invoice = loadDocument(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException(
                    "Impossible d'annuler une facture payée. Créez un avoir à la place."
            );
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cette facture est déjà annulée");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice updated = invoiceRepository.save(invoice);
        log.info("Facture {} annulée", invoice.getInvoiceNumber());

        return invoiceMapper.toResponse(updated);
    }
    @Override
    protected UUID getDocumentId(Invoice document) {
        return document.getInvoiceId();
    }
    @Override
    public void sendInvoiceByEmail(UUID invoiceId, String email) throws Exception {
        Invoice invoice = loadDocument(invoiceId);

        if (email == null || email.isBlank()) {
            throw new BadRequestException("L'adresse email est requise");
        }

        // Générer le PDF (or get from disk)
        byte[] pdfBytes = getOrGenerateInvoicePdf(invoice.getOrder().getOrderId());

        // TODO: Intégration service email (Spring Mail / SendGrid / etc.)
        // emailService.sendInvoice(invoice, email, pdfBytes);

        log.info("Facture {} envoyée par email à {} (implémentation TODO)",
                invoice.getInvoiceNumber(), email);
    }

    @Override
    public List<InvoiceResponse> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDateTime.now())
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public Double getTotalOutstandingAmount() {
        Double total = invoiceRepository.getTotalOutstandingAmount();
        return total != null ? total : 0.0;
    }

    @Override
    @Transactional
    public InvoiceResponse reprintInvoice(UUID invoiceId) {
        try {
            return reprintDocument(invoiceId);
        } catch (Exception e) {
            log.error("Erreur réimpression facture {}", invoiceId, e);
            throw new RuntimeException("Erreur réimpression: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public InvoiceResponse convertProformaToSale(UUID proformaId) {
        Invoice proforma = loadDocument(proformaId);

        if (!proforma.isProforma()) {
            throw new BadRequestException("Ce document n'est pas un proforma");
        }
        if (!proforma.canBeConverted()) {
            throw new BadRequestException(
                    "Ce proforma ne peut pas être converti. " +
                            "Statut actuel: " + proforma.getStatus() +
                            (proforma.getConvertedToSale() ? " (déjà converti)" : "")
            );
        }

        Order originalOrder = proforma.getOrder();

        // Pour l'instant: marquer comme converti sur la même commande
        proforma.markAsConverted(originalOrder);
        Invoice updated = invoiceRepository.save(proforma);

        log.info("Proforma {} converti en vente", proforma.getInvoiceNumber());
        return invoiceMapper.toResponse(updated);
    }

    // =========================================================================
    // IMPLÉMENTATION MÉTHODES ABSTRAITES (Template Method)
    // =========================================================================

    @Override
    protected void checkExistingDocument(Order order) {
        invoiceRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(invoice -> {
            throw new BadRequestException(
                    "Une facture existe déjà pour cette commande: " + invoice.getInvoiceNumber()
            );
        });
    }

    @Override
    protected String generateUniqueDocumentNumber(Order order, DocumentStrategy strategy) {
        InvoiceType type = determineInvoiceType(order);
        return documentNumberService.generateInvoiceNumber(type);
    }

    @Override
    protected Invoice createDocumentEntity(Order order,
                                           DocumentStrategy strategy,
                                           String documentNumber) {
        InvoiceType type = determineInvoiceType(order);
        List<Payment> payments = order.getPayments();

        // Calculer montants depuis les paiements
        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditAmount = payments.stream()
                .filter(p -> p.getMethod() == PaymentMethod.CREDIT)
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        InvoiceStatus status = determineInvoiceStatus(order, totalPaid, creditAmount);
        LocalDateTime dueDate = extractDueDateFromPayments(payments);
        Integer validityDays = (type == InvoiceType.PROFORMA) ? defaultValidityDays : null;

        // Méthode de paiement principale (première méthode non-crédit)
        String paymentMethodStr = payments.stream()
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT)
                .map(p -> p.getMethod().name())
                .findFirst()
                .orElseGet(() -> {
                    // ✅ getPrimaryPaymentMethod() est @Transient dans Order
                    // order.getPaymentMethod() n'existe pas — c'était un champ fantôme
                    PaymentMethod primary = order.getPrimaryPaymentMethod();
                    return primary != null ? primary.name() : null;
                });

        return Invoice.builder()
                .invoiceNumber(documentNumber)
                .invoiceType(type)
                .status(status)
                .order(order)
                .customer(order.getCustomer())
                .store(order.getStore())
                .invoiceDate(LocalDateTime.now())
                .paymentDueDate(dueDate)
                .validityDays(validityDays)
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getGlobalDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .amountPaid(totalPaid)
                .amountDue(order.getTotalAmount()
                        .subtract(totalPaid)
                        .subtract(creditAmount)
                        .max(BigDecimal.ZERO))
                .paymentMethod(paymentMethodStr)
                .notes(buildInvoiceNotes(order, payments, type))
                .printCount(0)
                .convertedToSale(false)
                .isActive(true)
                .build();
    }

//    @Override
//    protected byte[] generatePdfDocument(Invoice invoice,
//                                         DocumentStrategy strategy) throws DocumentException {
//        byte[] pdfBytes = buildPdfWithDecorators(invoice);
//
//        // Save to disk immediately after generation
//        try {
//            savePdfToDisk(invoice, pdfBytes);
//        } catch (IOException e) {
//            log.error("Erreur sauvegarde PDF sur disque", e);
//            // Continue - PDF is still returned even if disk save fails
//        }
//
//        return pdfBytes;
//    }
// InvoiceServiceImpl — surcharge uniquement pour les décorateurs
@Override
protected byte[] generatePdfDocument(Invoice invoice, DocumentStrategy strategy) throws  IOException{

    DocumentType docType = mapInvoiceTypeToDocumentType(invoice.getInvoiceType());
    DocumentBuildContext ctx = DocumentBuildContext.forOrder(invoice.getOrder());

    // Construire le builder via factory
    DocumentBuilder builder = builderFactory.createBuilder(docType, ctx);

    // Appliquer les décorateurs
    if (invoice.getStatus() == InvoiceStatus.DRAFT) {
        builder = new WatermarkDecorator(builder, draftWatermark);
    }
    if (qrCodeEnabled) {
        builder = new QRCodeDecorator(builder, buildQRCodeData(invoice));
    }

    byte[] pdf = builder
            .initialize()
            .addHeader()
            .addMainInfo()
            .addItemsTable()
            .addTotals()
            .addFooter()
            .build();

    // Sauvegarder sur disque immédiatement
    try { savePdfToDisk(invoice, pdf); }
    catch (IOException e) { log.error("Erreur sauvegarde PDF disque", e); }

    return pdf;
}

    @Override
    protected void updateDocumentWithPdfPath(Invoice invoice, String pdfPath) {
        String filename = pdfPath.substring(pdfPath.lastIndexOf('/') + 1);
        invoice.setPdfFilename(filename);
        invoice.setPdfPath(pdfPath);
    }

    @Override
    protected Invoice afterDocumentGeneration(Invoice invoice,
                                              Order order,
                                              DocumentStrategy strategy) {
        return invoice;
    }

    @Override
    protected Invoice saveDocument(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Override
    protected void publishDocumentEvent(Invoice invoice, Order order) {
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(this, invoice, order));
    }

    @Override
    protected InvoiceResponse mapToResponse(Invoice invoice) {
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    protected Invoice loadDocument(UUID documentId) {
        return invoiceRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        "Facture non trouvée: " + documentId
                ));
    }

    @Override
    protected void checkReprintAllowed(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException(
                    "Impossible de réimprimer une facture annulée: " + invoice.getInvoiceNumber()
            );
        }
    }

    @Override
    protected void incrementPrintCount(Invoice invoice) {
        invoice.incrementPrintCount();
    }

    @Override
    protected void updateReprintStatus(Invoice invoice) {
        // Les factures ne changent pas de statut lors de la réimpression
    }

    @Override
    protected void logReprint(Invoice invoice) {
        log.info("Facture {} réimprimée (impression #{})",
                invoice.getInvoiceNumber(), invoice.getPrintCount());
    }

    @Override
    protected String getStorageDirectory() {
        return invoicesDirectory;
    }

    // =========================================================================
    // FILE STORAGE HELPER METHODS
    // =========================================================================

    private Path getInvoicePdfPath(String filename) {
        return Paths.get(storageBasePath, invoicesDirectory, filename);
    }

    private String generatePdfFilename(Invoice invoice) {
        return String.format("INV_%s_%s.pdf",
                invoice.getInvoiceNumber(),
                invoice.getInvoiceId().toString().substring(0, 8));
    }

    private void savePdfToDisk(Invoice invoice, byte[] pdfBytes) throws IOException {
        String filename = generatePdfFilename(invoice);
        Path pdfPath = getInvoicePdfPath(filename);

        // Ensure directory exists
        Files.createDirectories(pdfPath.getParent());

        // Write file
        Files.write(pdfPath, pdfBytes);

        // Update invoice entity
        invoice.setPdfFilename(filename);
        invoice.setPdfPath(pdfPath.toString());

        log.debug("PDF sauvegardé sur disque: {}", pdfPath);
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // =========================================================================

    /**
     * Construit le PDF en appliquant les decorators appropriés (Decorator Pattern)
     */
    private byte[] buildPdfWithDecorators(Invoice invoice) throws IOException {
        DocumentBuilder builder = new InvoiceDocumentBuilder(invoice.getOrder());

        // Appliquer watermark si brouillon
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            builder = new WatermarkDecorator(builder, draftWatermark);
        }

        // Appliquer QR code si activé
        if (qrCodeEnabled) {
            String qrData = buildQRCodeData(invoice);
            builder = new QRCodeDecorator(builder, qrData);
        }

        return builder
                .initialize()
                .addHeader()
                .addMainInfo()
                .addItemsTable()
                .addTotals()
                .addFooter()
                .build();
    }

    /**
     * Détermine le type de facture selon le type de commande
     */
    private InvoiceType determineInvoiceType(Order order) {
        return switch (order.getOrderType()) {
            case CREDIT_SALE -> InvoiceType.CREDIT_SALE;
            case PROFORMA    -> InvoiceType.PROFORMA;
            default          -> InvoiceType.CREDIT_SALE;
        };
    }

    /**
     * Détermine le statut initial de la facture
     */
    private InvoiceStatus determineInvoiceStatus(Order order,
                                                 BigDecimal totalPaid,
                                                 BigDecimal creditAmount) {
        if (totalPaid.compareTo(order.getTotalAmount()) >= 0) {
            return InvoiceStatus.PAID;
        }
        if (creditAmount.compareTo(BigDecimal.ZERO) > 0 || totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            return InvoiceStatus.ISSUED;
        }
        return InvoiceStatus.DRAFT;
    }

    /**
     * Valide la transition de statut (règles métier)
     */
    private void validateStatusTransition(InvoiceStatus current, InvoiceStatus next) {
        // Facture payée → impossible de revenir en arrière (sauf CANCELLED pour avoir)
        if (current == InvoiceStatus.PAID && next != InvoiceStatus.CANCELLED) {
            throw new BadRequestException(
                    "Une facture payée ne peut pas passer au statut: " + next
            );
        }
        // Facture annulée → aucun changement possible
        if (current == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Impossible de modifier une facture annulée");
        }
        // Facture convertie → aucun changement possible
        if (current == InvoiceStatus.CONVERTED) {
            throw new BadRequestException("Impossible de modifier un proforma converti");
        }
    }

    /**
     * Extrait la date d'échéance depuis les notes de paiement crédit
     */
    private LocalDateTime extractDueDateFromPayments(List<Payment> payments) {
        for (Payment payment : payments) {
            if (payment.getMethod() == PaymentMethod.CREDIT
                    && payment.getNotes() != null
                    && payment.getNotes().contains("Échéance:")) {
                try {
                    String notes   = payment.getNotes();
                    String dateStr = notes.substring(notes.indexOf("Échéance:") + 10)
                            .split("\n")[0]
                            .trim();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    return LocalDate.parse(dateStr, fmt).atStartOfDay();
                } catch (Exception e) {
                    log.warn("Impossible de parser la date d'échéance depuis: {}",
                            payment.getNotes());
                }
            }
        }
        return LocalDateTime.now().plusDays(defaultValidityDays);
    }

    /**
     * Construit les notes de la facture
     */
    private String buildInvoiceNotes(Order order, List<Payment> payments, InvoiceType type) {
        StringBuilder notes = new StringBuilder();

        if (type == InvoiceType.PROFORMA) {
            notes.append("PROFORMA / DEVIS\n");
            notes.append("Validité: ").append(defaultValidityDays).append(" jours\n");
        }

        payments.stream()
                .filter(p -> p.getMethod() == PaymentMethod.CREDIT)
                .map(Payment::getNotes)
                .filter(n -> n != null && n.contains("Échéance:"))
                .findFirst()
                .ifPresent(creditNotes -> {
                    notes.append("Vente à crédit\n");
                    notes.append(creditNotes).append("\n");
                });

        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            notes.append(order.getNotes());
        }

        return notes.toString().trim();
    }

    /**
     * Construit les données encodées dans le QR code
     */
    private String buildQRCodeData(Invoice invoice) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return String.format("INVOICE|%s|%s|%s",
                invoice.getInvoiceNumber(),
                invoice.getTotalAmount().toPlainString(),
                invoice.getInvoiceDate().format(fmt));
    }



    // =========================================================================
// getOrGenerate* — délégation vers le bon type d'InvoiceType
// Chaque méthode cherche la facture par commande + type, génère si absent
// =========================================================================

    @Override
    public byte[] getOrGenerateProformaPdf(UUID orderId) throws IOException {
        return getOrGenerateByType(orderId, InvoiceType.PROFORMA);
    }

    @Override
    public byte[] getOrGenerateCreditNotePdf(UUID orderId) throws IOException {
        return getOrGenerateByType(orderId, InvoiceType.CREDIT_NOTE);
    }

    @Override
    public byte[] getOrGenerateDeliveryNotePdf(UUID orderId) throws IOException {
        return getOrGenerateByType(orderId, InvoiceType.DELIVERY_NOTE);
    }

    @Override
    public byte[] getOrGeneratePurchaseOrderPdf(UUID orderId) throws IOException {
        return getOrGenerateByType(orderId, InvoiceType.PURCHASE_ORDER);
    }

    @Override
    public byte[] getOrGenerateQuotePdf(UUID orderId) throws IOException {
        return getOrGenerateByType(orderId, InvoiceType.QUOTE);
    }

    @Override
    public byte[] getOrGenerateCorrectedInvoicePdf(UUID orderId) throws IOException {
        return getOrGenerateByType(orderId, InvoiceType.INVOICE_CORRECTED);
    }

    /**
     * Méthode générique : cherche la facture par ordre + type,
     * la génère si elle n'existe pas encore, puis retourne le PDF.
     */
    private byte[] getOrGenerateByType(UUID orderId, InvoiceType type)
            throws IOException {

        // 1. Chercher une facture existante du bon type pour cette commande
        Invoice invoice = invoiceRepository
                .findByOrder_OrderIdAndInvoiceType(orderId, type)
                .orElse(null);

        // 2. Si elle n'existe pas encore, la générer
        if (invoice == null) {
            log.info("Aucune facture {} trouvée pour commande {}, génération...", type, orderId);
            generateInvoice(orderId);   // crée et sauvegarde via Template Method
            invoice = invoiceRepository
                    .findByOrder_OrderIdAndInvoiceType(orderId, type)
                    .orElseThrow(() -> new NotFoundException(
                            "Impossible de générer la facture " + type + " pour la commande: " + orderId
                    ));
        }

        // 3. Vérifier le PDF sur disque
        if (invoice.getPdfFilename() != null) {
            Path pdfPath = getInvoicePdfPath(invoice.getPdfFilename());
            if (Files.exists(pdfPath)) {
                invoice.incrementPrintCount();
                invoiceRepository.save(invoice);
                return Files.readAllBytes(pdfPath);
            }
        }

        // 4. Régénérer le PDF
        byte[] pdfBytes = buildPdfWithDecorators(invoice);
        savePdfToDisk(invoice, pdfBytes);
        invoice.incrementPrintCount();
        invoiceRepository.save(invoice);
        return pdfBytes;
    }

    private DocumentType mapInvoiceTypeToDocumentType(InvoiceType type) {
        return switch (type) {
            case  CREDIT_SALE, PURCHASE_ORDER, INVOICE_CORRECTED -> DocumentType.INVOICE;
            case PROFORMA, QUOTE       -> DocumentType.PROFORMA;
            case CREDIT_NOTE           -> DocumentType.CREDIT_NOTE;
            case DELIVERY_NOTE         -> DocumentType.DELIVERY_NOTE;
        };

    }

}