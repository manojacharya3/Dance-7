package com.studioos.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.studioos.model.Branch;
import com.studioos.model.Invoice;
import com.studioos.model.Membership;
import com.studioos.model.Student;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Renders branded Dance7 invoice PDFs from stored records only. */
@Service
public class InvoicePdfService {
    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] render(Invoice invoice, Student student, Branch branch, Membership membership, String paymentReference) {
        try {
            Document document = new Document(PageSize.A4, 48, 48, 48, 48);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();
            Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font sub = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font cell = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph title = new Paragraph("DANCE7 — THE ART FACTORY", brand);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph tag = new Paragraph("Payment Receipt / Invoice", sub);
            tag.setAlignment(Element.ALIGN_CENTER);
            document.add(tag);
            document.add(new Paragraph(" "));

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            addRow(meta, cell, "Invoice Number", invoice.getInvoiceNumber());
            addRow(meta, cell, "Invoice Date", invoice.getInvoiceDate().format(DATE));
            addRow(meta, cell, "Branch", branch != null ? branch.getName() : ("Branch #" + invoice.getBranchId()));
            addRow(meta, cell, "Student", student != null ? studentName(student) : ("Student #" + invoice.getStudentId()));
            addRow(meta, cell, "Membership", membership != null ? membership.getPlanName() : ("Membership #" + invoice.getPaymentId()));
            addRow(meta, cell, "Payment Reference", paymentReference == null ? "—" : paymentReference);
            document.add(meta);
            document.add(new Paragraph(" "));

            Paragraph amountHead = new Paragraph("Amount", head);
            document.add(amountHead);
            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(100);
            addRow(totals, cell, "Package amount", "Rs. " + money(invoice.getAmount()));
            addRow(totals, cell, "Status", invoice.getStatus());
            document.add(totals);
            document.add(new Paragraph(" "));
            Paragraph foot = new Paragraph("Thank you for dancing with us. This is a system-generated receipt.", sub);
            foot.setAlignment(Element.ALIGN_CENTER);
            document.add(foot);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("Invoice PDF render failed: {}", e.getClass().getSimpleName());
            throw new IllegalStateException("Could not generate invoice PDF.");
        }
    }

    private void addRow(PdfPTable table, Font font, String label, String value) {
        PdfPCell left = new PdfPCell(new Phrase(label, font));
        left.setBorder(PdfPCell.NO_BORDER);
        PdfPCell right = new PdfPCell(new Phrase(value == null ? "—" : value, font));
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(left);
        table.addCell(right);
    }

    private String studentName(Student student) {
        String name = ((student.getFirstName() == null ? "" : student.getFirstName()) + " "
            + (student.getLastName() == null ? "" : student.getLastName())).trim();
        return name.isEmpty() ? ("Student #" + student.getId()) : name;
    }

    private String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
    }
}
