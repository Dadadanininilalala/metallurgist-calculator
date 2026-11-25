package ru.metallurg.io;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.JFreeChart;

import javax.swing.table.TableModel;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Экспорт результатов расчетов и графиков в форматы CSV, XLSX и PDF (п. 1, 2 ТЗ).
 */
public class FileExporter {

    /**
     * Экспорт данных из таблицы JTable в CSV (разделитель ';', кодировка UTF-8 с BOM для Excel).
     */
    public static void exportToCsv(TableModel model, File file) throws IOException {
        try (OutputStream os = new FileOutputStream(file);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            // Записываем BOM, чтобы Windows Excel корректно открыл кириллицу
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);

            // Заголовки
            for (int col = 0; col < model.getColumnCount(); col++) {
                writer.print(escapeCsv(model.getColumnName(col)));
                if (col < model.getColumnCount() - 1) writer.print(";");
            }
            writer.println();

            // Строки данных
            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object val = model.getValueAt(row, col);
                    writer.print(escapeCsv(val == null ? "" : val.toString()));
                    if (col < model.getColumnCount() - 1) writer.print(";");
                }
                writer.println();
            }
        }
    }

    /**
     * Экспорт данных из таблицы JTable в книгу Excel (.xlsx) через Apache POI.
     */
    public static void exportToXlsx(TableModel model, File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Журнал плавок");

            // Шапка таблицы
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < model.getColumnCount(); col++) {
                headerRow.createCell(col).setCellValue(model.getColumnName(col));
            }

            // Строки
            for (int row = 0; row < model.getRowCount(); row++) {
                Row dataRow = sheet.createRow(row + 1);
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object val = model.getValueAt(row, col);
                    dataRow.createCell(col).setCellValue(val == null ? "" : val.toString());
                }
            }

            for (int col = 0; col < model.getColumnCount(); col++) {
                sheet.autoSizeColumn(col);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Экспорт таблицы и диаграммы в PDF-отчет (OpenPDF).
     */
    public static void exportToPdf(TableModel model, JFreeChart chart, File file) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Подключаем системный шрифт Windows Arial для корректной кириллицы
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        BaseFont baseFont = new File(fontPath).exists()
                ? BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
                : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 14, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, 9, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font cellFont = new com.lowagie.text.Font(baseFont, 8, com.lowagie.text.Font.NORMAL);

        Paragraph title = new Paragraph("Отчет по расчетам металлургических плавок", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12);
        document.add(title);

        // Вставка графика баланса, если он передан
        if (chart != null) {
            File tempPng = File.createTempFile("chart_", ".png");
            ChartBuilder.saveChartAsPng(chart, tempPng);
            Image img = Image.getInstance(tempPng.getAbsolutePath());
            img.scaleToFit(450, 260);
            img.setAlignment(Element.ALIGN_CENTER);
            document.add(img);
            document.add(new Paragraph(" "));
            tempPng.deleteOnExit();
        }

        // Таблица с данными
        PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
        pdfTable.setWidthPercentage(100);

        for (int col = 0; col < model.getColumnCount(); col++) {
            PdfPCell cell = new PdfPCell(new Phrase(model.getColumnName(col), headerFont));
            cell.setBackgroundColor(new Color(220, 230, 241));
            cell.setPadding(5);
            pdfTable.addCell(cell);
        }

        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object val = model.getValueAt(row, col);
                PdfPCell cell = new PdfPCell(new Phrase(val == null ? "" : val.toString(), cellFont));
                cell.setPadding(4);
                pdfTable.addCell(cell);
            }
        }

        document.add(pdfTable);
        document.close();
    }

    private static String escapeCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
