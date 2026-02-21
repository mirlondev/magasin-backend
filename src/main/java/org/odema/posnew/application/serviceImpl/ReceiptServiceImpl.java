package org.odema.posnew.application.serviceImpl;

import com.itextpdf.text.DocumentException;
import lombok.extern.slf4j.Slf4j;

import org.odema.posnew.application.mapper.ReceiptMapper;
import org.odema.posnew.design.context.DocumentBuildContext;
import org.odema.posnew.design.event.ReceiptGeneratedEvent;
import org.odema.posnew.design.factory.DocumentBuilderFactory;
import org.odema.posnew.design.factory.DocumentStrategyFactory;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.template.DocumentServiceTemplate;

import org.odema.posnew.application.dto.response.ReceiptResponse;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.Receipt;

import org.odema.posnew.domain.model.ShiftReport;
import org.odema.posnew.domain.model.enums.DocumentType;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.ReceiptStatus;
import org.odema.posnew.domain.model.enums.ReceiptType;
import org.odema.posnew.domain.repository.OrderRepository;
import org.odema.posnew.domain.repository.ReceiptRepository;
import org.odema.posnew.domain.repository.ShiftReportRepository;
import org.odema.posnew.domain.service.DocumentNumberService;
import org.odema.posnew.domain.service.FileStorageService;
import org.odema.posnew.domain.service.ReceiptService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReceiptServiceImpl
        extends DocumentServiceTemplate<Receipt, ReceiptResponse>
        implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final ReceiptMapper receiptMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.file.directories.receipts:receipts}")
    private String receiptsDirectory;

    public ReceiptServiceImpl(
            OrderRepository orderRepository,
            DocumentStrategyFactory strategyFactory,
            DocumentNumberService documentNumberService,
            FileStorageService fileStorageService,
            DocumentBuilderFactory builderFactory,        // ✅ AJOUTÉ
            ReceiptRepository receiptRepository,
            ShiftReportRepository shiftReportRepository,
            ReceiptMapper receiptMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        super(orderRepository, strategyFactory, documentNumberService,
                fileStorageService, builderFactory);         // ✅ passé au super
        this.receiptRepository     = receiptRepository;
        this.shiftReportRepository = shiftReportRepository;
        this.receiptMapper         = receiptMapper;
        this.eventPublisher        = eventPublisher;
    }

    @Override
    protected DocumentBuildContext buildContext(Receipt receipt) {
        return DocumentBuildContext.forOrder(receipt.getOrder());
    }

    @Override
    @Transactional
    public ReceiptResponse generateReceipt(UUID orderId, ReceiptType type) {
        try {
            // On stocke le type dans un ThreadLocal pour le passer à createDocumentEntity
            ReceiptTypeHolder.set(type);
            return generateDocument(orderId);
        } catch (Exception e) {
            log.error("Erreur génération ticket pour commande {} - type {}", orderId, type, e);
            throw new RuntimeException("Erreur génération ticket: " + e.getMessage(), e);
        } finally {
            ReceiptTypeHolder.clear();
        }
    }

    @Override
    public ReceiptResponse getReceiptById(UUID receiptId) {
        Receipt receipt = loadDocument(receiptId);
        return receiptMapper.toResponse(receipt);
    }

    @Override
    public ReceiptResponse getReceiptByNumber(String receiptNumber) {
        Receipt receipt = receiptRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Ticket non trouvé avec le numéro: " + receiptNumber
                ));
        return receiptMapper.toResponse(receipt);
    }

    @Override
    public ReceiptResponse getReceiptByOrder(UUID orderId) {
        Receipt receipt = receiptRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun ticket trouvé pour la commande: " + orderId
                ));
        return receiptMapper.toResponse(receipt);
    }

    @Override
    @Transactional
    public ReceiptResponse reprintReceipt(UUID receiptId) {
        try {
            return reprintDocument(receiptId);
        } catch (Exception e) {
            log.error("Erreur réimpression ticket {}", receiptId, e);
            throw new RuntimeException("Erreur réimpression: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateReceiptPdf(UUID receiptId) throws IOException {
        Receipt receipt = loadDocument(receiptId);

        // Lire depuis le disque si déjà généré
        if (receipt.getPdfFilename() != null && !receipt.getPdfFilename().isBlank()) {
            try {
                byte[] stored = fileStorageService.readFileAsBytes(
                        receipt.getPdfFilename(), getStorageDirectory()
                );
                if (stored != null && stored.length > 0) {
                    receipt.incrementPrintCount();
                    receiptRepository.save(receipt);
                    log.info("PDF ticket {} servi depuis disque (impression #{})",
                            receipt.getReceiptNumber(), receipt.getPrintCount());
                    return stored;
                }
            } catch (IOException e) {
                log.warn("PDF introuvable sur disque pour {}, régénération...",
                        receipt.getReceiptNumber());
            }
        }

        // Régénérer si absent
        try {
            // Pas de strategy disponible ici — on passe null,
            // generatePdfDocument() gère ce cas via isShiftReceipt()
            byte[] pdfBytes = generatePdfDocument(receipt, null);

            String filename = receipt.getReceiptNumber() + ".pdf";
            fileStorageService.storeFileFromBytes(pdfBytes, filename, getStorageDirectory());

            receipt.setPdfFilename(filename);
            receipt.setPdfPath(getStorageDirectory() + "/" + filename);
            receipt.incrementPrintCount();
            receiptRepository.save(receipt);

            log.info("PDF ticket {} régénéré (impression #{})",
                    receipt.getReceiptNumber(), receipt.getPrintCount());
            return pdfBytes;

        } catch (IOException e) {
            throw new IOException("Erreur génération PDF: " + e.getMessage(), e);
        }
    }
//
//    @Override
//    public byte[] generateReceiptPdf(UUID receiptId) throws IOException {
//        Receipt receipt = loadDocument(receiptId);
//        // Incrémenter compteur (réimpression)
//        receipt.incrementPrintCount();
//        receiptRepository.save(receipt);
//        log.info("PDF ticket {} généré (impression #{})",
//                receipt.getReceiptNumber(), receipt.getPrintCount());
//        try {
//            return buildReceiptPdf(receipt);
//        } catch (DocumentException e) {
//            throw new IOException("Erreur génération PDF: " + e.getMessage(), e);
//        }
//    }

    @Override
    public String generateThermalData(UUID receiptId) {
        Receipt receipt = loadDocument(receiptId);

        // Retourner données en cache si déjà générées
        if (receipt.getThermalData() != null && !receipt.getThermalData().isBlank()) {
            return receipt.getThermalData();
        }

        // Générer et sauvegarder
        String thermalData = buildThermalData(receipt);
        receipt.setThermalData(thermalData);
        receiptRepository.save(receipt);

        return thermalData;
    }

    @Override
    @Transactional
    public ReceiptResponse voidReceipt(UUID receiptId, String reason) {
        Receipt receipt = loadDocument(receiptId);

        if (receipt.isVoid()) {
            throw new BadRequestException(
                    "Le ticket " + receipt.getReceiptNumber() + " est déjà annulé"
            );
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("La raison de l'annulation est obligatoire");
        }

        receipt.setStatus(ReceiptStatus.VOID);
        String existingNotes = receipt.getNotes() != null ? receipt.getNotes() + "\n" : "";
        receipt.setNotes(existingNotes + "ANNULÉ [" + LocalDateTime.now() + "]: " + reason);
        receipt.setUpdatedAt(LocalDateTime.now());

        Receipt updated = receiptRepository.save(receipt);
        log.warn("Ticket {} annulé. Raison: {}", receipt.getReceiptNumber(), reason);

        return receiptMapper.toResponse(updated);
    }

    @Override
    public List<ReceiptResponse> getReceiptsByShift(UUID shiftReportId) {
        return receiptRepository.findByShiftReport_ShiftReportId(shiftReportId)
                .stream()
                .map(receiptMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReceiptResponse> getReceiptsByDateRange(UUID storeId,
                                                        LocalDate startDate,
                                                        LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début doit être avant la date de fin");
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.atTime(23, 59, 59);

        return receiptRepository.findByStoreAndDateRange(storeId, start, end)
                .stream()
                .map(receiptMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReceiptResponse> getReceiptsByCashier(UUID cashierId,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début doit être avant la date de fin");
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.atTime(23, 59, 59);

        return receiptRepository.findByCashierAndDateRange(cashierId, start, end)
                .stream()
                .map(receiptMapper::toResponse)
                .toList();
    }

    // =========================================================================
    // TICKETS SPÉCIAUX (ouverture/fermeture caisse, entrées/sorties)
    // =========================================================================
    @Override
    @Transactional
    public ReceiptResponse generateShiftClosingReceipt(UUID shiftReportId) {
        ShiftReport shift = loadShift(shiftReportId);

        if (shift.getClosingTime() == null) {
            throw new BadRequestException(
                    "Impossible de générer le ticket de fermeture: la session est encore ouverte"
            );
        }

        String receiptNumber = documentNumberService.generateReceiptNumber(
                shift.getStore().getStoreId(), ReceiptType.SHIFT_CLOSING
        );

        String notes = String.format(
                "FERMETURE DE CAISSE\n" +
                        "Fond initial:  %.2f FCFA\n" +
                        "Total ventes:  %.2f FCFA\n" +
                        "Fond final:    %.2f FCFA\n" +
                        "Écart:         %.2f FCFA",
                shift.getOpeningBalance(),
                shift.getTotalSales(),
                shift.getClosingBalance(),
                shift.getClosingBalance().subtract(
                        shift.getOpeningBalance().add(
                                shift.getTotalSales() != null ? shift.getTotalSales() : BigDecimal.ZERO
                        )
                )
        );

        Receipt receipt = buildShiftReceipt(shift, receiptNumber,
                ReceiptType.SHIFT_CLOSING, shift.getTotalSales(),
                shift.getClosingTime(), notes);

        Receipt saved = receiptRepository.save(receipt);
        saveReceiptPdfQuietly(saved);  // ✅ une seule version

        log.info("Ticket fermeture caisse généré: {}", receiptNumber);
        return receiptMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReceiptResponse generateShiftOpeningReceipt(UUID shiftReportId) {
        ShiftReport shift = loadShift(shiftReportId);

        String receiptNumber = documentNumberService.generateReceiptNumber(
                shift.getStore().getStoreId(), ReceiptType.SHIFT_OPENING
        );

        String notes = String.format(
                "OUVERTURE DE CAISSE\nFonds initial: %.2f FCFA\nDate: %s",
                shift.getOpeningBalance(), shift.getOpeningTime()
        );

        Receipt receipt = buildShiftReceipt(shift, receiptNumber,
                ReceiptType.SHIFT_OPENING, shift.getOpeningBalance(),
                shift.getClosingTime(), notes);

        Receipt saved = receiptRepository.save(receipt);
        saveReceiptPdfQuietly(saved);  // ✅ une seule version

        log.info("Ticket ouverture caisse généré: {}", receiptNumber);
        return receiptMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReceiptResponse generatePaymentReceivedReceipt(UUID orderId,
                                                          BigDecimal amount,
                                                          String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        ShiftReport shift = shiftReportRepository
                .findOpenShiftByCashier(order.getCashier().getUserId())
                .orElse(null);

        String receiptNumber = documentNumberService.generateReceiptNumber(
                order.getStore().getStoreId(), ReceiptType.PAYMENT_RECEIVED
        );

        Receipt receipt = Receipt.builder()
                .receiptNumber(receiptNumber)
                .receiptType(ReceiptType.PAYMENT_RECEIVED)
                .status(ReceiptStatus.ACTIVE)
                .order(order)
                .shiftReport(shift)
                .cashier(order.getCashier())
                .store(order.getStore())
                .receiptDate(LocalDateTime.now())
                .totalAmount(amount)
                .amountPaid(amount)
                .changeAmount(BigDecimal.ZERO)
                .notes(notes != null ? notes
                        : "Paiement reçu pour commande " + order.getOrderNumber())
                .printCount(0)
                .isActive(true)
                .build();

        Receipt saved = receiptRepository.save(receipt);
        saveReceiptPdfQuietly(saved);  // ✅ une seule version

        log.info("Reçu paiement généré: {} - {} FCFA", receiptNumber, amount);
        return receiptMapper.toResponse(saved);
    }



    @Override
    @Transactional
    public ReceiptResponse generateCashInReceipt(UUID shiftReportId,
                                                 Double amount,
                                                 String reason) {
        validateCashMovement(amount, reason);
        ShiftReport shift = loadShift(shiftReportId);

        String receiptNumber = documentNumberService.generateReceiptNumber(
                shift.getStore().getStoreId(), ReceiptType.CASH_IN
        );

        String notes = String.format("ENTRÉE D'ARGENT\nMontant: %.2f FCFA\nMotif: %s",
                amount, reason);

        Receipt receipt = buildShiftReceipt(shift, receiptNumber,
                ReceiptType.CASH_IN, BigDecimal.valueOf(amount),
                LocalDateTime.now(), notes);

        Receipt saved = receiptRepository.save(receipt);
        saveReceiptPdfQuietly(saved);

        log.info("Ticket entrée argent généré: {} - {} FCFA", receiptNumber, amount);
        return receiptMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReceiptResponse generateCashOutReceipt(UUID shiftReportId,
                                                  Double amount,
                                                  String reason) {
        validateCashMovement(amount, reason);
        ShiftReport shift = loadShift(shiftReportId);

        String receiptNumber = documentNumberService.generateReceiptNumber(
                shift.getStore().getStoreId(), ReceiptType.CASH_OUT
        );

        String notes = String.format("SORTIE D'ARGENT\nMontant: %.2f FCFA\nMotif: %s",
                amount, reason);

        Receipt receipt = buildShiftReceipt(shift, receiptNumber,
                ReceiptType.CASH_OUT, BigDecimal.valueOf(amount),
                LocalDateTime.now(), notes);

        Receipt saved = receiptRepository.save(receipt);
        saveReceiptPdfQuietly(saved);

        log.info("Ticket sortie argent généré: {} - {} FCFA", receiptNumber, amount);
        return receiptMapper.toResponse(saved);
    }

    // =========================================================================
    // IMPLÉMENTATION MÉTHODES ABSTRAITES (Template Method)
    // =========================================================================

    @Override
    protected void checkExistingDocument(Order order) {
        receiptRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(receipt -> {
            throw new BadRequestException(
                    "Un ticket existe déjà pour cette commande: " + receipt.getReceiptNumber()
            );
        });
    }

    @Override
    protected String generateUniqueDocumentNumber(Order order, DocumentStrategy strategy) {
        ReceiptType type = ReceiptTypeHolder.get() != null
                ? ReceiptTypeHolder.get()
                : determineReceiptType(order);
        return documentNumberService.generateReceiptNumber(
                order.getStore().getStoreId(), type
        );
    }

    @Override
    protected Receipt createDocumentEntity(Order order,
                                           DocumentStrategy strategy,
                                           String documentNumber) {
        ReceiptType type = ReceiptTypeHolder.get() != null
                ? ReceiptTypeHolder.get()
                : determineReceiptType(order);

        // Session de caisse active
        ShiftReport shift = shiftReportRepository
                .findOpenShiftByCashier(order.getCashier().getUserId())
                .orElse(null);

        // Méthode de paiement principale
        String paymentMethodStr = order.getPayments().stream()
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT)
                .map(p -> p.getMethod().name())
                .findFirst()
                .orElse(order.getPrimaryPaymentMethod() != null
                        ? order.getPrimaryPaymentMethod().getLabel()
                        : null);

        return Receipt.builder()
                .receiptNumber(documentNumber)
                .receiptType(type)
                .status(ReceiptStatus.ACTIVE)
                .order(order)
                .shiftReport(shift)
                .cashier(order.getCashier())
                .store(order.getStore())
                .receiptDate(LocalDateTime.now())
                .totalAmount(order.getTotalAmount())
                .amountPaid(order.getTotalAmount())
                .changeAmount(order.getChangeAmount())
                .paymentMethod(paymentMethodStr)
                .notes(buildReceiptNotes(order, type))
                .printCount(0)
                .isActive(true)
                .build();
    }

    @Override
    protected void updateDocumentWithPdfPath(Receipt receipt, String pdfPath) {
        String filename = pdfPath.substring(pdfPath.lastIndexOf('/') + 1);
        receipt.setPdfFilename(filename);
        receipt.setPdfPath(pdfPath);
    }

    @Override
    protected Receipt afterDocumentGeneration(Receipt receipt,
                                              Order order,
                                              DocumentStrategy strategy) {
        // Générer et stocker les données thermiques
        String thermalData = buildThermalData(receipt);
        receipt.setThermalData(thermalData);
        return receipt;
    }

    @Override
    protected Receipt saveDocument(Receipt receipt) {
        return receiptRepository.save(receipt);
    }

    @Override
    protected void publishDocumentEvent(Receipt receipt, Order order) {
        eventPublisher.publishEvent(new ReceiptGeneratedEvent(this, receipt, order));
    }

    @Override
    protected ReceiptResponse mapToResponse(Receipt receipt) {
        return receiptMapper.toResponse(receipt);
    }

    @Override
    protected Receipt loadDocument(UUID documentId) {
        return receiptRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        "Ticket non trouvé: " + documentId
                ));
    }

    @Override
    protected void checkReprintAllowed(Receipt receipt) {
        if (receipt.isVoid()) {
            throw new BadRequestException(
                    "Impossible de réimprimer un ticket annulé: " + receipt.getReceiptNumber()
            );
        }
    }

    @Override
    protected void incrementPrintCount(Receipt receipt) {
        receipt.incrementPrintCount();
    }

    @Override
    protected void updateReprintStatus(Receipt receipt) {
        if (receipt.getPrintCount() > 1) {
            receipt.setStatus(ReceiptStatus.REPRINTED);
        }
    }

    @Override
    protected void logReprint(Receipt receipt) {
        log.info("Ticket {} réimprimé (impression #{}) - Type: {}",
                receipt.getReceiptNumber(),
                receipt.getPrintCount(),
                receipt.getReceiptType());
    }

    @Override
    protected String getStorageDirectory() {
        return receiptsDirectory;
    }


    @Override
    protected UUID getDocumentId(Receipt document) {
        return document.getReceiptId();
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // =========================================================================

    /**
     * Construit le PDF du ticket selon son type (Builder Pattern)
     */
  /*  private byte[] buildReceiptPdf(Receipt receipt) throws DocumentException {
        DocumentBuilder builder;

        // Tickets caisse (shift) → builder minimaliste
        if (isShiftReceipt(receipt.getReceiptType())) {
            builder = new ShiftReceiptDocumentBuilder(receipt);
        } else {
            builder = new ReceiptDocumentBuilder(receipt.getOrder());
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
*/
    @Override
    protected byte[] generatePdfDocument(Receipt receipt, DocumentStrategy strategy)
            throws IOException {

        if (isShiftReceipt(receipt.getReceiptType())) {
            return builderFactory.createShiftReceiptBuilder(receipt)
                    .initialize()
                    .addHeader()
                    .addMainInfo()
                    .addItemsTable()
                    .addTotals()
                    .addFooter()
                    .build();
        }

        // Tickets normaux — si strategy null (régénération), utiliser le builder direct
        if (strategy == null) {
            DocumentBuildContext ctx = DocumentBuildContext.forOrder(receipt.getOrder());
            return builderFactory.createBuilder(DocumentType.TICKET, ctx)
                    .initialize()
                    .addHeader()
                    .addMainInfo()
                    .addItemsTable()
                    .addTotals()
                    .addFooter()
                    .build();
        }

        // Cas normal via Template Method
        return super.generatePdfDocument(receipt, strategy);
    }
    private Receipt buildShiftReceipt(ShiftReport shift,
                                      String receiptNumber,
                                      ReceiptType type,
                                      BigDecimal amount,
                                      LocalDateTime date,
                                      String notes) {
        return Receipt.builder()
                .receiptNumber(receiptNumber)
                .receiptType(type)
                .status(ReceiptStatus.ACTIVE)
                .order(null)
                .shiftReport(shift)
                .cashier(shift.getCashier())
                .store(shift.getStore())
                .receiptDate(date != null ? date : LocalDateTime.now())
                .totalAmount(amount != null ? amount : BigDecimal.ZERO)
                .amountPaid(BigDecimal.ZERO)
                .changeAmount(BigDecimal.ZERO)
                .notes(notes)
                .printCount(0)
                .isActive(true)
                .build();
    }

    private void saveReceiptPdfQuietly(Receipt receipt) {
        try {
            byte[] pdfBytes = generatePdfDocument(receipt, null);
            String filename = receipt.getReceiptNumber() + ".pdf";
            fileStorageService.storeFileFromBytes(pdfBytes, filename, receiptsDirectory);
            receipt.setPdfFilename(filename);
            receipt.setPdfPath(receiptsDirectory + "/" + filename);
            receiptRepository.save(receipt);
            log.debug("PDF sauvegardé pour ticket {}", receipt.getReceiptNumber());
        } catch (Exception e) {
            log.warn("Impossible de générer le PDF pour le ticket {}: {}",
                    receipt.getReceiptNumber(), e.getMessage());
        }
    }

    // -------- Méthodes utilitaires --------

    private ShiftReport loadShift(UUID shiftReportId) {
        return shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException(
                        "Session de caisse non trouvée: " + shiftReportId
                ));
    }

    private ReceiptType determineReceiptType(Order order) {
        if (order == null || order.getOrderType() == null) {
            return ReceiptType.SALE;
        }
        return switch (order.getOrderType()) {
            case POS_SALE, ONLINE -> ReceiptType.SALE;
            case RETURN           -> ReceiptType.REFUND;
            default               -> ReceiptType.SALE;
        };
    }

    private boolean isShiftReceipt(ReceiptType type) {
        return type == ReceiptType.SHIFT_OPENING
                || type == ReceiptType.SHIFT_CLOSING
                || type == ReceiptType.CASH_IN
                || type == ReceiptType.CASH_OUT;
    }

    private String buildReceiptNotes(Order order, ReceiptType type) {
        StringBuilder notes = new StringBuilder();
        notes.append("Type: ").append(formatReceiptType(type)).append("\n");

        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            notes.append(order.getNotes());
        }
        return notes.toString().trim();
    }

    private void validateCashMovement(Double amount, String reason) {
        if (amount == null || amount <= 0) {
            throw new BadRequestException("Le montant doit être supérieur à 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("La raison est obligatoire");
        }
    }

    private String formatReceiptType(ReceiptType type) {
        return switch (type) {
            case SALE             -> "Vente";
            case REFUND           -> "Remboursement";
            case CANCELLATION     -> "Annulation";
            case SHIFT_OPENING    -> "Ouverture de caisse";
            case SHIFT_CLOSING    -> "Fermeture de caisse";
            case CASH_IN          -> "Entrée d'argent";
            case CASH_OUT         -> "Sortie d'argent";
            case PAYMENT_RECEIVED -> "Paiement reçu";
            case DELIVERY_NOTE    -> "Bon de livraison";
            case VOID             -> "Ticket annulé";
        };
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00 FCFA";
        return String.format("%,.2f FCFA", amount);
    }

    private String labelValue(String label, String value) {
        return String.format("%-14s: %s\n", label, value != null ? value : "");
    }

    private String center(String text, int width) {
        if (text == null || text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    // =========================================================================
    // ThreadLocal pour passer le type de ticket au Template Method
    // (évite de modifier la signature de generateDocument)
    // =========================================================================

    private static class ReceiptTypeHolder {
        private static final ThreadLocal<ReceiptType> HOLDER = new ThreadLocal<>();
        static void set(ReceiptType type) { HOLDER.set(type); }
        static ReceiptType get()          { return HOLDER.get(); }
        static void clear()               { HOLDER.remove(); }
    }

    //new methods

    /**
     * Génère les données texte pour imprimante thermique.
     *
     * ✅ FIX: Suppression du null byte (\u0000) dans la commande ESC/POS
     * PostgreSQL UTF8 rejette tout caractère nul en base.
     * Les vraies commandes ESC/POS seront envoyées directement à l'imprimante
     * sans passer par la base de données.
     */
    private String buildThermalData(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        String sep1 = "================================\n";
        String sep2 = "--------------------------------\n";

        // En-tête
        sb.append("\n").append(sep1);
        sb.append(center(receipt.getStore().getName(), 32)).append("\n");
        sb.append(center(receipt.getStore().getAddress() != null
                ? receipt.getStore().getAddress() : "", 32)).append("\n");
        sb.append(sep1);

        // Numéro et type de ticket
        sb.append(labelValue("Type",    formatReceiptType(receipt.getReceiptType())));
        sb.append(labelValue("Ticket",  receipt.getReceiptNumber()));
        sb.append(labelValue("Date",    receipt.getReceiptDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        sb.append(labelValue("Caissier", receipt.getCashier().getUsername()));

        if (receipt.getOrder() != null && receipt.getOrder().getCustomer() != null) {
            sb.append(labelValue("Client",
                    receipt.getOrder().getCustomer().getFullName()));
        }

        // Articles (seulement pour tickets de vente)
        if (receipt.getOrder() != null
                && receipt.getOrder().getItems() != null
                && !receipt.getOrder().getItems().isEmpty()) {
            sb.append("\n").append(sep2);
            sb.append("ARTICLES:\n");
            sb.append(sep2);

            receipt.getOrder().getItems().forEach(item -> {
                String name = item.getProduct().getName();
                if (name.length() > 22) name = name.substring(0, 20) + "..";
                sb.append(name).append("\n");
                sb.append(String.format("  %d x %.2f = %.2f FCFA\n",
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getFinalPrice()));

                if (item.getDiscountAmount() != null
                        && item.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append(String.format("  Remise: -%.2f FCFA\n",
                            item.getDiscountAmount()));
                }
            });
            sb.append(sep2);
        }

        // Notes pour tickets spéciaux
        if (receipt.getNotes() != null && !receipt.getNotes().isBlank()) {
            sb.append("\n");
            sb.append(receipt.getNotes()).append("\n");
        }

        // Totaux
        sb.append("\n").append(sep2);
        if (receipt.getOrder() != null) {
            if (receipt.getOrder().getSubtotal() != null) {
                sb.append(labelValue("Sous-total",
                        formatAmount(receipt.getOrder().getSubtotal())));
            }
            if (receipt.getOrder().getTaxAmount() != null
                    && receipt.getOrder().getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(labelValue("TVA",
                        formatAmount(receipt.getOrder().getTaxAmount())));
            }
            if (receipt.getOrder().getGlobalDiscountAmount() != null
                    && receipt.getOrder().getGlobalDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(labelValue("Remise",
                        "-" + formatAmount(receipt.getOrder().getGlobalDiscountAmount())));
            }
        }

        if (receipt.getTotalAmount() != null) {
            sb.append(sep2);
            sb.append(String.format("%-16s %14s\n", "TOTAL",
                    formatAmount(receipt.getTotalAmount())));
        }

        if (receipt.getAmountPaid() != null
                && receipt.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(labelValue("Paye",   formatAmount(receipt.getAmountPaid())));
        }
        if (receipt.getChangeAmount() != null
                && receipt.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(labelValue("Monnaie", formatAmount(receipt.getChangeAmount())));
        }

        if (receipt.getPaymentMethod() != null) {
            sb.append(labelValue("Mode", receipt.getPaymentMethod()));
        }

        // Pied de page
        sb.append("\n").append(sep1);
        sb.append(center("Merci de votre visite !", 32)).append("\n");
        sb.append(sep1).append("\n");

        // ✅ FIX: PAS de commandes ESC/POS en base (null bytes interdits en PostgreSQL UTF8)
        // Les commandes ESC/POS (\x1B, \x1D, etc.) sont générées à la volée
        // par le service d'impression au moment de l'envoi vers l'imprimante.
        // On stocke uniquement le texte brut en base.

        // ✅ FIX: Nettoyage défensif - supprimer tout caractère non imprimable
        return sanitizeForDatabase(sb.toString());
    }

    /**
     * ✅ FIX: Nettoie une chaîne pour PostgreSQL UTF8.
     * Supprime les null bytes et autres caractères de contrôle dangereux.
     * Garde les retours à la ligne (\n) et tabulations (\t).
     */
    private String sanitizeForDatabase(String input) {
        if (input == null) return null;

        return input
                // Supprime les null bytes (interdit en PostgreSQL)
                .replace("\u0000", "")
                // Supprime les caractères de contrôle ESC/POS sauf \n et \t
                .replaceAll("[\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                // Supprime les caractères GS (0x1D) utilisés par ESC/POS
                .replace("\u001B", "")  // ESC
                .replace("\u001D", "")  // GS
                .replace("\u000C", ""); // Form feed
    }

    // ✅ FIX: Méthode séparée pour les vraies commandes ESC/POS
    // À appeler uniquement pour envoi direct à l'imprimante (pas en base)
    /**
     * Génère les commandes ESC/POS binaires pour imprimante thermique.
     * NE PAS stocker en base - envoyer directement au port série/USB/réseau.
     */
    public byte[] generateEscPosCommands(Receipt receipt) {
        String textContent = buildThermalData(receipt);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // Initialiser imprimante
            baos.write(new byte[]{0x1B, 0x40}); // ESC @ = reset

            // Mode centre pour l'en-tête
            baos.write(new byte[]{0x1B, 0x61, 0x01}); // ESC a 1 = center

            // Contenu texte
            baos.write(textContent.getBytes(StandardCharsets.UTF_8));

            // Saut de ligne x3
            baos.write(new byte[]{0x0A, 0x0A, 0x0A});

            // Coupure partielle (GS V A 0)
            baos.write(new byte[]{0x1D, 0x56, 0x41, 0x00});

        } catch (IOException e) {
            log.error("Erreur génération ESC/POS", e);
        }

        return baos.toByteArray();
    }



    @Override
    @Transactional
    public ReceiptResponse generateVoidReceipt(UUID receiptId, String reason) {
        Receipt originalReceipt = loadDocument(receiptId);

        // 1. Annuler le ticket original
        voidReceipt(receiptId, reason);

        // 2. Générer un ticket d'annulation
        String voidNumber = documentNumberService.generateReceiptNumber(
                originalReceipt.getStore().getStoreId(), ReceiptType.VOID
        );

        Receipt voidReceipt = Receipt.builder()
                .receiptNumber(voidNumber)
                .receiptType(ReceiptType.VOID)
                .status(ReceiptStatus.ACTIVE)
                .order(originalReceipt.getOrder())
                .shiftReport(originalReceipt.getShiftReport())
                .cashier(originalReceipt.getCashier())
                .store(originalReceipt.getStore())
                .receiptDate(LocalDateTime.now())
                .totalAmount(originalReceipt.getTotalAmount())
                .amountPaid(BigDecimal.ZERO)
                .changeAmount(BigDecimal.ZERO)
                .notes("ANNULATION de " + originalReceipt.getReceiptNumber()
                        + "\nRaison: " + reason)
                .printCount(0)
                .isActive(true)
                .build();

        Receipt saved = receiptRepository.save(voidReceipt);
        saveReceiptPdfQuietly(saved);

        log.info("Ticket VOID généré: {} pour annulation de {}",
                voidNumber, originalReceipt.getReceiptNumber());
        return receiptMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReceiptResponse generateDeliveryNoteReceipt(UUID orderId) {
        try {
            ReceiptTypeHolder.set(ReceiptType.DELIVERY_NOTE);
            return generateDocument(orderId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération bon de livraison: " + e.getMessage(), e);
        } finally {
            ReceiptTypeHolder.clear();
        }
    }
}