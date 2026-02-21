package oldcode;


import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.odema.posnew.design.builder.impl.InvoiceDocumentBuilder;
import org.odema.posnew.design.builder.impl.ShiftReceiptDocumentBuilder;
import org.odema.posnew.application.dto.response.InvoiceResponse;
import org.odema.posnew.domain.enums_old.InvoiceStatus;
import org.odema.posnew.domain.enums_old.PaymentStatus;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.repository.InvoiceRepository;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.repository.PaymentRepository;
import org.odema.posnew.domain.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

//@Service
//@RequiredArgsConstructor
//@Slf4j
public class InvoiceServiceOldImpl  {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;
    private final FileStorageService fileStorageService;

    @Value("${app.file.directories.invoices:invoices}")
    private String invoicesDirectory;

    @Value("${app.company.name:ODEMA POS}")
    private String companyName;

    @Value("${app.company.address:123 Rue Principale, Ville}")
    private String companyAddress;

    @Value("${app.company.phone:+237 6XX XX XX XX}")
    private String companyPhone;

    @Value("${app.company.email:contact@odema.com}")
    private String companyEmail;

    @Value("${app.company.tax-id:TAX-123456}")
    private String companyTaxId;

    // Couleurs personnalisées pour design professionnel
    private static final BaseColor HEADER_COLOR = new BaseColor(41, 128, 185); // Bleu professionnel
    private static final BaseColor ACCENT_COLOR = new BaseColor(52, 152, 219);
    private static final BaseColor TEXT_DARK = new BaseColor(44, 62, 80);

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(UUID orderId) throws IOException {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // Vérifier si une facture existe déjà
        invoiceRepository.findByOrder_OrderId(orderId).ifPresent(invoice -> {
            throw new BadRequestException("Une facture existe déjà pour cette commande");
        });

        // Pour les factures de crédit, un client est obligatoire
        if (order.getCustomer() == null) {
            throw new BadRequestException("Un client est requis pour générer une facture");
        }

        // Générer le numéro de facture
        String invoiceNumber = generateInvoiceNumber();

        // Récupérer les paiements
        List<Payment> payments = order.getPayments();

        // Calculer les montants basés sur les paiements
        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditAmount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Déterminer le statut
        InvoiceStatus status;
        if (totalPaid.compareTo(order.getTotalAmount()) >= 0) {
            status = InvoiceStatus.PAID;
        } else if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            status = InvoiceStatus.ISSUED;
        } else {
            status = InvoiceStatus.DRAFT;
        }

        // Extraire les notes de paiement crédit pour la date d'échéance
        String creditNotes = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .map(Payment::getNotes)
                .filter(notes -> notes != null && notes.contains("Échéance:"))
                .findFirst()
                .orElse(null);

        LocalDateTime dueDate = LocalDateTime.now().plusDays(30); // Par défaut 30 jours
        if (creditNotes != null && creditNotes.contains("Échéance:")) {
            // Extraire la date d'échéance des notes si disponible
            try {
                String dateStr = creditNotes.substring(creditNotes.indexOf("Échéance:") + 10).split("\\n")[0].trim();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                dueDate = LocalDate.parse(dateStr, formatter).atStartOfDay();
            } catch (Exception e) {
                log.warn("Could not parse due date from credit notes: {}", creditNotes);
            }
        }

        // Créer la facture
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .customer(order.getCustomer())
                .store(order.getStore())
                .invoiceDate(LocalDateTime.now())
                .paymentDueDate(dueDate)
                .status(status)
                .paymentMethod(order.getPaymentMethod().name())
                .notes(buildInvoiceNotes(order, creditNotes))
                .isActive(true)
                .build();

        // Définir les montants
        invoice.setSubtotal(order.getSubtotal());
        invoice.setTaxAmount(order.getTaxAmount());
        invoice.setDiscountAmount(order.getDiscountAmount());
        invoice.setTotalAmount(order.getTotalAmount());
        invoice.setAmountPaid(totalPaid);
        invoice.setAmountDue(order.getTotalAmount().subtract(totalPaid).subtract(creditAmount));

        // Sauvegarder la facture
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Générer le PDF
        byte[] pdfBytes = generateInvoicePdfBytes(savedInvoice);
        saveInvoicePdf(savedInvoice, pdfBytes);

