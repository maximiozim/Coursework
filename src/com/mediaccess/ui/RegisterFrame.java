package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RegisterFrame extends JFrame {
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField dobField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    public RegisterFrame() {
        setTitle("MediAccess - Реєстрація");
        setSize(500, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel firstNameLabel = createFixedLabel("Ім'я:");
        firstNameField = new JTextField(20);
        JLabel lastNameLabel = createFixedLabel("Прізвище:");
        lastNameField = new JTextField(20);
        JLabel phoneLabel = createFixedLabel("Телефон:");
        phoneField = new JTextField(20);
        JLabel emailLabel = createFixedLabel("Email:");
        emailField = new JTextField(20);
        JLabel dobLabel = createFixedLabel("Дата народження (yyyy-MM-dd):");
        dobField = new JTextField(20);
        JLabel passLabel = createFixedLabel("Пароль:");
        passwordField = new JPasswordField(20);
        JLabel confirmPassLabel = createFixedLabel("Підтвердіть пароль:");
        confirmPasswordField = new JPasswordField(20);

        JButton registerButton = new JButton("Зареєструватися");
        registerButton.addActionListener(e -> registerUser());

        JButton backButton = new JButton("Назад");
        backButton.addActionListener(e -> dispose());

        int row = 0;

        addRow(gbc, firstNameLabel, firstNameField, row++);
        addRow(gbc, lastNameLabel, lastNameField, row++);
        addRow(gbc, phoneLabel, phoneField, row++);
        addRow(gbc, emailLabel, emailField, row++);
        addRow(gbc, dobLabel, dobField, row++);
        addRow(gbc, passLabel, passwordField, row++);
        addRow(gbc, confirmPassLabel, confirmPasswordField, row++);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        add(registerButton, gbc);

        gbc.gridy = ++row;
        add(backButton, gbc);

        setVisible(true);
    }

    private JLabel createFixedLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(220, 20));  // Мінімальна ширина
        return label;
    }

    private void addRow(GridBagConstraints gbc, JLabel label, JComponent field, int row) {
        gbc.gridx = 0; gbc.gridy = row;
        add(label, gbc);
        gbc.gridx = 1;
        add(field, gbc);
    }

    private void registerUser() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String dobText = dobField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Паролі не співпадають!", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String confirmationCode = String.valueOf((int) (Math.random() * 1000000));
        DatabaseManager.createConfirmationCode(email, confirmationCode);
        DatabaseManager.sendEmail(email, confirmationCode);

        String inputCode = JOptionPane.showInputDialog(this, "Введіть код, який надіслано на email:");
        if (inputCode == null || !DatabaseManager.validateConfirmationCode(email, inputCode)) {
            JOptionPane.showMessageDialog(this, "Невірний або прострочений код!", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_RegisterUser(?, ?, ?, ?, ?, ?)}")) {

            String hashedPassword = DatabaseManager.hashPassword(password);
            Date dateOfBirth = Date.valueOf(dobText);

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setDate(3, dateOfBirth);
            stmt.setString(4, phone);
            stmt.setString(5, email);
            stmt.setString(6, hashedPassword);

            stmt.execute();
            JOptionPane.showMessageDialog(this, "Акаунт створено!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (SQLException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Помилка реєстрації або некоректна дата!", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
