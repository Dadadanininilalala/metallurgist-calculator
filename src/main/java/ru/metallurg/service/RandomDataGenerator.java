package ru.metallurg.service;

import ru.metallurg.db.DatabaseManager;
import ru.metallurg.model.ConverterResult;
import ru.metallurg.model.DeoxidationResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Генератор случайных плавок заданного количества (режим ввода №2 по п. 1 ТЗ).
 */
public class RandomDataGenerator {
    private final ConverterCalculator converterCalc = new ConverterCalculator();
    private final DeoxidationCalculator deoxCalc = new DeoxidationCalculator();
    private final Random random = new Random();

    /**
     * Генерирует count случайных расчетов и сохраняет их в БД.
     */
    public int generateAndSave(int count, boolean includeConverter, boolean includeAkos) throws SQLException {
        if (count <= 0 || (!includeConverter && !includeAkos)) return 0;

        String[] grades = {"Ст3сп", "09Г2С", "20К", "45", "10ХСНД", "17Г1С"};
        String sql = """
                INSERT INTO calculations (calc_date, equipment_id, calc_type, steel_grade, input_summary, result_summary, total_yield_tons)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int inserted = 0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < count; i++) {
                boolean doConverter;
                if (includeConverter && includeAkos) {
                    doConverter = random.nextBoolean();
                } else {
                    doConverter = includeConverter;
                }

                String grade = grades[random.nextInt(grades.length)];
                String dateStr = LocalDateTime.now().format(fmt);

                if (doConverter) {
                    int eqId = random.nextBoolean() ? 1 : 2;
                    double hotMetal = eqId == 1 ? 125 + random.nextInt(15) : 240 + random.nextInt(25);
                    double scrap = Math.round(hotMetal * 0.22 * 10.0) / 10.0;
                    ConverterResult res = converterCalc.calculate(hotMetal, scrap, 4.3, 0.7, 0.5, 1360, 1650);

                    ps.setString(1, dateStr);
                    ps.setInt(2, eqId);
                    ps.setString(3, "Баланс конвертерной плавки");
                    ps.setString(4, grade);
                    ps.setString(5, res.toInputSummary());
                    ps.setString(6, res.toResultSummary());
                    ps.setDouble(7, res.steelYieldTons);
                } else {
                    int eqId = random.nextBoolean() ? 3 : 4;
                    double mass = eqId == 3 ? 150 + random.nextInt(12) : 285 + random.nextInt(18);
                    DeoxidationResult res = deoxCalc.calculate(mass, 0.12, 0.65, 0.02, 0.25, 0.03);

                    ps.setString(1, dateStr);
                    ps.setInt(2, eqId);
                    ps.setString(3, "Раскисление и легирование (АКОС)");
                    ps.setString(4, grade);
                    ps.setString(5, res.toInputSummary());
                    ps.setString(6, res.toResultSummary());
                    ps.setDouble(7, res.steelMassTons);
                }
                ps.addBatch();
                inserted++;
            }
            ps.executeBatch();
        }
        return inserted;
    }
}