        return invoiceMapper.toResponse(savedInvoice);
    }

    @Override
    public InvoiceResponse getInvoiceById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByOrder(UUID orderId) {
        Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Aucune facture trouvée pour cette commande"));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public List<InvoiceResponse> getInvoicesByCustomer(UUID customerId) {
        return invoiceRepository.findByCustomer_CustomerId(customerId).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public List<InvoiceResponse> getInvoicesByStore(UUID storeId) {
        return invoiceRepository.findByStore_StoreId(storeId.toString()).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
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
    public List<InvoiceResponse> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        return invoiceRepository.findByDateRange(startDateTime, endDateTime).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public byte[] generateInvoicePdf(UUID invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        return generateInvoicePdfBytes(invoice);
    }

    @Override
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
    public InvoiceResponse updateInvoiceStatus(UUID invoiceId, String status) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        try {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            invoice.setStatus(invoiceStatus);
            invoice.setUpdatedAt(LocalDateTime.now());

            Invoice updatedInvoice = invoiceRepository.save(invoice);
            return invoiceMapper.toResponse(updatedInvoice);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de facture invalide: " + status);
        }
    }

    @Override
    @Transactional
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
        return invoiceMapper.toResponse(updatedInvoice);
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture non trouvée"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Impossible d'annuler une facture payée");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(updatedInvoice);
    }

    @Override
    public void sendInvoiceByEmail(UUID invoiceId, String email) throws Exception {
        log.info("Envoi de la facture {} à l'adresse {}", invoiceId, email);
        // TODO: Implémenter l'envoi d'email avec pièce jointe
    }

    @Override
    public List<InvoiceResponse> getOverdueInvoices() {
        LocalDateTime now = LocalDateTime.now();
        return invoiceRepository.findOverdueInvoices(now).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public Double getTotalOutstandingAmount() {
        return invoiceRepository.getTotalOutstandingAmount();
    }

    // ============================================================================
    // MÉTHODES AMÉLIORÉES POUR FACTURES PROFESSIONNELLES
    // ============================================================================

    private byte[] generateInvoicePdfBytes(Invoice invoice) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Add header/footer
            writer.setPageEvent(new InvoiceHeaderFooter());

            document.open();

            // NOUVEAU: En-tête amélioré avec design moderne
            addEnhancedCompanyHeader(document);

            // NOUVEAU: Informations facture avec design amélioré
            addEnhancedInvoiceInfo(document, invoice);

            // NOUVEAU: Tableau articles avec design moderne
            addEnhancedItemsTable(document, invoice.getOrder());

            // NOUVEAU: Totaux avec design professionnel
            addEnhancedTotals(document, invoice);

            // NOUVEAU: Pied de page professionnel
            addEnhancedFooter(document, invoice);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating invoice PDF", e);
            throw new IOException("Error generating invoice PDF", e);
        }
    }

    /**
     * NOUVEAU: En-tête amélioré avec design moderne
     */
    private void addEnhancedCompanyHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 1});

        // Colonne gauche : Logo + Nom société
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);

        try {
            // Logo
            InputStream logoStream = getClass().getClassLoader()
                    .getResourceAsStream("static/logo.jpg");
            if (logoStream != null) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.scaleToFit(80, 80);
                leftCell.addElement(logo);
            }
        } catch (Exception e) {
            log.debug("Logo not found, continuing without logo");
        }

        // Nom société
        InvoiceDocumentBuilder.setCompagnyFont(leftCell, HEADER_COLOR, companyName, TEXT_DARK);

        headerTable.addCell(leftCell);

        // Colonne droite : Coordonnées
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font contactFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);

        Paragraph contacts = new Paragraph();
        contacts.setAlignment(Element.ALIGN_RIGHT);
        contacts.add(new Chunk(companyAddress + "\n", contactFont));
        contacts.add(new Chunk("Tél: " + companyPhone + "\n", contactFont));
        contacts.add(new Chunk("Email: " + companyEmail + "\n", contactFont));
        contacts.add(new Chunk("NIF: " + companyTaxId, contactFont));

        rightCell.addElement(contacts);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Ligne de séparation colorée
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(HEADER_COLOR);
        separator.setLineWidth(2);
        document.add(new Chunk(separator));
        document.add(Chunk.NEWLINE);
    }

    /**
     * NOUVEAU: Section informations facture avec design amélioré
     */
    private void addEnhancedInvoiceInfo(Document document, Invoice invoice)
            throws DocumentException {

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});
        infoTable.setSpacingBefore(10f);
        infoTable.setSpacingAfter(15f);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_DARK);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

        // Colonne gauche : Client
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.BOX);
        clientCell.setBorderColor(BaseColor.LIGHT_GRAY);
        clientCell.setPadding(10);
        clientCell.setBackgroundColor(new BaseColor(236, 240, 241));

        Paragraph clientTitle = new Paragraph("FACTURER À", labelFont);
        clientCell.addElement(clientTitle);
        clientCell.addElement(Chunk.NEWLINE);

        Customer customer = invoice.getCustomer();
        Paragraph clientInfo = new Paragraph();
        clientInfo.add(new Chunk(customer.getFullName() + "\n",
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        if (customer.getAddress() != null) {
            clientInfo.add(new Chunk(customer.getAddress() + "\n", valueFont));
        }
        clientInfo.add(new Chunk("Tél: " + customer.getPhone() + "\n", valueFont));
        clientInfo.add(new Chunk(customer.getEmail(), valueFont));

        clientCell.addElement(clientInfo);
        infoTable.addCell(clientCell);

        // Colonne droite : Détails facture
        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBorder(Rectangle.BOX);
        invoiceCell.setBorderColor(HEADER_COLOR);
        invoiceCell.setBorderWidth(2);
        invoiceCell.setPadding(10);

        // Titre FACTURE
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, HEADER_COLOR);
        Paragraph title = new Paragraph("FACTURE", titleFont);
        invoiceCell.addElement(title);
        invoiceCell.addElement(Chunk.NEWLINE);

        // Détails
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);

        addDetailRow(detailsTable, "N° Facture:", invoice.getInvoiceNumber(), labelFont, valueFont);
        addDetailRow(detailsTable, "Date:", invoice.getInvoiceDate().format(formatter), labelFont, valueFont);
        if (invoice.getPaymentDueDate() != null) {
            addDetailRow(detailsTable, "Échéance:",
                    invoice.getPaymentDueDate().format(formatter), labelFont, valueFont);
        }

        // Badge statut
        PdfPCell statusCell = new PdfPCell();
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setColspan(2);
        Paragraph status = new Paragraph(getStatusBadge(invoice.getStatus()),
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE));
        status.setAlignment(Element.ALIGN_CENTER);
        statusCell.setBackgroundColor(getStatusColor(invoice.getStatus()));
        statusCell.setPadding(5);
        statusCell.addElement(status);
        detailsTable.addCell(statusCell);

        invoiceCell.addElement(detailsTable);
        infoTable.addCell(invoiceCell);

        document.add(infoTable);
    }

    /**
     * NOUVEAU: Tableau articles avec design moderne
     */
    private void addEnhancedItemsTable(Document document, Order order)
            throws DocumentException {

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1.5f, 2, 1.5f, 2});
        table.setSpacingBefore(10f);

        // En-têtes colorés
        String[] headers = {"DESCRIPTION", "QTÉ", "PRIX UNIT.", "REMISE", "TOTAL"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(HEADER_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        // Lignes articles avec alternance de couleurs
        boolean alternate = false;
        for (OrderItem item : order.getItems()) {
            BaseColor rowColor = alternate ?
                    new BaseColor(236, 240, 241) : BaseColor.WHITE;

            table.addCell(createStyledCell(item.getProduct().getName(),
                    cellFont, Element.ALIGN_LEFT, rowColor));
            table.addCell(createStyledCell(item.getQuantity().toString(),
                    cellFont, Element.ALIGN_CENTER, rowColor));
            table.addCell(createStyledCell(formatCurrency(item.getUnitPrice()),
                    cellFont, Element.ALIGN_RIGHT, rowColor));
            table.addCell(createStyledCell(formatCurrency(item.getDiscountAmount()),
                    cellFont, Element.ALIGN_RIGHT, rowColor));
            table.addCell(createStyledCell(formatCurrency(item.getFinalPrice()),
                    cellFont, Element.ALIGN_RIGHT, rowColor));

            alternate = !alternate;
        }

        document.add(table);
    }

    /**
     * NOUVEAU: Section totaux avec design professionnel
     */
    private void addEnhancedTotals(Document document, Invoice invoice)
            throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(15f);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, HEADER_COLOR);

        // Lignes de totaux
        addTotalRow(table, "Sous-total:", formatCurrency(invoice.getSubtotal()),
                labelFont, valueFont);

        if (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(table, "TVA:", formatCurrency(invoice.getTaxAmount()),
                    labelFont, valueFont);
        }

        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(table, "Remise:", formatCurrency(invoice.getDiscountAmount()),
                    labelFont, valueFont);
        }

        // Ligne totale avec fond coloré
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL", totalFont));
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setBackgroundColor(new BaseColor(236, 240, 241));
        totalLabelCell.setPadding(8);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(
                new Phrase(formatCurrency(invoice.getTotalAmount()), totalFont));
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setBackgroundColor(new BaseColor(236, 240, 241));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(8);
        table.addCell(totalValueCell);

        // Montant payé et dû
        if (invoice.getAmountPaid() != null && invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(table, "Montant payé:", formatCurrency(invoice.getAmountPaid()),
                    labelFont, valueFont);
        }

        if (invoice.getAmountDue() != null && invoice.getAmountDue().compareTo(BigDecimal.ZERO) > 0) {
            Font dueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,
                    new BaseColor(231, 76, 60)); // Rouge
            addTotalRow(table, "Solde dû:", formatCurrency(invoice.getAmountDue()),
                    labelFont, dueFont);
        }

        document.add(table);
    }

    /**
     * NOUVEAU: Pied de page professionnel
     */
    private void addEnhancedFooter(Document document, Invoice invoice)
            throws DocumentException {

        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        // Notes
        if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
            Font notesFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC);
            Paragraph notes = new Paragraph();
            notes.add(new Chunk("Notes: ",
                    new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD)));
            notes.add(new Chunk(invoice.getNotes(), notesFont));
            document.add(notes);
            document.add(Chunk.NEWLINE);
        }

        // Ligne de séparation
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(BaseColor.LIGHT_GRAY);
        document.add(new Chunk(separator));
        document.add(Chunk.NEWLINE);

        // Conditions de paiement
        Font termsFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_DARK);
        Paragraph terms = new Paragraph();
        terms.add(new Chunk("CONDITIONS DE PAIEMENT\n",
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD)));
        terms.add(new Chunk(
                "• Paiement à réception de facture ou selon échéance indiquée\n" +
                        "• Retard de paiement : pénalités de 10% après échéance\n" +
                        "• Tout litige relève du tribunal de commerce de Pointe-Noire\n",
                termsFont));
        document.add(terms);

        document.add(Chunk.NEWLINE);

        // Message de remerciement
        Font thanksFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, HEADER_COLOR);
        Paragraph thanks = new Paragraph("Merci pour votre confiance !", thanksFont);
        thanks.setAlignment(Element.ALIGN_CENTER);
        document.add(thanks);

        // Informations légales au bas
        Font legalFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, BaseColor.GRAY);
        Paragraph legal = new Paragraph(
                companyName + " - NIF: " + companyTaxId + " - RC: XXXXX\n" +
                        "Siège social: " + companyAddress,
                legalFont);
        legal.setAlignment(Element.ALIGN_CENTER);
        legal.setSpacingBefore(20f);
        document.add(legal);
    }

    // ============================================================================
    // MÉTHODES UTILITAIRES POUR LE DESIGN AMÉLIORÉ
    // ============================================================================

    private void addDetailRow(PdfPTable table, String label, String value,
                              Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont) {
        ShiftReceiptDocumentBuilder.getPdfCell(table, label, value, labelFont, valueFont);
    }

    private PdfPCell createStyledCell(String text, Font font, int alignment,
                                      BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(8);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private String getStatusBadge(InvoiceStatus status) {
        return switch (status) {
            case SENT -> "Envoyée";
            case PAID -> "✓ PAYÉE";
            case ISSUED -> "◉ ÉMISE";
            case OVERDUE -> "⚠ EN RETARD";
            case DRAFT -> "◐ BROUILLON";
            case CANCELLED -> "✗ ANNULÉE";
            case REFUNDED -> "REMBOURSER";
        };
    }

    private BaseColor getStatusColor(InvoiceStatus status) {
        return switch (status) {
            case SENT -> new BaseColor(39, 174, 96);
            case PAID -> new BaseColor(39, 174, 96);      // Vert
            case ISSUED -> new BaseColor(52, 152, 219);   // Bleu
            case OVERDUE -> new BaseColor(231, 76, 60);   // Rouge
            case DRAFT -> new BaseColor(149, 165, 166);   // Gris
            case CANCELLED -> new BaseColor(127, 140, 141); // Gris foncé
            case REFUNDED -> new BaseColor(127, 140, 122);
        };
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }

    private String buildInvoiceNotes(Order order, String creditNotes) {
        StringBuilder notes = new StringBuilder();

        if (creditNotes != null) {
            notes.append("Vente à crédit\n");
            notes.append(creditNotes).append("\n");
        }

        if (order.getNotes() != null) {
            notes.append(order.getNotes());
        }

        return notes.toString().trim();
    }

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

    private class InvoiceHeaderFooter extends PdfPageEventHelper {
        private PdfTemplate total;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(30, 16);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(3);
            try {
                footer.setWidths(new int[]{24, 24, 2});
                footer.setTotalWidth(527);
                footer.setLockedWidth(true);
                footer.getDefaultCell().setFixedHeight(20);
                footer.getDefaultCell().setBorder(Rectangle.TOP);

                footer.addCell(new Phrase(String.format("Page %d", writer.getPageNumber()),
                        new Font(baseFont, 8)));

                footer.addCell(new Phrase(companyName + " © " + Year.now().getValue(),
                        new Font(baseFont, 8)));

                PdfPCell cell = new PdfPCell(Image.getInstance(total));
                cell.setBorder(Rectangle.TOP);
                footer.addCell(cell);

                footer.writeSelectedRows(0, -1, 34, 50, writer.getDirectContent());
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber()),
                            new Font(baseFont, 8)), 2, 2, 0);
        }
    }
}