package ru.metallurg.ui;

import ru.metallurg.security.AuthService;
import javax.swing.*;
import java.awt.*;

/**
 * Графическое окно входа и регистрации пользователей на Swing (п. 13, 15 ТЗ).
 */
public class LoginFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final JTextField loginField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);

    public LoginFrame() {
        setTitle("Калькулятор металлурга — Авторизация");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 240);
        setLocationRelativeTo(null);
        setResizable(false);

        // По умолчанию создадим учетную запись admin / admin для быстрого входа на защите
        authService.register("admin", "admin");

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Вход в систему расчетов", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Логин:"), gbc);
        gbc.gridx = 1;
        loginField.setText("admin");
        mainPanel.add(loginField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Пароль:"), gbc);
        gbc.gridx = 1;
        passwordField.setText("admin");
        mainPanel.add(passwordField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginBtn = new JButton("Войти");
        JButton registerBtn = new JButton("Регистрация");
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        mainPanel.add(btnPanel, gbc);

        loginBtn.addActionListener(e -> handleLogin());
        registerBtn.addActionListener(e -> handleRegister());

        add(mainPanel);
    }

    private void handleLogin() {
        String user = loginField.getText();
        String pass = new String(passwordField.getPassword());

        if (authService.login(user, pass)) {
            dispose();
            new MainFrame(user).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Неверный логин или пароль!", "Ошибка доступа", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void handleRegister() {
        String user = loginField.getText();
        String pass = new String(passwordField.getPassword());

        if (authService.register(user, pass)) {
            JOptionPane.showMessageDialog(this, "Пользователь '" + user + "' зарегистрирован! Теперь нажмите «Войти».",
                    "Регистрация", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка регистрации (логин занят или пароль короче 4 символов).",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
    }
}
