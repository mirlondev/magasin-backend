package org.odema.posnew.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.dto.response.InvoiceResponse;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.InvoiceStatus;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.InvoiceMapper;
import org.odema.posnew.repository.InvoiceRepository;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.service.FileStorageService;
import org.odema.posnew.service.InvoiceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final FileStorageService fileStorageService;
    private final InvoiceMapper invoiceMapper;

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

    @Value("${app.invoice.logo-path:classpath:static/logo.png}")
    private String logoPath;

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(UUID orderId) throws IOException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // Vérifier si la commande est complète
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Seules les commandes complétées peuvent être facturées");
        }

        // Vérifier si une facture existe déjà
        invoiceRepository.findByOrder_OrderId(orderId).ifPresent(invoice -> {
            throw new BadRequestException("Une facture existe déjà pour cette commande");
        });

        // Générer le numéro de facture
        String invoiceNumber = generateInvoiceNumber();

        // Créer la facture
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .customer(order.getCustomer())
                .store(order.getStore())
                .invoiceDate(LocalDateTime.now())
                .paymentDueDate(LocalDateTime.now().plusDays(30)) // 30 jours pour payer
                .status(InvoiceStatus.ISSUED)
                .paymentMethod(order.getPaymentMethod().name())
                .notes("Facture générée automatiquement")
                .isActive(true)
                .build();

        // Calculer les montants
        invoice.calculateAmounts();

        // Sauvegarder la facture
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Générer le PDF
        generateInvoicePdf(savedInvoice.getInvoiceId());

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

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Ajouter l'en-tête et le pied de page
            writer.setPageEvent(new InvoiceHeaderFooter());

            document.open();

            // Logo et informations de l'entreprise
            addCompanyHeader(document);

            // Informations de la facture
            addInvoiceInfo(document, invoice);

            // Informations du client
            addCustomerInfo(document, invoice.getCustomer());

            // Table des articles
            addItemsTable(document, invoice.getOrder());

            // Totaux
            addTotals(document, invoice);

            // Notes et conditions
            addNotesAndTerms(document, invoice);

            document.close();

            // Sauvegarder le PDF
            byte[] pdfBytes = baos.toByteArray();
            saveInvoicePdf(invoice, pdfBytes);

            return pdfBytes;
        } catch (DocumentException e) {
            log.error("Erreur lors de la génération du PDF", e);
            throw new IOException("Erreur lors de la génération du PDF", e);
        }
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
        // Implémentation simplifiée - dans une vraie application, utiliser JavaMail
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

    // Méthodes privées pour la génération de PDF

    private void addCompanyHeader(Document document) throws DocumentException {
        try {
            // Logo
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

        // Nom de l'entreprise
        Font companyFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Paragraph company = new Paragraph(companyName, companyFont);
        company.setAlignment(Element.ALIGN_LEFT);
        document.add(company);

        // Adresse
        Font addressFont = new Font(Font.FontFamily.HELVETICA, 10);
        Paragraph address = new Paragraph(companyAddress + "\n" + companyPhone + "\n" + companyEmail, addressFont);
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

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Numéro de facture
        table.addCell(createCell("Numéro de facture:", true, infoFont));
        table.addCell(createCell(invoice.getInvoiceNumber(), false, infoFont));

        // Date de facturation
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        table.addCell(createCell("Date de facturation:", true, infoFont));
        table.addCell(createCell(invoice.getInvoiceDate().format(formatter), false, infoFont));

        // Date d'échéance
        if (invoice.getPaymentDueDate() != null) {
            table.addCell(createCell("Date d'échéance:", true, infoFont));
            table.addCell(createCell(invoice.getPaymentDueDate().format(formatter), false, infoFont));
        }

        // Statut
        table.addCell(createCell("Statut:", true, infoFont));
        table.addCell(createCell(invoice.getStatus().name(), false, infoFont));

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

        // En-tête du tableau
        table.addCell(createCell("Description", true, tableFont));
        table.addCell(createCell("Qté", true, tableFont));
        table.addCell(createCell("Prix unitaire", true, tableFont));
        table.addCell(createCell("Remise", true, tableFont));
        table.addCell(createCell("Total", true, tableFont));

        // Articles
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

        if (isHeader) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }

        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0,00 FCFA";
        return String.format("%,.2f FCFA", amount);
    }

    private String generateInvoiceNumber() {
        String prefix = "INV";
        String year = String.valueOf(java.time.Year.now().getValue());
        String month = String.format("%02d", java.time.LocalDate.now().getMonthValue());
        String sequence = String.format("%04d", getNextInvoiceSequence());

        return prefix + year + month + sequence;
    }

    private Long getNextInvoiceSequence() {
        // Dans une vraie application, utiliser une séquence de base de données
        LocalDate today = LocalDate.now();
        long count = invoiceRepository.count();
        return count + 1;
    }

    private void saveInvoicePdf(Invoice invoice, byte[] pdfBytes) throws IOException {
        String filename = invoice.getInvoiceNumber() + ".pdf";

        // Stocker le fichier
        fileStorageService.storeFileFromBytes(pdfBytes, filename, invoicesDirectory);

        // Mettre à jour l'entité
        invoice.setPdfFilename(filename);
        invoice.setPdfPath(invoicesDirectory + "/" + filename);
        invoiceRepository.save(invoice);
    }

    // Classe interne pour l'en-tête et le pied de page
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

                // Numéro de page
                footer.addCell(new Phrase(String.format("Page %d", writer.getPageNumber()),
                        new Font(baseFont, 8)));

                // Copyright
                footer.addCell(new Phrase(companyName + " © " + java.time.Year.now().getValue(),
                        new Font(baseFont, 8)));

                // Numéro de page avec template
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