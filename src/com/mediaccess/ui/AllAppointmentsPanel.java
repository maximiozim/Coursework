package com.mediaccess.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

// Панель для DBO/Admin для перегляду ВСІХ записів
public class AllAppointmentsPanel extends JFrame {
    private JTable appointmentTable;
    private DefaultTableModel tableModel;
    private static final Logger logger = Logger.getLogger(AllAppointmentsPanel.class.getName());

    public AllAppointmentsPanel() {
        logger.log(Level.INFO, "Створення AllAppointmentsPanel");
        setTitle("Управління Записами на Прийом (Всі)");
        setSize(900, 600); // Зробимо трохи більше
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Закривати тільки це вікно
        setLayout(new BorderLayout(10, 10));

        // Модель таблиці, нередагована
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        appointmentTable = new JTable(tableModel);
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appointmentTable.setAutoCreateRowSorter(true); // Додати сортування

        JScrollPane scrollPane = new JScrollPane(appointmentTable);

        // Кнопки для DBO (можливо, тільки перегляд/фільтрація/видалення?)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = new JButton("Оновити Список");
        JButton deleteButton = new JButton("Видалити Вибраний Запис"); // Кнопка видалення
        // JButton filterButton = new JButton("Фільтр"); // Можливо, додати фільтри

        refreshButton.addActionListener(e -> loadAllAppointments());
        deleteButton.addActionListener(e -> deleteSelectedAppointment()); // Додаємо обробник видалення

        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        // buttonPanel.add(filterButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        loadAllAppointments(); // Завантажити всі записи при відкритті

        setVisible(true);
    }

    // Метод завантаження ВСІХ записів
    private void loadAllAppointments() {
        logger.log(Level.INFO,"Завантаження всіх записів на прийом");
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        // Запит для отримання ВСІХ записів з іменами пацієнта та лікаря
        String sql = "SELECT a.AppointmentID, " +
                "p.FirstName + ' ' + p.LastName AS PatientFullName, " +
                "d.FirstName + ' ' + d.LastName AS DoctorFullName, " +
                "a.AppointmentDate, a.Status " +
                "FROM dbo.Appointments a " +
                "LEFT JOIN dbo.Patients p ON a.PatientID = p.PatientID " + // LEFT JOIN, якщо пацієнт міг бути видалений
                "LEFT JOIN dbo.Doctors d ON a.DoctorID = d.DoctorID " + // LEFT JOIN, якщо лікар міг бути видалений
                "ORDER BY a.AppointmentDate DESC"; // Сортуємо за спаданням дати

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement(); // Звичайний Statement, бо немає параметрів
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Додаємо колонки
            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(meta.getColumnLabel(i));
            }

            // Додаємо рядки
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                tableModel.addRow(row);
            }
            logger.log(Level.INFO,"Успішно завантажено {0} всіх записів", tableModel.getRowCount());

        } catch (SQLException e) {
            logger.log(Level.SEVERE,"Помилка завантаження всіх записів", e);
            JOptionPane.showMessageDialog(this, "Помилка бази даних при завантаженні записів: " + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Метод видалення ВИБРАНОГО запису (для DBO/Admin)
    private void deleteSelectedAppointment() {
        int selectedRowView = appointmentTable.getSelectedRow(); // Індекс у видимому списку
        if (selectedRowView >= 0) {
            // Конвертуємо індекс View в індекс Model (важливо при сортуванні/фільтрації)
            int selectedRowModel = appointmentTable.convertRowIndexToModel(selectedRowView);
            // Отримуємо ID запису з першої колонки моделі
            int appointmentId = (int) tableModel.getValueAt(selectedRowModel, 0);

            // Запитуємо підтвердження
            int confirmation = JOptionPane.showConfirmDialog(
                    this,
                    "Ви впевнені, що хочете видалити запис ID: " + appointmentId + "?",
                    "Підтвердження видалення",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirmation == JOptionPane.YES_OPTION) {
                logger.log(Level.INFO,"Спроба видалення AppointmentID: {0}", appointmentId);
                String deleteSql = "DELETE FROM dbo.Appointments WHERE AppointmentID = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(deleteSql)) {

                    stmt.setInt(1, appointmentId);
                    int rowsAffected = stmt.executeUpdate();

                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(this, "Запис успішно видалено!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadAllAppointments(); // Оновити таблицю після видалення
                    } else {
                        JOptionPane.showMessageDialog(this, "Не вдалося видалити запис. Можливо, його вже було видалено.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    logger.log(Level.SEVERE,"Помилка видалення AppointmentID: " + appointmentId, e);
                    JOptionPane.showMessageDialog(this, "Помилка бази даних при видаленні запису: " + e.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            // Якщо жоден рядок не вибрано
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть запис для видалення!", "Увага", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Діалог додавання тут НЕ потрібен, якщо DBO не додає записи вручну
    // Якщо все ж потрібен, можна скопіювати openAddAppointmentDialog зі старої версії,
    // але це менш зручно, ніж додавання через панель пацієнта.

}