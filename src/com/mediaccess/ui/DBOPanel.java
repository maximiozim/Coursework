package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;

public class DBOPanel extends JFrame {
    public DBOPanel() {
        setTitle("Панель DBO");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10));

        JButton doctorsButton = new JButton("Переглянути лікарів");
        JButton appointmentsButton = new JButton("Переглянути записи на прийом");
        JButton logoutButton = new JButton("Вийти");

        doctorsButton.addActionListener(e -> new DoctorRecordsPanel());
        appointmentsButton.addActionListener(e -> new AllAppointmentsPanel());
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame(); // Повернення на вікно входу
        });

        add(new JLabel("Ласкаво просимо в систему!", SwingConstants.CENTER));
        add(doctorsButton);
        add(appointmentsButton);
        add(logoutButton);

        setVisible(true);
    }
}
