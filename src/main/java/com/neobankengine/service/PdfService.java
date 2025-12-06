package com.neobankengine.service;

import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.neobankengine.dto.TransactionResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class PdfService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00");

    /**
     * Build PDF statement with logo, summary, watermark, and QR code
     *
     * @param accountId account id (for header)
     * @param txs       list of TransactionResponse (sorted newest -> oldest)
     * @param openingBalance optional opening balance (may be null)
     * @return PDF bytes
     */
    public byte[] buildStatementPdf(Long accountId, List<TransactionResponse> txs, Double openingBalance) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Document and writer
            Document document = new Document(PageSize.A4, 36, 36, 64, 36); // margins
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPdfVersion(PdfWriter.PDF_VERSION_1_7);
            document.open();

            // Fonts
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font tableHeader = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font tableCell = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // --- Header with logo and title ---
            PdfPTable headerTbl = new PdfPTable(new float[]{1f, 3f});
            headerTbl.setWidthPercentage(100);

            // logo: try load from resources
            try {
                InputStream is = getClass().getResourceAsStream("/static/logo.png");
                if (is != null) {
                    Image logo = Image.getInstance(readAllBytes(is));
                    logo.scaleToFit(80, 80);
                    PdfPCell logoCell = new PdfPCell(logo, false);
                    logoCell.setBorder(Rectangle.NO_BORDER);
                    headerTbl.addCell(logoCell);
                } else {
                    headerTbl.addCell(emptyCell());
                }
            } catch (Exception e) {
                log.warn("logo load failed: {}", e.getMessage());
                headerTbl.addCell(emptyCell());
            }

            // Title cell
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            Paragraph title = new Paragraph("Account Statement - Account " + accountId, titleFont);
            Paragraph genInfo = new Paragraph("Generated: " + java.time.Instant.now().toString(), subFont);
            titleCell.addElement(title);
            titleCell.addElement(Chunk.NEWLINE);
            titleCell.addElement(genInfo);
            headerTbl.addCell(titleCell);

            document.add(headerTbl);
            document.add(Chunk.NEWLINE);

            // --- Summary box (opening balance, total credit, total debit, closing balance) ---
            double totalCredit = txs.stream()
                    .filter(t -> "CREDIT".equalsIgnoreCase(t.getType()))
                    .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                    .sum();

            double totalDebit = txs.stream()
                    .filter(t -> "DEBIT".equalsIgnoreCase(t.getType()))
                    .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                    .sum();

            double closing = (openingBalance == null ? 0.0 : openingBalance) + totalCredit - totalDebit;


            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setSpacingBefore(6f);
            summary.setSpacingAfter(10f);
            addSummaryCell(summary, "Opening Balance", MONEY_FMT.format(openingBalance == null ? 0.0 : openingBalance), tableHeader, tableCell);
            addSummaryCell(summary, "Total Credit", MONEY_FMT.format(totalCredit), tableHeader, tableCell);
            addSummaryCell(summary, "Total Debit", MONEY_FMT.format(totalDebit), tableHeader, tableCell);
            addSummaryCell(summary, "Closing Balance", MONEY_FMT.format(closing), tableHeader, tableCell);
            document.add(summary);

            // --- Transactions table ---
            PdfPTable table = new PdfPTable(new float[]{1f, 1.2f, 1f, 1.4f, 2.6f});
            table.setWidthPercentage(100);
            // headers
            addTableHeader(table, "Txn ID", tableHeader);
            addTableHeader(table, "Type", tableHeader);
            addTableHeader(table, "Amount", tableHeader);
            addTableHeader(table, "Timestamp", tableHeader);
            addTableHeader(table, "Reference", tableHeader);

            // rows (newest first)
            for (TransactionResponse t : txs) {
                addTableCell(table, t.getTransactionId() == null ? "" : t.getTransactionId().toString(), tableCell);
                addTableCell(table, t.getType() == null ? "" : t.getType(), tableCell);
                addTableCell(table, MONEY_FMT.format(t.getAmount() == null ? 0.0 : t.getAmount()), tableCell);
                addTableCell(table, t.getTimestamp() == null ? "" : t.getTimestamp().format(TS_FMT), tableCell);
                addTableCell(table, t.getReferenceText() == null ? "" : t.getReferenceText(), tableCell);
            }

            document.add(table);

            // --- QR Code for verification (bottom-right) ---
            try {
                String verificationUrl = "https://yourbank.example.com/verify?acc=" + accountId; // change as needed
                Image qrImage = createQrImage(verificationUrl, 120, 120);
                qrImage.setAbsolutePosition(document.getPageSize().getRight() - 140, document.bottom() + 20);
                document.add(qrImage);
            } catch (Exception e) {
                log.warn("QR generation failed: {}", e.getMessage());
            }

            // --- Watermark (diagonal, scaled, centered, subtle) ---
            addWatermark(writer, "NEO BANK ENGINE - CONFIDENTIAL");

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            return new byte[0];
        }
    }

    // ---------- helper methods ----------

    private static void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell h = new PdfPCell(new Phrase(text, font));
        h.setBackgroundColor(new Color(230, 230, 230));
        h.setHorizontalAlignment(Element.ALIGN_CENTER);
        h.setPadding(6f);
        table.addCell(h);
    }

    private static void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(6f);
        table.addCell(c);
    }

    private static void addSummaryCell(PdfPTable summary, String title, String value, Font hFont, Font vFont) {
        PdfPCell p = new PdfPCell();
        p.setBorderColor(Color.GRAY);
        p.setPadding(6f);
        Paragraph t = new Paragraph(title, hFont);
        Paragraph v = new Paragraph(value, vFont);
        p.addElement(t);
        p.addElement(Chunk.NEWLINE);
        p.addElement(v);
        summary.addCell(p);
    }

    private static PdfPCell emptyCell() {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static Image createQrImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix matrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int grayValue = (matrix.get(x, y) ? 0 : 0xFFFFFF);
                img.setRGB(x, y, grayValue);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return Image.getInstance(baos.toByteArray());
    }

    /**
     * Improved watermark routine:
     * - computes a font size that fits the page (so it won't overflow)
     * - centers the text and rotates it diagonally
     * - uses subtle opacity
     */
    private static void addWatermark(PdfWriter writer, String watermarkText) {
        PdfContentByte canvas = writer.getDirectContentUnder();
        BaseFont bf = null;
        try {
            bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);

            // compute a base font size and scale down if text wider than page width
            com.lowagie.text.Rectangle pageSize = writer.getPageSize();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            float baseFontSize = 60f; // starting point
            float maxAllowedWidth = pageWidth * 0.85f; // leave margins
            float textWidth = bf.getWidthPoint(watermarkText, baseFontSize);

            if (textWidth > maxAllowedWidth) {
                baseFontSize = baseFontSize * (maxAllowedWidth / textWidth);
                if (baseFontSize < 20f) baseFontSize = 20f; // don't go too small
            }

            // subtle opacity
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.08f); // subtle
            canvas.saveState();
            canvas.setGState(gs);

            // set text
            canvas.beginText();
            canvas.setFontAndSize(bf, baseFontSize);

            // center coordinates
            float x = (pageSize.getLeft() + pageSize.getRight()) / 2f;
            float y = (pageSize.getTop() + pageSize.getBottom()) / 2f;

            // rotate 45 degrees around center
            canvas.showTextAligned(Element.ALIGN_CENTER, watermarkText, x, y, 45f);

            canvas.endText();
            canvas.restoreState();

        } catch (Exception e) {
            // swallow watermark errors so PDF still generates
            // but log if needed
            // (no logger here because this is static; calling class logs in calling method)
        }
    }
}
