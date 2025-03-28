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
        setSize(400, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel firstNameLabel = new JLabel("Ім'я:");
        firstNameField = new JTextField(20);
        JLabel lastNameLabel = new JLabel("Прізвище:");
        lastNameField = new JTextField(20);
        JLabel phoneLabel = new JLabel("Телефон:");
        phoneField = new JTextField(20);
        JLabel emailLabel = new JLabel("Email:");
        emailField = new JTextField(20);
        JLabel dobLabel = new JLabel("Дата народження (yyyy-MM-dd):");
        dobField = new JTextField(20);
        JLabel passLabel = new JLabel("Пароль:");
        passwordField = new JPasswordField(20);
        JLabel confirmPassLabel = new JLabel("Підтвердіть пароль:");
        confirmPasswordField = new JPasswordField(20);

        JButton registerButton = new JButton("Зареєструватися");
        registerButton.addActionListener(e -> registerUser());

        JButton backButton = new JButton("Назад");
        backButton.addActionListener(e -> dispose());

        gbc.gridx = 0; gbc.gridy = 0;
        add(firstNameLabel, gbc);
        gbc.gridx = 1;
        add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(lastNameLabel, gbc);
        gbc.gridx = 1;
        add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(phoneLabel, gbc);
        gbc.gridx = 1;
        add(phoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(emailLabel, gbc);
        gbc.gridx = 1;
        add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(dobLabel, gbc);
        gbc.gridx = 1;
        add(dobField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        add(passLabel, gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        add(confirmPassLabel, gbc);
        gbc.gridx = 1;
        add(confirmPasswordField, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        add(registerButton, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        add(backButton, gbc);

        setVisible(true);
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

        String confirmationCode = String.valueOf((int)(Math.random() * 1000000));
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
