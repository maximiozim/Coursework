package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class DBOPanel extends JFrame {
    public DBOPanel() {
        setTitle("Панель DBO (Головний користувач)");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel label = new JLabel("\uD83D\uDD11 Панель DBO - повний контроль над системою");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));

        add(label, BorderLayout.CENTER);
        setVisible(true);
    }
}
