package com.mediaccess.ui;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        if (DatabaseManager.testConnection()) {
            SwingUtilities.invokeLater(LoginFrame::new);
        } else {
            JOptionPane.showMessageDialog(null, "Помилка підключення до бази даних!", "Помилка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
