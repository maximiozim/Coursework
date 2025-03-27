package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JFrame {
    public AdminPanel() {
        setTitle("Адмін Панель");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        add(new JLabel("👑 Вітаємо в адмін-панелі!"), BorderLayout.CENTER);
        setVisible(true);
    }
}
