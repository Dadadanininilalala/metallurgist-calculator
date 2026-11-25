package ru.metallurg.db;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Класс управления подключением к БД SQLite напрямую через JDBC (без ORM).
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:metallurg.db";

    /**
     * Возвращает активное соединение с базой данных.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Инициализирует структуру БД, создает таблицы и наполняет тестовыми данными.
     */
    public static void initDatabase() {
        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT DEFAULT 'Инженер-технолог'
                );
                """;

        String createEquipmentTable = """
                CREATE TABLE IF NOT EXISTS equipment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    capacity_tons REAL NOT NULL,
                    power_or_blow TEXT NOT NULL,
                    description TEXT
                );
                """;

        String createCalculationsTable = """
                CREATE TABLE IF NOT EXISTS calculations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    calc_date TEXT NOT NULL,
                    equipment_id INTEGER NOT NULL,
                    calc_type TEXT NOT NULL,
                    steel_grade TEXT NOT NULL,
                    input_summary TEXT NOT NULL,
                    result_summary TEXT NOT NULL,
                    total_yield_tons REAL NOT NULL,
                    FOREIGN KEY (equipment_id) REFERENCES equipment(id)
                );
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createEquipmentTable);
            stmt.execute(createCalculationsTable);

            seedEquipmentIfEmpty(conn);
            seedCalculationsIfEmpty(conn);
            demonstrateCrudSql(conn);
        } catch (SQLException e) {
            System.err.println("Ошибка инициализации БД: " + e.getMessage());
        }
    }

    /**
     * Заполняет витрину оборудования (п. 4 ТЗ), если таблица пуста.
     */
    private static void seedEquipmentIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM equipment")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        String insertSql = "INSERT INTO equipment (name, type, capacity_tons, power_or_blow, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            Object[][] items = {
                    {"Кислородный конвертер ККЦ-1", "Конвертер", 160.0, "Интенсивность продувки 450 м3/мин", "Верхняя кислородная продувка, передел передельного чугуна"},
                    {"Кислородный конвертер ККЦ-2", "Конвертер", 300.0, "Интенсивность продувки 900 м3/мин", "Комбинированная продувка с донной подачей аргона"},
                    {"АКОС-1 (УПК)", "Агрегат ковш-печь", 160.0, "Трансформатор 24 МВА", "Внепечная обработка, раскисление и доводка химсостава"},
                    {"АКОС-2 (УПК)", "Агрегат ковш-печь", 300.0, "Трансформатор 40 МВА", "Комплексное легирование и вакуумирование трубных сталей"}
            };
            for (Object[] item : items) {
                ps.setString(1, (String) item[0]);
                ps.setString(2, (String) item[1]);
                ps.setDouble(3, (Double) item[2]);
                ps.setString(4, (String) item[3]);
                ps.setString(5, (String) item[4]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Генерирует 25 тестовых записей в основной таблице расчетов (п. 19 ТЗ).
     */
    private static void seedCalculationsIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM calculations")) {
            if (rs.next() && rs.getInt(1) >= 20) return;
        }

        String insertSql = """
                INSERT INTO calculations (calc_date, equipment_id, calc_type, steel_grade, input_summary, result_summary, total_yield_tons)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        String[] grades = {"Ст3сп", "09Г2С", "20К", "45", "10ХСНД"};
        Random rnd = new Random(42);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (int i = 1; i <= 25; i++) {
                boolean isConverter = (i % 2 == 1);
                int eqId = isConverter ? (i % 4 == 1 ? 1 : 2) : (i % 4 == 3 ? 3 : 4);
                String type = isConverter ? "Баланс конвертерной плавки" : "Раскисление и легирование (АКОС)";
                String grade = grades[rnd.nextInt(grades.length)];
                double mass = (eqId == 1 || eqId == 3) ? 150 + rnd.nextInt(15) : 280 + rnd.nextInt(25);
                double yield = isConverter ? Math.round(mass * 0.91 * 10.0) / 10.0 : mass;

                String input = isConverter
                        ? String.format("Чугун: %.1f т, Лом: %.1f т, Известь: %.1f т", mass * 0.82, mass * 0.18, mass * 0.06)
                        : String.format("Сталь: %.1f т, Цель Mn: 0.65%%, Si: 0.25%%", mass);

                String result = isConverter
                        ? String.format("Выход годного: %.1f т, Тепловой избыток: +18.4 МДж/т", yield)
                        : String.format("FeMn: %.0f кг, FeSi: %.0f кг, Al: %.0f кг", mass * 5.2, mass * 3.1, mass * 0.8);

                ps.setString(1, LocalDateTime.now().minusHours(i * 3L).format(fmt));
                ps.setInt(2, eqId);
                ps.setString(3, type);
                ps.setString(4, grade);
                ps.setString(5, input);
                ps.setString(6, result);
                ps.setDouble(7, yield);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Демонстрация базовых операторов SQL: SELECT, INSERT, UPDATE, DELETE (п. 19 ТЗ).
     */
    private static void demonstrateCrudSql(Connection conn) throws SQLException {
        System.out.println("=== ДЕМОНСТРАЦИЯ SQL-ОПЕРАТОРОВ (JDBC) ===");
        try (Statement st = conn.createStatement()) {
            // 1. INSERT
            String ins = "INSERT INTO calculations (calc_date, equipment_id, calc_type, steel_grade, input_summary, result_summary, total_yield_tons) " +
                    "VALUES ('2026-01-01 12:00', 1, 'Тест SQL', 'Ст3сп', 'Тест', 'Тест', 145.0)";
            st.executeUpdate(ins);
            System.out.println("[SQL INSERT] Выполнено: " + ins);

            // 2. SELECT
            String sel = "SELECT id, steel_grade, total_yield_tons FROM calculations WHERE calc_type = 'Тест SQL'";
            try (ResultSet rs = st.executeQuery(sel)) {
                if (rs.next()) {
                    System.out.printf("[SQL SELECT] Найдена запись id=%d, марка=%s, выход=%.1f т%n",
                            rs.getInt("id"), rs.getString("steel_grade"), rs.getDouble("total_yield_tons"));
                }
            }

            // 3. UPDATE
            String upd = "UPDATE calculations SET total_yield_tons = 148.5 WHERE calc_type = 'Тест SQL'";
            int updated = st.executeUpdate(upd);
            System.out.println("[SQL UPDATE] Обновлено строк: " + updated);

            // 4. DELETE
            String del = "DELETE FROM calculations WHERE calc_type = 'Тест SQL'";
            int deleted = st.executeUpdate(del);
            System.out.println("[SQL DELETE] Удалено тестовых строк: " + deleted);
        }
        System.out.println("==========================================");
    }
}
