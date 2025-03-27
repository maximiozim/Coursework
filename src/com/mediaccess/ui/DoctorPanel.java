package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class DoctorPanel extends JFrame {
    public DoctorPanel() {
        setTitle("Панель Лікаря");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        add(new JLabel("🩺 Панель лікаря"), BorderLayout.CENTER);
        setVisible(true);
    }
}
