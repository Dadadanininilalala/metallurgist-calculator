package ru.metallurg.security;

import ru.metallurg.db.DatabaseManager;
import java.sql.*;

/**
 * Сервис регистрации и аутентификации пользователей.
 */
public class AuthService {

    /**
     * Регистрирует нового пользователя. Возвращает true при успехе.
     */
    public boolean register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.length() < 4) {
            return false;
        }
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordHasher.hashPassword(password));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Ошибка уникальности логина или сбой БД
            return false;
        }
    }

    /**
     * Проверяет логин и пароль пользователя.
     */
    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    return storedHash.equals(PasswordHasher.hashPassword(password));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при входе: " + e.getMessage());
        }
        return false;
    }
}
