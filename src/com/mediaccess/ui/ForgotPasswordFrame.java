package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class ForgotPasswordFrame extends JFrame {
    public ForgotPasswordFrame() {
        setTitle("Відновлення пароля");
        setSize(400, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(3, 2, 10, 10));

        JTextField emailField = new JTextField();
        JButton sendButton = new JButton("Надіслати код");

        sendButton.addActionListener(e -> {
            String email = emailField.getText();
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Введіть email", "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String code = DatabaseManager.requestPasswordReset(email);
            if (code != null) {
                DatabaseManager.sendEmail(email, code);
                JOptionPane.showMessageDialog(this, "Код надіслано на email", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                new ResetPasswordFrame(email);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Email не знайдено або помилка", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(new JLabel("Email:"));
        add(emailField);
        add(new JLabel());
        add(sendButton);

        setVisible(true);
    }
}
