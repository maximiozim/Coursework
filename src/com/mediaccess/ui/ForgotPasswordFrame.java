package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class ForgotPasswordFrame extends JFrame {
    public ForgotPasswordFrame() {
        setTitle("Відновлення пароля");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTextField emailField = new JTextField();
        JButton sendCodeBtn = new JButton("Надіслати код");

        sendCodeBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            String code = DatabaseManager.requestPasswordReset(email);

            if (code != null) {
                DatabaseManager.sendEmail(email, code);
                JOptionPane.showMessageDialog(this, "Код надіслано на пошту!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                new ResetPasswordFrame(email); // Передаємо email
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Email не знайдено!", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.add(new JLabel("Введіть ваш email:"));
        panel.add(emailField);
        panel.add(sendCodeBtn);

        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }
}
