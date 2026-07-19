package com.freelancesuite.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.InvoiceLineItem;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.Color;

@Component
public class PdfGeneratorUtil {

    public ByteArrayInputStream generateInvoicePdf(Invoice invoice) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Font
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("TAX INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            document.add(title);

            Paragraph invNum = new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), boldFont);
            invNum.setAlignment(Element.ALIGN_RIGHT);
            document.add(invNum);

            Paragraph datePar = new Paragraph("Date: " + invoice.getCreatedAt().toLocalDate() + " | Due Date: " + invoice.getDueDate(), subFont);
            datePar.setAlignment(Element.ALIGN_RIGHT);
            document.add(datePar);

            document.add(Chunk.NEWLINE);

            // Agency & Client Info Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            PdfPCell cellLeft = new PdfPCell();
            cellLeft.setBorder(Rectangle.NO_BORDER);
            cellLeft.addElement(new Paragraph("ISSUED BY:", boldFont));
            cellLeft.addElement(new Paragraph(invoice.getProject().getClient().getAgency().getName(), boldFont));
            if (invoice.getProject().getClient().getAgency().getGstin() != null) {
                cellLeft.addElement(new Paragraph("GSTIN: " + invoice.getProject().getClient().getAgency().getGstin(), subFont));
            }

            PdfPCell cellRight = new PdfPCell();
            cellRight.setBorder(Rectangle.NO_BORDER);
            cellRight.addElement(new Paragraph("BILLED TO:", boldFont));
            cellRight.addElement(new Paragraph(invoice.getProject().getClient().getCompanyName(), boldFont));
            cellRight.addElement(new Paragraph("Attn: " + invoice.getProject().getClient().getContactPerson(), regularFont));
            cellRight.addElement(new Paragraph("Email: " + invoice.getProject().getClient().getEmail(), subFont));
            if (invoice.getProject().getClient().getGstin() != null) {
                cellRight.addElement(new Paragraph("GSTIN: " + invoice.getProject().getClient().getGstin(), subFont));
            }

            infoTable.addCell(cellLeft);
            infoTable.addCell(cellRight);
            document.add(infoTable);

            document.add(Chunk.NEWLINE);

            // Items Table
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{4, 1, 2, 2});

            // Table Headers
            String[] headers = {"Description", "Qty", "Unit Price (Rs)", "Amount (Rs)"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, boldFont));
                headerCell.setBackgroundColor(new Color(240, 240, 240));
                headerCell.setPadding(6);
                itemsTable.addCell(headerCell);
            }

            // Table Rows
            for (InvoiceLineItem item : invoice.getLineItems()) {
                itemsTable.addCell(new Phrase(item.getDescription(), regularFont));
                itemsTable.addCell(new Phrase(String.valueOf(item.getQuantity()), regularFont));
                itemsTable.addCell(new Phrase("Rs. " + item.getUnitPrice(), regularFont));
                itemsTable.addCell(new Phrase("Rs. " + item.getAmount(), regularFont));
            }

            document.add(itemsTable);

            document.add(Chunk.NEWLINE);

            // Summary Table
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(40);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            summaryTable.addCell(new Phrase("Subtotal:", boldFont));
            summaryTable.addCell(new Phrase("Rs. " + invoice.getSubtotal(), regularFont));

            summaryTable.addCell(new Phrase("CGST (9%):", regularFont));
            summaryTable.addCell(new Phrase("Rs. " + invoice.getCgst(), regularFont));

            summaryTable.addCell(new Phrase("SGST (9%):", regularFont));
            summaryTable.addCell(new Phrase("Rs. " + invoice.getSgst(), regularFont));

            summaryTable.addCell(new Phrase("Total Amount:", boldFont));
            summaryTable.addCell(new Phrase("Rs. " + invoice.getTotalAmount(), boldFont));

            document.add(summaryTable);

            document.close();

        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
