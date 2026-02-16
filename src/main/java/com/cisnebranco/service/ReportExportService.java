package com.cisnebranco.service;

import com.cisnebranco.dto.response.report.ClientSpendingReport;
import com.cisnebranco.dto.response.report.DailyRevenueReport;
import com.cisnebranco.dto.response.report.GroomerPerformanceReport;
import com.cisnebranco.dto.response.report.OsStatusDistribution;
import com.cisnebranco.dto.response.report.PaymentMethodStats;
import com.cisnebranco.dto.response.report.ServiceTypeReport;
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
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportExportService {

    private static final String BOM = "\uFEFF";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── CSV Export ──────────────────────────────────────────────────────

    public String dailyRevenueToCsv(List<DailyRevenueReport> data) {
        StringWriter sw = new StringWriter();
        sw.write(BOM);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Data,Total OS,Receita Total,Comissão Total,Saldo Total,Total Pago");
        for (DailyRevenueReport r : data) {
            pw.printf("%s,%d,%s,%s,%s,%s%n",
                    r.getReportDate().format(DATE_FMT),
                    r.getTotalOrders(),
                    fmtMoney(r.getTotalRevenue()),
                    fmtMoney(r.getTotalCommission()),
                    fmtMoney(r.getTotalBalance()),
                    fmtMoney(r.getTotalPaid()));
        }
        return sw.toString();
    }

    public String serviceTypeToCsv(List<ServiceTypeReport> data) {
        StringWriter sw = new StringWriter();
        sw.write(BOM);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("ID,Serviço,Total Serviços,Receita Total,Preço Médio");
        for (ServiceTypeReport r : data) {
            pw.printf("%d,%s,%d,%s,%s%n",
                    r.getServiceTypeId(),
                    escapeCsv(r.getServiceName()),
                    r.getTotalServices(),
                    fmtMoney(r.getTotalRevenue()),
                    fmtMoney(r.getAvgPrice()));
        }
        return sw.toString();
    }

    public String clientSpendingToCsv(List<ClientSpendingReport> data) {
        StringWriter sw = new StringWriter();
        sw.write(BOM);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("ID,Cliente,Total OS,Total Gasto,Primeira Visita,Última Visita");
        for (ClientSpendingReport r : data) {
            pw.printf("%d,%s,%d,%s,%s,%s%n",
                    r.getClientId(),
                    escapeCsv(r.getClientName()),
                    r.getTotalOrders(),
                    fmtMoney(r.getTotalSpent()),
                    r.getFirstVisit() != null ? r.getFirstVisit().format(DATETIME_FMT) : "",
                    r.getLastVisit() != null ? r.getLastVisit().format(DATETIME_FMT) : "");
        }
        return sw.toString();
    }

    public String groomerPerformanceToCsv(List<GroomerPerformanceReport> data) {
        StringWriter sw = new StringWriter();
        sw.write(BOM);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("ID,Groomer,Total OS,Receita Total,Comissão Total,Valor Médio OS");
        for (GroomerPerformanceReport r : data) {
            pw.printf("%d,%s,%d,%s,%s,%s%n",
                    r.getGroomerId(),
                    escapeCsv(r.getGroomerName()),
                    r.getTotalOrders(),
                    fmtMoney(r.getTotalRevenue()),
                    fmtMoney(r.getTotalCommission()),
                    fmtMoney(r.getAvgOrderValue()));
        }
        return sw.toString();
    }

    public String statusDistributionToCsv(List<OsStatusDistribution> data) {
        StringWriter sw = new StringWriter();
        sw.write(BOM);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Status,Quantidade,Valor Total");
        for (OsStatusDistribution r : data) {
            pw.printf("%s,%d,%s%n", escapeCsv(r.getStatus()), r.getOrderCount(), fmtMoney(r.getTotalValue()));
        }
        return sw.toString();
    }

    public String paymentMethodsToCsv(List<PaymentMethodStats> data) {
        StringWriter sw = new StringWriter();
        sw.write(BOM);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Método,Transações,Valor Total,Valor Médio");
        for (PaymentMethodStats r : data) {
            pw.printf("%s,%d,%s,%s%n",
                    escapeCsv(r.getMethod()), r.getTransactionCount(),
                    fmtMoney(r.getTotalAmount()), fmtMoney(r.getAvgTransaction()));
        }
        return sw.toString();
    }

    // ── PDF Export ──────────────────────────────────────────────────────

    public byte[] dailyRevenueToPdf(List<DailyRevenueReport> data, LocalDate startDate, LocalDate endDate) {
        String title = "Relatório de Receita Diária — " + startDate.format(DATE_FMT) + " a " + endDate.format(DATE_FMT);
        String[] headers = {"Data", "Total OS", "Receita", "Comissão", "Saldo", "Pago"};
        float[] widths = {2f, 1f, 1.5f, 1.5f, 1.5f, 1.5f};

        return buildPdf(title, headers, widths, data.size(), (table, i) -> {
            DailyRevenueReport r = data.get(i);
            addCell(table, r.getReportDate().format(DATE_FMT));
            addCell(table, String.valueOf(r.getTotalOrders()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalRevenue()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalCommission()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalBalance()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalPaid()));
        });
    }

    public byte[] serviceTypeToPdf(List<ServiceTypeReport> data, LocalDate startDate, LocalDate endDate) {
        String title = "Relatório por Tipo de Serviço — " + startDate.format(DATE_FMT) + " a " + endDate.format(DATE_FMT);
        String[] headers = {"Serviço", "Total", "Receita", "Preço Médio"};
        float[] widths = {3f, 1f, 1.5f, 1.5f};

        return buildPdf(title, headers, widths, data.size(), (table, i) -> {
            ServiceTypeReport r = data.get(i);
            addCell(table, r.getServiceName());
            addCell(table, String.valueOf(r.getTotalServices()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalRevenue()));
            addCellRight(table, "R$ " + fmtMoney(r.getAvgPrice()));
        });
    }

    public byte[] clientSpendingToPdf(List<ClientSpendingReport> data) {
        String title = "Top Clientes por Gasto";
        String[] headers = {"Cliente", "Total OS", "Total Gasto", "Primeira Visita", "Última Visita"};
        float[] widths = {3f, 1f, 1.5f, 2f, 2f};

        return buildPdf(title, headers, widths, data.size(), (table, i) -> {
            ClientSpendingReport r = data.get(i);
            addCell(table, r.getClientName());
            addCell(table, String.valueOf(r.getTotalOrders()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalSpent()));
            addCell(table, r.getFirstVisit() != null ? r.getFirstVisit().format(DATETIME_FMT) : "—");
            addCell(table, r.getLastVisit() != null ? r.getLastVisit().format(DATETIME_FMT) : "—");
        });
    }

    public byte[] groomerPerformanceToPdf(List<GroomerPerformanceReport> data, LocalDate startDate, LocalDate endDate) {
        String title = "Desempenho dos Groomers — " + startDate.format(DATE_FMT) + " a " + endDate.format(DATE_FMT);
        String[] headers = {"Groomer", "Total OS", "Receita", "Comissão", "Valor Médio"};
        float[] widths = {3f, 1f, 1.5f, 1.5f, 1.5f};

        return buildPdf(title, headers, widths, data.size(), (table, i) -> {
            GroomerPerformanceReport r = data.get(i);
            addCell(table, r.getGroomerName());
            addCell(table, String.valueOf(r.getTotalOrders()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalRevenue()));
            addCellRight(table, "R$ " + fmtMoney(r.getTotalCommission()));
            addCellRight(table, "R$ " + fmtMoney(r.getAvgOrderValue()));
        });
    }

    // ── Private helpers ────────────────────────────────────────────────

    @FunctionalInterface
    private interface RowWriter {
        void writeRow(PdfPTable table, int index);
    }

    private byte[] buildPdf(String title, String[] headers, float[] widths, int rowCount, RowWriter rowWriter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setAlignment(Element.ALIGN_CENTER);
            titleParagraph.setSpacingAfter(12);
            document.add(titleParagraph);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Paragraph dateParagraph = new Paragraph("Gerado em: " + LocalDate.now().format(DATE_FMT), dateFont);
            dateParagraph.setAlignment(Element.ALIGN_RIGHT);
            dateParagraph.setSpacingAfter(10);
            document.add(dateParagraph);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(widths);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(41, 65, 122));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            for (int i = 0; i < rowCount; i++) {
                rowWriter.writeRow(table, i);
            }

            document.add(table);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    private void addCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addCellRight(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private String fmtMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
