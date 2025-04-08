package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class ResetPasswordFrame extends JFrame {
    public ResetPasswordFrame(String email) {
        setTitle("Скидання пароля");
        setSize(450, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel codeLabel = createFixedLabel("Код підтвердження:");
        JTextField codeField = new JTextField(20);
        JLabel newPassLabel = createFixedLabel("Новий пароль:");
        JPasswordField newPassField = new JPasswordField(20);
        JLabel confirmLabel = createFixedLabel("Підтвердити пароль:");
        JPasswordField confirmField = new JPasswordField(20);
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

        int row = 0;
        addRow(gbc, codeLabel, codeField, row++);
        addRow(gbc, newPassLabel, newPassField, row++);
        addRow(gbc, confirmLabel, confirmField, row++);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(resetBtn, gbc);

        setVisible(true);
    }

    private JLabel createFixedLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(180, 20));
        return label;
    }

    private void addRow(GridBagConstraints gbc, JLabel label, JComponent field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        add(label, gbc);
        gbc.gridx = 1;
        add(field, gbc);
    }
}
