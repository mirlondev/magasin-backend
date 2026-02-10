package org.odema.posnew.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.dto.response.InvoiceResponse;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.InvoiceStatus;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.InvoiceMapper;
import org.odema.posnew.repository.InvoiceRepository;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.repository.PaymentRepository;
import org.odema.posnew.service.FileStorageService;
import org.odema.posnew.service.InvoiceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

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
            // Format: "Crédit - Échéance: dd/MM/yyyy"
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

    // Private helper methods

    private byte[] generateInvoicePdfBytes(Invoice invoice) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Add header/footer
            writer.setPageEvent(new InvoiceHeaderFooter());

            document.open();

            // Add company header
            addCompanyHeader(document);

            // Add invoice info
            addInvoiceInfo(document, invoice);

            // Add customer info
            addCustomerInfo(document, invoice.getCustomer());

            // Add items table
            addItemsTable(document, invoice.getOrder());

            // Add totals
            addTotals(document, invoice);

            // Add notes and terms
            addNotesAndTerms(document, invoice);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating invoice PDF", e);
            throw new IOException("Error generating invoice PDF", e);
        }
    }

    private void addCompanyHeader(Document document) throws DocumentException {
        try {
            InputStream logoStream = getClass().getClassLoader().getResourceAsStream("static/logo.png");
            if (logoStream != null) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            }
        } catch (Exception e) {
            log.warn("Logo non trouvé, continuation sans logo");
        }

        Font companyFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Paragraph company = new Paragraph(companyName, companyFont);
        company.setAlignment(Element.ALIGN_LEFT);
        document.add(company);

        Font addressFont = new Font(Font.FontFamily.HELVETICA, 10);
        Paragraph address = new Paragraph(
                companyAddress + "\n" + companyPhone + "\n" + companyEmail + "\n" + "NIF: " + companyTaxId,
                addressFont);
        address.setAlignment(Element.ALIGN_LEFT);
        document.add(address);

        document.add(Chunk.NEWLINE);
    }

    private void addInvoiceInfo(Document document, Invoice invoice) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Paragraph title = new Paragraph("FACTURE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font infoFont = new Font(Font.FontFamily.HELVETICA, 10);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        table.addCell(createCell("Numéro de facture:", true, infoFont));
        table.addCell(createCell(invoice.getInvoiceNumber(), false, infoFont));

        table.addCell(createCell("Date de facturation:", true, infoFont));
        table.addCell(createCell(invoice.getInvoiceDate().format(formatter), false, infoFont));

        if (invoice.getPaymentDueDate() != null) {
            table.addCell(createCell("Date d'échéance:", true, infoFont));
            table.addCell(createCell(invoice.getPaymentDueDate().format(formatter), false, infoFont));
        }

        table.addCell(createCell("Statut:", true, infoFont));
        table.addCell(createCell(getStatusLabel(invoice.getStatus()), false, infoFont));

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addCustomerInfo(Document document, Customer customer) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Paragraph section = new Paragraph("INFORMATIONS CLIENT", sectionFont);
        section.setSpacingBefore(10f);
        document.add(section);

        Font infoFont = new Font(Font.FontFamily.HELVETICA, 10);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        table.addCell(createCell("Nom:", true, infoFont));
        table.addCell(createCell(customer.getFullName(), false, infoFont));

        table.addCell(createCell("Email:", true, infoFont));
        table.addCell(createCell(customer.getEmail(), false, infoFont));

        table.addCell(createCell("Téléphone:", true, infoFont));
        table.addCell(createCell(customer.getPhone(), false, infoFont));

        if (customer.getAddress() != null) {
            table.addCell(createCell("Adresse:", true, infoFont));
            table.addCell(createCell(customer.getAddress(), false, infoFont));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addItemsTable(Document document, Order order) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Paragraph section = new Paragraph("ARTICLES", sectionFont);
        section.setSpacingBefore(10f);
        document.add(section);

        Font tableFont = new Font(Font.FontFamily.HELVETICA, 9);
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 2, 2, 2});

        table.addCell(createCell("Description", true, tableFont));
        table.addCell(createCell("Qté", true, tableFont));
        table.addCell(createCell("Prix unitaire", true, tableFont));
        table.addCell(createCell("Remise", true, tableFont));
        table.addCell(createCell("Total", true, tableFont));

        for (OrderItem item : order.getItems()) {
            table.addCell(createCell(item.getProduct().getName(), false, tableFont));
            table.addCell(createCell(item.getQuantity().toString(), false, tableFont));
            table.addCell(createCell(formatCurrency(item.getUnitPrice()), false, tableFont));
            table.addCell(createCell(formatCurrency(item.getDiscountAmount()), false, tableFont));
            table.addCell(createCell(formatCurrency(item.getFinalPrice()), false, tableFont));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addTotals(Document document, Invoice invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font font = new Font(Font.FontFamily.HELVETICA, 10);

        table.addCell(createCell("Sous-total:", true, font));
        table.addCell(createCell(formatCurrency(invoice.getSubtotal()), false, font));

        if (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            table.addCell(createCell("Taxe:", true, font));
            table.addCell(createCell(formatCurrency(invoice.getTaxAmount()), false, font));
        }

        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            table.addCell(createCell("Remise:", true, font));
            table.addCell(createCell(formatCurrency(invoice.getDiscountAmount()), false, font));
        }

        table.addCell(createCell("Total:", true, new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD)));
        table.addCell(createCell(formatCurrency(invoice.getTotalAmount()), false,
                new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD)));

        table.addCell(createCell("Montant payé:", true, font));
        table.addCell(createCell(formatCurrency(invoice.getAmountPaid()), false, font));

        table.addCell(createCell("Solde dû:", true, new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD)));
        table.addCell(createCell(formatCurrency(invoice.getAmountDue()), false,
                new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD)));

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addNotesAndTerms(Document document, Invoice invoice) throws DocumentException {
        if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
            Font notesFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
            Paragraph notes = new Paragraph("Notes: " + invoice.getNotes(), notesFont);
            notes.setSpacingBefore(20f);
            document.add(notes);
        }

        Font termsFont = new Font(Font.FontFamily.HELVETICA, 9);
        Paragraph terms = new Paragraph(
                "Conditions de paiement: Paiement dans les 30 jours suivant la date de facturation.\n" +
                        "Merci pour votre confiance!", termsFont);
        terms.setSpacingBefore(10f);
        document.add(terms);
    }

    private PdfPCell createCell(String text, boolean isHeader, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);

        if (isHeader) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorder(Rectangle.BOX);
        }

        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }

    private String getStatusLabel(InvoiceStatus status) {
        return switch (status) {
            case DRAFT -> "Brouillon";
            case ISSUED -> "Émise";
            case PAID -> "Payée";
            case OVERDUE -> "En retard";
            case CANCELLED -> "Annulée";
            default -> status.name();
        };
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
        String year = String.valueOf(java.time.Year.now().getValue());
        String month = String.format("%02d", java.time.LocalDate.now().getMonthValue());
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

                footer.addCell(new Phrase(companyName + " © " + java.time.Year.now().getValue(),
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