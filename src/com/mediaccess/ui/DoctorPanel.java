package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DoctorPanel extends JFrame {
    private int doctorId; // Поле для зберігання ID лікаря
    private static final Logger logger = Logger.getLogger(DoctorPanel.class.getName());


    // Оновлений конструктор, що приймає ID лікаря
    public DoctorPanel(int doctorId) {
        this.doctorId = doctorId;
        logger.log(Level.INFO, "Створення DoctorPanel для DoctorID: {0}", this.doctorId);

        setTitle("Панель Лікаря");
        // Можна додати ім'я лікаря в заголовок
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Головне вікно після логіну
        setLayout(new GridLayout(3, 1, 10, 10));

        JButton viewAppointmentsButton = new JButton("Переглянути мої прийоми");
        JButton logoutButton = new JButton("Вийти");

        // Обробник кнопки викликає НОВИЙ клас DoctorAppointmentPanel з ID лікаря
        viewAppointmentsButton.addActionListener(e -> {
            logger.log(Level.INFO, "Лікар {0} відкриває панель прийомів", this.doctorId);
            new DoctorAppointmentPanel(this.doctorId); // Передаємо ID лікаря
        });

        // Обробник виходу
        logoutButton.addActionListener(e -> {
            logger.log(Level.INFO, "Лікар {0} виходить з системи", this.doctorId);
            dispose(); // Закриваємо поточну панель
            SwingUtilities.invokeLater(LoginFrame::new); // Відкриваємо вікно логіну
        });

        add(new JLabel("Ласкаво просимо, Лікарю!", SwingConstants.CENTER));
        add(viewAppointmentsButton);
        add(logoutButton);

        setVisible(true);
    }
}