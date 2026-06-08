package com.budgettracker.service;

import com.budgettracker.model.Transaction;
import com.budgettracker.repository.TransactionRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    @Autowired private TransactionRepository transactionRepository;

    private List<Transaction> getTransactions(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate);
        }
        return transactionRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(Long userId, LocalDate startDate, LocalDate endDate, String userName) throws IOException {
        List<Transaction> transactions = getTransactions(userId, startDate, endDate);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        DeviceRgb purple = new DeviceRgb(99, 102, 241);
        DeviceRgb green  = new DeviceRgb(16, 185, 129);
        DeviceRgb red    = new DeviceRgb(239, 68, 68);
        DeviceRgb gray   = new DeviceRgb(100, 116, 139);

        // Header
        document.add(new Paragraph("Budget Tracker")
                .setFontSize(24).setBold().setFontColor(purple)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Transaction Report - " + userName)
                .setFontSize(12).setFontColor(gray)
                .setTextAlignment(TextAlignment.CENTER));

        String period = startDate != null
                ? startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + " to " +
                  endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "All Time";
        document.add(new Paragraph("Period: " + period).setFontSize(10).setFontColor(gray)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setFontSize(10).setFontColor(gray).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("\n"));

        // Summary
        BigDecimal totalIncome  = transactions.stream().filter(t -> "INCOME".equals(t.getType()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = transactions.stream().filter(t -> "EXPENSE".equals(t.getType()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
        addSummaryCell(summaryTable, "Total Income",   "Rs." + totalIncome,  green);
        addSummaryCell(summaryTable, "Total Expense",  "Rs." + totalExpense, red);
        addSummaryCell(summaryTable, "Net Balance",    "Rs." + balance,      balance.compareTo(BigDecimal.ZERO) >= 0 ? green : red);
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // Transactions Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 1, 3, 2, 1})).useAllAvailableWidth();
        for (String h : new String[]{"Date", "Type", "Description", "Category", "Amount"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(purple).setTextAlignment(TextAlignment.CENTER));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        boolean alt = false;
        for (Transaction t : transactions) {
            com.itextpdf.kernel.colors.Color rowBg = alt ? new DeviceRgb(248, 250, 252) : ColorConstants.WHITE;
            DeviceRgb amtClr  = "INCOME".equals(t.getType()) ? green : red;
            String prefix     = "INCOME".equals(t.getType()) ? "+" : "-";

            addCell(table, t.getTransactionDate().format(fmt),              rowBg, ColorConstants.BLACK);
            addCell(table, t.getType(),                                     rowBg, amtClr);
            addCell(table, t.getDescription() != null ? t.getDescription() : "-", rowBg, ColorConstants.BLACK);
            addCell(table, t.getCategory() != null ? t.getCategory().getName() : "-", rowBg, gray);
            addCell(table, prefix + "Rs." + t.getAmount(),                  rowBg, amtClr);
            alt = !alt;
        }
        document.add(table);
        document.close();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(Long userId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Transaction> transactions = getTransactions(userId, startDate, endDate);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(baos))) {
            csvWriter.writeNext(new String[]{"Date", "Type", "Amount", "Description", "Category", "Wallet", "Notes"});
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Transaction t : transactions) {
                csvWriter.writeNext(new String[]{
                        t.getTransactionDate().format(fmt),
                        t.getType(),
                        t.getAmount().toString(),
                        t.getDescription()  != null ? t.getDescription()  : "",
                        t.getCategory()     != null ? t.getCategory().getName()  : "",
                        t.getWallet()       != null ? t.getWallet().getName()    : "",
                        t.getNotes()        != null ? t.getNotes()               : ""
                });
            }
        }
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Long userId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Transaction> transactions = getTransactions(userId, startDate, endDate);
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Transactions");

        // Header style
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)99, (byte)102, (byte)241}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);

        XSSFRow headerRow = sheet.createRow(0);
        String[] cols = {"Date", "Type", "Amount", "Description", "Category", "Wallet", "Notes"};
        for (int i = 0; i < cols.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        // Income / Expense styles
        XSSFCellStyle incomeStyle  = buildColorStyle(workbook, (byte)16,  (byte)185, (byte)129);
        XSSFCellStyle expenseStyle = buildColorStyle(workbook, (byte)239, (byte)68,  (byte)68);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int rowNum = 1;
        for (Transaction t : transactions) {
            XSSFRow row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(t.getTransactionDate().format(fmt));

            XSSFCell typeCell = row.createCell(1);
            typeCell.setCellValue(t.getType());
            typeCell.setCellStyle("INCOME".equals(t.getType()) ? incomeStyle : expenseStyle);

            XSSFCell amtCell = row.createCell(2);
            amtCell.setCellValue(t.getAmount().doubleValue());
            amtCell.setCellStyle("INCOME".equals(t.getType()) ? incomeStyle : expenseStyle);

            row.createCell(3).setCellValue(t.getDescription() != null ? t.getDescription() : "");
            row.createCell(4).setCellValue(t.getCategory()    != null ? t.getCategory().getName()  : "");
            row.createCell(5).setCellValue(t.getWallet()      != null ? t.getWallet().getName()    : "");
            row.createCell(6).setCellValue(t.getNotes()       != null ? t.getNotes()               : "");
        }

        // Summary sheet
        XSSFSheet summarySheet = workbook.createSheet("Summary");
        BigDecimal totalIncome  = transactions.stream().filter(t -> "INCOME".equals(t.getType()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = transactions.stream().filter(t -> "EXPENSE".equals(t.getType()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        XSSFRow r0 = summarySheet.createRow(0);
        r0.createCell(0).setCellValue("Total Income");  r0.createCell(1).setCellValue(totalIncome.doubleValue());
        XSSFRow r1 = summarySheet.createRow(1);
        r1.createCell(0).setCellValue("Total Expense"); r1.createCell(1).setCellValue(totalExpense.doubleValue());
        XSSFRow r2 = summarySheet.createRow(2);
        r2.createCell(0).setCellValue("Net Balance");   r2.createCell(1).setCellValue(totalIncome.subtract(totalExpense).doubleValue());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    // ---- iText helpers ----
    private void addSummaryCell(Table table, String label, String value, DeviceRgb color) {
        Cell cell = new Cell()
                .add(new Paragraph(label).setFontSize(10).setFontColor(new DeviceRgb(100, 116, 139)))
                .add(new Paragraph(value).setFontSize(14).setBold().setFontColor(color))
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(12);
        table.addCell(cell);
    }

    private void addCell(Table table, String text, com.itextpdf.kernel.colors.Color bg, com.itextpdf.kernel.colors.Color fg) {
        table.addCell(new Cell()
                .add(new Paragraph(text).setFontColor(fg).setFontSize(9))
                .setBackgroundColor(bg));
    }

    // ---- POI helper ----
    private XSSFCellStyle buildColorStyle(XSSFWorkbook wb, byte r, byte g, byte b) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{r, g, b}, null));
        style.setFont(font);
        return style;
    }
}
