package ru.metallurg.io;

import ru.metallurg.db.DatabaseManager;
import ru.metallurg.model.ConverterResult;
import ru.metallurg.service.ConverterCalculator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Загрузка исходных данных плавок из CSV-файла и автоматический расчет (режим ввода №3 по п. 1 ТЗ).
 * Формат строки CSV: МаркаСтали;МассаЧугуна_т;МассаЛома_т;C_%;Si_%;Mn_%
 */
public class FileImporter {

    public static int importConverterCsv(File file) throws Exception {
        ConverterCalculator calculator = new ConverterCalculator();
        String sql = """
                INSERT INTO calculations (calc_date, equipment_id, calc_type, steel_grade, input_summary, result_summary, total_yield_tons)
                VALUES (?, 1, 'Баланс конвертерной плавки', ?, ?, ?, ?)
                """;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int importedCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
             Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#") || line.toLowerCase().startsWith("марка")) {
                    continue; // Пропускаем заголовок или комментарии
                }

                String[] parts = line.split("[;,]");
                if (parts.length < 6) continue;

                String grade = parts[0].trim();
                double hotMetal = Double.parseDouble(parts[1].trim().replace(",", "."));
                double scrap = Double.parseDouble(parts[2].trim().replace(",", "."));
                double c = Double.parseDouble(parts[3].trim().replace(",", "."));
                double si = Double.parseDouble(parts[4].trim().replace(",", "."));
                double mn = Double.parseDouble(parts[5].trim().replace(",", "."));


                ConverterResult res = calculator.calculate(hotMetal, scrap, c, si, mn, 1360.0, 1650.0);

                ps.setString(1, LocalDateTime.now().format(fmt));
                ps.setString(2, grade);
                ps.setString(3, res.toInputSummary());
                ps.setString(4, res.toResultSummary());
                ps.setDouble(5, res.steelYieldTons);
                ps.addBatch();
                importedCount++;
            }
            ps.executeBatch();
        }
        return importedCount;
    }
}
