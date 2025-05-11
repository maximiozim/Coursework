package com.mediaccess.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatientMedicalHistoryPanel extends JFrame {

    private JTable historyTable;
    private DefaultTableModel tableModel;
    private int patientId;
    private JLabel patientNameLabel; // Мітка для імені пацієнта
    private static final Logger logger = Logger.getLogger(PatientMedicalHistoryPanel.class.getName());

    public PatientMedicalHistoryPanel(int patientId) {
        if (patientId <= 0) {
            logger.log(Level.SEVERE, "Спроба створити PatientMedicalHistoryPanel з невалідним PatientID: {0}", patientId);
            JOptionPane.showMessageDialog(null, "Помилка: Неможливо відобразити історію без ID пацієнта.", "Помилка", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        this.patientId = patientId;
        logger.log(Level.INFO, "Створення PatientMedicalHistoryPanel для PatientID: {0}", this.patientId);

        setTitle("Моя Медична Історія");
        setSize(800, 500);
        setLocationRelativeTo(null); // Центруємо вікно
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Закривати тільки це вікно
        setLayout(new BorderLayout(10, 10));

        // Панель для відображення імені пацієнта
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        patientNameLabel = new JLabel("Завантаження даних пацієнта...");
        topPanel.add(new JLabel("Пацієнт:"));
        topPanel.add(patientNameLabel);
        add(topPanel, BorderLayout.NORTH);

        // Налаштування таблиці
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Заборона редагування
            }
        };
        historyTable = new JTable(tableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        add(scrollPane, BorderLayout.CENTER);

        // Панель для кнопок внизу
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = new JButton("Оновити");
        JButton closeButton = new JButton("Закрити");

        refreshButton.addActionListener(e -> {
            fetchPatientName(); // Оновити ім'я
            loadMedicalHistory(); // Оновити таблицю
        });
        closeButton.addActionListener(e -> dispose()); // Просто закрити вікно

        bottomPanel.add(refreshButton);
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Завантаження даних при відкритті
        fetchPatientName();
        loadMedicalHistory();

        setVisible(true);
    }

    // Метод для отримання та відображення імені пацієнта
    private void fetchPatientName() {
        logger.log(Level.INFO, "Отримання імені для PatientID: {0}", this.patientId);
        String sql = "SELECT FirstName, LastName FROM dbo.Patients WHERE PatientID = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, this.patientId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("FirstName") + " " + rs.getString("LastName");
                patientNameLabel.setText(fullName + " (ID: " + this.patientId + ")");
                logger.log(Level.INFO, "Ім'я пацієнта отримано: {0}", fullName);
            } else {
                logger.log(Level.WARNING, "Пацієнта з PatientID {0} не знайдено в таблиці Patients.", this.patientId);
                patientNameLabel.setText("Пацієнта не знайдено (ID: " + this.patientId + ")");
                patientNameLabel.setForeground(Color.RED); // Позначити червоним
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при отриманні імені пацієнта PatientID: " + this.patientId, e);
            patientNameLabel.setText("Помилка завантаження імені");
            patientNameLabel.setForeground(Color.RED);
        }
    }


    // Метод завантаження медичної історії для пацієнта
    private void loadMedicalHistory() {
        logger.log(Level.INFO, "Завантаження мед. історії для PatientID: {0}", this.patientId);
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        // Запит для отримання історії з іменем лікаря
        String sql = "SELECT mh.HistoryID, mh.DateAdded, mh.Diagnosis, mh.Treatment, " +
                "ISNULL(d.FirstName + ' ' + d.LastName, 'N/A') AS DoctorFullName " + // Обробка NULL DoctorID
                "FROM dbo.MedicalHistory mh " +
                "LEFT JOIN dbo.Doctors d ON mh.DoctorID = d.DoctorID " + // LEFT JOIN на випадок, якщо лікар не вказаний або видалений
                "WHERE mh.PatientID = ? " + // Фільтр по PatientID
                "ORDER BY mh.DateAdded DESC"; // Сортування від новіших до старіших

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, this.patientId);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Додаємо колонки (один раз)
            // Можна задати назви вручну для кращого вигляду
            tableModel.addColumn("ID Запису");
            tableModel.addColumn("Дата Додавання");
            tableModel.addColumn("Діагноз");
            tableModel.addColumn("Лікування");
            tableModel.addColumn("Лікар");
            // for (int i = 1; i <= columnCount; i++) {
            //     tableModel.addColumn(meta.getColumnLabel(i));
            // }

            // Додаємо рядки
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("HistoryID")); // ID Запису
                Timestamp dateAdded = rs.getTimestamp("DateAdded");
                // Форматуємо дату
                row.add(dateAdded != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(dateAdded) : "N/A");
                row.add(rs.getString("Diagnosis")); // Діагноз
                row.add(rs.getString("Treatment")); // Лікування
                row.add(rs.getString("DoctorFullName")); // Лікар
                tableModel.addRow(row);
            }
            logger.log(Level.INFO, "Успішно завантажено {0} записів історії для PatientID: {1}", new Object[]{tableModel.getRowCount(), this.patientId});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при завантаженні мед. історії для PatientID: " + this.patientId, e);
            JOptionPane.showMessageDialog(this, "Помилка бази даних при завантаженні історії:\n" + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Неочікувана помилка при завантаженні мед. історії для PatientID: " + this.patientId, ex);
            JOptionPane.showMessageDialog(this, "Сталася неочікувана помилка: " + ex.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}