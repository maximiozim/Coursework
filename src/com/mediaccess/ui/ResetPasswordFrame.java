package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class ResetPasswordFrame extends JFrame {
    public ResetPasswordFrame(String email) {
        setTitle("Скидання пароля");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 2, 10, 10));

        JTextField codeField = new JTextField();
        JPasswordField newPassField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        JButton resetBtn = new JButton("Змінити пароль");

        resetBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            String newPassword = new String(newPassField.getPassword());
            String confirmPassword = new String(confirmField.getPassword());

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Паролі не співпадають!", "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = DatabaseManager.resetPassword(email, code, newPassword, confirmPassword);

            if (success) {
                JOptionPane.showMessageDialog(this, "Пароль змінено!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Невірний код або помилка!", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(new JLabel("Код підтвердження:"));
        add(codeField);
        add(new JLabel("Новий пароль:"));
        add(newPassField);
        add(new JLabel("Підтвердити пароль:"));
        add(confirmField);
        add(new JLabel()); // порожня клітинка
        add(resetBtn);

        setVisible(true);
    }
}
