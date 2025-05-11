package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatientPanel extends JFrame {
    private int patientId; // Поле для зберігання ID пацієнта
    private static final Logger logger = Logger.getLogger(PatientPanel.class.getName());

    public PatientPanel(int patientId) {
        if (patientId <= 0) { // Додаткова перевірка
            logger.log(Level.SEVERE, "Спроба створити PatientPanel з невалідним PatientID: {0}", patientId);
            JOptionPane.showMessageDialog(null, "Помилка: Некоректний ID пацієнта для панелі.", "Критична Помилка", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        this.patientId = patientId;
        logger.log(Level.INFO, "Створення PatientPanel для PatientID: {0}", this.patientId);

        setTitle("Панель Пацієнта");
        setSize(450, 300); // Зробив трохи вище для нової кнопки
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10)); // Змінено на 4 рядки для нової кнопки

        // Створення кнопок
        JButton viewAppointmentsButton = new JButton("Переглянути мої записи на прийом");
        JButton viewHistoryButton = new JButton("Моя Медична Історія"); // Нова кнопка
        JButton logoutButton = new JButton("Вийти");

        // Обробники подій
        viewAppointmentsButton.addActionListener(e -> {
            logger.log(Level.INFO, "Пацієнт {0} відкриває панель записів", this.patientId);
            new AppointmentPanel(this.patientId);
        });

        // Обробник для НОВОЇ кнопки
        viewHistoryButton.addActionListener(e -> {
            logger.log(Level.INFO, "Пацієнт {0} відкриває панель мед. історії", this.patientId);
            // Створюємо та показуємо нове вікно з історією
            new PatientMedicalHistoryPanel(this.patientId);
        });

        logoutButton.addActionListener(e -> {
            logger.log(Level.INFO, "Пацієнт {0} виходить з системи", this.patientId);
            dispose();
            SwingUtilities.invokeLater(LoginFrame::new);
        });

        // Додавання компонентів (змінено порядок)
        add(new JLabel("Ласкаво просимо!", SwingConstants.CENTER)); // Загальне привітання
        add(viewAppointmentsButton);
        add(viewHistoryButton); // Додано нову кнопку
        add(logoutButton);

        setVisible(true);
    }
}