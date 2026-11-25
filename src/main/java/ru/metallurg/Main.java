package ru.metallurg;

import ru.metallurg.db.DatabaseManager;
import ru.metallurg.ui.LoginFrame;
import javax.swing.*;

/**
 * Точка входа в приложение «Калькулятор металлурга».
 */
public class Main {
    public static void main(String[] args) {
        // Инициализация БД SQLite и наполнение тестовыми данными
        DatabaseManager.initDatabase();

        // Запуск GUI в потоке обработки событий Swing
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new LoginFrame().setVisible(true);
        });
    }
}
