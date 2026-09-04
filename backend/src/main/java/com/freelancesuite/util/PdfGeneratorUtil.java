package com.freelancesuite.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.InvoiceLineItem;
import com.freelancesuite.entity.Proposal;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;

@Component
public class PdfGeneratorUtil {

    public static ByteArrayInputStream generateInvoicePdf(Invoice invoice) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Font Settings
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            // Title & Invoice Info
            Paragraph title = new Paragraph("TAX INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            document.add(title);

            Paragraph subtitle = new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), subFont);
            subtitle.setAlignment(Element.ALIGN_LEFT);
            document.add(subtitle);

            document.add(Chunk.NEWLINE);

            // Client & Agency Info Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            PdfPCell cellLeft = new PdfPCell();
            cellLeft.setBorder(Rectangle.NO_BORDER);
            cellLeft.addElement(new Paragraph("Billed To:", boldFont));
            cellLeft.addElement(new Paragraph(invoice.getProject().getClient().getCompanyName(), regularFont));
            cellLeft.addElement(new Paragraph("GSTIN: " + (invoice.getProject().getClient().getGstin() != null ? invoice.getProject().getClient().getGstin() : "N/A"), regularFont));
            cellLeft.addElement(new Paragraph("Contact: " + invoice.getProject().getClient().getContactPerson(), regularFont));

            PdfPCell cellRight = new PdfPCell();
            cellRight.setBorder(Rectangle.NO_BORDER);
            cellRight.addElement(new Paragraph("Payable To:", boldFont));
            cellRight.addElement(new Paragraph(invoice.getProject().getClient().getAgency().getName(), regularFont));
            cellRight.addElement(new Paragraph("GSTIN: " + (invoice.getProject().getClient().getAgency().getGstin() != null ? invoice.getProject().getClient().getAgency().getGstin() : "N/A"), regularFont));
            cellRight.addElement(new Paragraph("Date: " + invoice.getCreatedAt().toLocalDate().toString(), regularFont));
            cellRight.addElement(new Paragraph("Due Date: " + invoice.getDueDate().toString(), regularFont));

            infoTable.addCell(cellLeft);
            infoTable.addCell(cellRight);
            document.add(infoTable);

            document.add(Chunk.NEWLINE);

            // Line Items Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 1, 2, 2});

            // Table Headers
            String[] headers = {"Item Description", "Qty", "Unit Price (₹)", "Amount (₹)"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setBackgroundColor(new Color(240, 240, 240));
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Populate Items
            for (InvoiceLineItem item : invoice.getLineItems()) {
                table.addCell(new Phrase(item.getDescription(), regularFont));
                table.addCell(new Phrase(String.valueOf(item.getQuantity()), regularFont));
                table.addCell(new Phrase(item.getUnitPrice().toString(), regularFont));
                table.addCell(new Phrase(item.getAmount().toString(), regularFont));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Totals Summary Table
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(40);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            summaryTable.addCell(new Phrase("Subtotal:", boldFont));
            summaryTable.addCell(new Phrase("₹" + invoice.getSubtotal().toString(), regularFont));

            boolean isIgst = invoice.getIgst() != null && invoice.getIgst().compareTo(java.math.BigDecimal.ZERO) > 0;
            if (isIgst) {
                summaryTable.addCell(new Phrase("IGST (18%):", boldFont));
                summaryTable.addCell(new Phrase("₹" + invoice.getIgst().toString(), regularFont));
            } else {
                summaryTable.addCell(new Phrase("CGST (9%):", boldFont));
                summaryTable.addCell(new Phrase("₹" + invoice.getCgst().toString(), regularFont));

                summaryTable.addCell(new Phrase("SGST (9%):", boldFont));
                summaryTable.addCell(new Phrase("₹" + invoice.getSgst().toString(), regularFont));
            }

            summaryTable.addCell(new Phrase("Total Amount:", boldFont));
            summaryTable.addCell(new Phrase("₹" + invoice.getTotalAmount().toString(), boldFont));

            document.add(summaryTable);

            document.close();
        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Generates a locked snapshot of a signed proposal, including the computed
     * signature hash, as a "downloadable proof of signature" PDF. Called once at
     * sign time; the resulting bytes are stored on the Proposal so what the
     * client downloads later can never drift from what they actually signed.
     */
    public static ByteArrayInputStream generateContractPdf(Proposal proposal) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font monoFont = FontFactory.getFont(FontFactory.COURIER, 8, Color.DARK_GRAY);

            document.add(new Paragraph("SERVICES AGREEMENT PROPOSAL", titleFont));
            document.add(new Paragraph("Share token: " + proposal.getShareToken(), subFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Client: " + proposal.getClientName(), boldFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("1. Project Scope", boldFont));
            document.add(new Paragraph(nullToEmpty(proposal.getProjectScope()), regularFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("2. Deliverables", boldFont));
            document.add(new Paragraph(nullToEmpty(proposal.getDeliverables()), regularFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("3. Financial Terms", boldFont));
            document.add(new Paragraph("Estimated budget: Rs. " + proposal.getEstimatedBudget() + " + 18% GST", regularFont));
            document.add(new Paragraph(nullToEmpty(proposal.getPaymentTerms()), regularFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("4. Digital Signature", boldFont));
            document.add(new Paragraph("Signed by: " + proposal.getSignatureName(), regularFont));
            document.add(new Paragraph("Signed at: " +
                    (proposal.getSignedAt() != null ? proposal.getSignedAt().format(formatter) : "-"), regularFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Verification hash (SHA-256 of proposal content + signer + timestamp):", subFont));
            document.add(new Paragraph(nullToEmpty(proposal.getSignatureHash()), monoFont));

            document.close();
        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
