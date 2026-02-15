package org.odema.posnew.design.builder;

import com.itextpdf.text.*;

@Slf4j
public abstract class AbstractPdfDocumentBuilder implements DocumentBuilder {

    // Configuration
    @Value("${app.company.name:ODEMA POS}")
    protected String companyName;

    @Value("${app.company.address:123 Rue Principale}")
    protected String companyAddress;

    @Value("${app.company.phone:+237 6XX XX XX XX}")
    protected String companyPhone;

    @Value("${app.company.email:contact@odema.com}")
    protected String companyEmail;

    @Value("${app.company.tax-id:TAX-123456}")
    protected String companyTaxId;

    // Couleurs design
    protected static final BaseColor HEADER_COLOR = new BaseColor(41, 128, 185);
    protected static final BaseColor ACCENT_COLOR = new BaseColor(52, 152, 219);
    protected static final BaseColor TEXT_DARK = new BaseColor(44, 62, 80);

    // État
    protected Document document;
    protected ByteArrayOutputStream outputStream;
    protected Order order;

    protected AbstractPdfDocumentBuilder(Order order) {
        this.order = order;
    }

    @Override
    public DocumentBuilder initialize() {
        this.document = new Document(PageSize.A4);
        this.outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            log.debug("Document PDF initialisé");
        } catch (DocumentException e) {
            log.error("Erreur initialisation PDF", e);
            throw new RuntimeException("Erreur initialisation document", e);
        }

        return this;
    }

    @Override
    public byte[] build() throws DocumentException {
        try {
            document.close();
            byte[] bytes = outputStream.toByteArray();
            log.debug("Document PDF construit: {} bytes", bytes.length);
            return bytes;
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.warn("Erreur fermeture stream", e);
            }
        }
    }

    // Méthodes utilitaires communes
    protected void addLogo() {
        try {
            InputStream logoStream = getClass().getClassLoader()
                    .getResourceAsStream("static/logo.jpg");

            if (logoStream != null) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.scaleToFit(80, 80);
                document.add(logo);
            }
        } catch (Exception e) {
            log.debug("Logo non trouvé ou erreur chargement", e);
        }
    }

    protected PdfPCell createStyledCell(String text, Font font,
                                        int alignment, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(8);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    protected String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }
}
