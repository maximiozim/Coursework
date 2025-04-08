package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class ForgotPasswordFrame extends JFrame {
    public ForgotPasswordFrame() {
        setTitle("Відновлення пароля");
        setSize(450, 180);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel emailLabel = createFixedLabel("Email:");
        JTextField emailField = new JTextField(20);
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

        gbc.gridx = 0; gbc.gridy = 0;
        add(emailLabel, gbc);
        gbc.gridx = 1;
        add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        add(sendButton, gbc);

        setVisible(true);
    }

    private JLabel createFixedLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(180, 20));
        return label;
    }
}
