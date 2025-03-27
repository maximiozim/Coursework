package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class PatientPanel extends JFrame {
    public PatientPanel() {
        setTitle("Панель Пацієнта");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        add(new JLabel("👤 Панель пацієнта"), BorderLayout.CENTER);
        setVisible(true);
    }
}
