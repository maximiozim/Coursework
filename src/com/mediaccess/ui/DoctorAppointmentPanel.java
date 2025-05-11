package com.mediaccess.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter; // Для сортування
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List; // Для зберігання PatientID
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DoctorAppointmentPanel extends JFrame {
    private JTable appointmentTable;
    private DefaultTableModel tableModel;
    private int doctorId;
    private static final Logger logger = Logger.getLogger(DoctorAppointmentPanel.class.getName());
    private final String[] DOCTOR_SETTABLE_STATUSES = {"Підтверджено", "Завершено", "Не з'явився", "Скасовано"};

    // Список для зберігання PatientID паралельно до рядків таблиці
    // Це простіший спосіб, ніж додавати приховану колонку, особливо з сортуванням
    private List<Integer> patientIdsForRow = new ArrayList<>();

    public DoctorAppointmentPanel(int doctorId) {
        // ... (перевірка doctorId і початкові налаштування як раніше) ...
        if (doctorId <= 0) {
            logger.log(Level.SEVERE, "Спроба створити DoctorAppointmentPanel з невалідним DoctorID: {0}", doctorId);
            JOptionPane.showMessageDialog(null, "Помилка: Некоректний ID лікаря.", "Критична Помилка", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        this.doctorId = doctorId;
        logger.log(Level.INFO, "Створення DoctorAppointmentPanel для DoctorID: {0}", this.doctorId);

        setTitle("Мої Заплановані Прийоми");
        setSize(900, 600); // Збільшимо трохи
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        appointmentTable = new JTable(tableModel);
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Сортувальник для таблиці
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
        appointmentTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(appointmentTable);

        // --- Кнопки ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = new JButton("Оновити Список");
        JButton changeStatusButton = new JButton("Змінити Статус Вибраного");
        JButton deleteButton = new JButton("Видалити Вибраний Запис");
        JButton viewPatientHistoryButton = new JButton("Історія Пацієнта"); // НОВА КНОПКА

        // --- Обробники подій ---
        refreshButton.addActionListener(e -> loadDoctorAppointments());
        changeStatusButton.addActionListener(e -> changeSelectedAppointmentStatus());
        deleteButton.addActionListener(e -> deleteSelectedAppointment());

        // Обробник для НОВОЇ кнопки
        viewPatientHistoryButton.addActionListener(e -> viewSelectedPatientHistory());

        // Додавання кнопок
        buttonPanel.add(refreshButton);
        buttonPanel.add(changeStatusButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewPatientHistoryButton); // Додано нову кнопку

        // Додавання компонентів
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        loadDoctorAppointments();
        setVisible(true);
    }

    // --- ОНОВЛЕНИЙ МЕТОД ЗАВАНТАЖЕННЯ ---
    private void loadDoctorAppointments() {
        logger.log(Level.INFO,"Завантаження прийомів для DoctorID: {0}", this.doctorId);
        tableModel.setRowCount(0); // Очистка рядків
        tableModel.setColumnCount(0); // Очистка колонок
        patientIdsForRow.clear(); // Очистка списку ID пацієнтів

        // !!! ЗАПИТ ТЕПЕР ВКЛЮЧАЄ a.PatientID !!!
        String sql = "SELECT a.AppointmentID, a.PatientID, p.FirstName + ' ' + p.LastName AS PatientFullName, " +
                "a.AppointmentDate, a.Status " +
                "FROM dbo.Appointments a " +
                "INNER JOIN dbo.Patients p ON a.PatientID = p.PatientID " +
                "WHERE a.DoctorID = ? " +
                "ORDER BY a.AppointmentDate";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, this.doctorId);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();

            // Додаємо видимі колонки до моделі
            // Ми НЕ додаємо PatientID як видиму колонку
            tableModel.addColumn("ID Запису");        // Index 0 (AppointmentID)
            tableModel.addColumn("Пацієнт");          // Index 1 (PatientFullName)
            tableModel.addColumn("Дата Прийому");      // Index 2 (AppointmentDate)
            tableModel.addColumn("Статус");            // Index 3 (Status)

            // Додаємо рядки
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                int currentPatientId = rs.getInt("PatientID"); // Отримуємо PatientID
                patientIdsForRow.add(currentPatientId); // Зберігаємо PatientID паралельно

                row.add(rs.getInt("AppointmentID")); // Колонка 0
                row.add(rs.getString("PatientFullName")); // Колонка 1
                Timestamp appDate = rs.getTimestamp("AppointmentDate");
                row.add(appDate != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(appDate) : "N/A"); // Колонка 2
                row.add(rs.getString("Status")); // Колонка 3

                tableModel.addRow(row); // Додаємо видимий рядок
            }
            logger.log(Level.INFO,"Успішно завантажено {0} прийомів для DoctorID: {1}", new Object[]{tableModel.getRowCount(), this.doctorId});

        } catch (SQLException e) {
            logger.log(Level.SEVERE,"Помилка SQL при завантаженні прийомів для DoctorID: " + this.doctorId, e);
            JOptionPane.showMessageDialog(this, "Помилка бази даних при завантаженні прийомів:\n" + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Неочікувана помилка при завантаженні прийомів для DoctorID: " + this.doctorId, ex);
            JOptionPane.showMessageDialog(this, "Сталася неочікувана помилка: " + ex.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Метод для ВІДКРИТТЯ ІСТОРІЇ вибраного пацієнта ---
    private void viewSelectedPatientHistory() {
        int selectedRowView = appointmentTable.getSelectedRow(); // Індекс рядка у видимій таблиці

        if (selectedRowView >= 0) { // Якщо рядок вибрано
            // !!! ВАЖЛИВО: Конвертуємо View індекс в Model індекс !!!
            // Це необхідно, бо порядок рядків у моделі може не співпадати
            // з видимим порядком через сортування.
            // Також потрібно отримати PatientID зі списку patientIdsForRow за Model індексом.
            int selectedRowModel = appointmentTable.convertRowIndexToModel(selectedRowView);

            // Перевірка, чи індекс валідний для нашого паралельного списку
            if (selectedRowModel >= 0 && selectedRowModel < patientIdsForRow.size()) {
                // Отримуємо ID пацієнта зі списку за індексом моделі
                int selectedPatientId = patientIdsForRow.get(selectedRowModel);
                String patientName = tableModel.getValueAt(selectedRowModel, 1).toString(); // Отримуємо ім'я для логування

                logger.log(Level.INFO, "Лікар {0} відкриває історію для PatientID: {1} ({2})",
                        new Object[]{this.doctorId, selectedPatientId, patientName});

                // Створюємо та показуємо нове вікно історії хвороби для лікаря
                new DoctorPatientHistoryPanel(this.doctorId, selectedPatientId);

            } else {
                logger.log(Level.SEVERE, "Помилка отримання PatientID для вибраного рядка (Model Index: {0})", selectedRowModel);
                JOptionPane.showMessageDialog(this, "Не вдалося визначити пацієнта для вибраного запису.", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Якщо жоден рядок не вибрано
            JOptionPane.showMessageDialog(this, "Будь ласка, спочатку виберіть запис пацієнта зі списку!", "Увага", JOptionPane.WARNING_MESSAGE);
        }
    }


    // --- Метод зміни статусу (залишається майже без змін) ---
    private void changeSelectedAppointmentStatus() {
        int selectedRowView = appointmentTable.getSelectedRow();
        if (selectedRowView >= 0) {
            int selectedRowModel = appointmentTable.convertRowIndexToModel(selectedRowView);
            int appointmentId = (int) tableModel.getValueAt(selectedRowModel, 0);
            String currentStatus = tableModel.getValueAt(selectedRowModel, 3).toString();
            // ... (решта коду методу як у попередній версії) ...
            logger.log(Level.INFO,"Лікар {0} ініціює зміну статусу для AppointmentID: {1} (поточний: {2})",
                    new Object[]{this.doctorId, appointmentId, currentStatus});

            String newStatus = (String) JOptionPane.showInputDialog(
                    this, "Виберіть новий статус для запису ID: " + appointmentId,
                    "Зміна Статусу Прийому", JOptionPane.QUESTION_MESSAGE, null,
                    DOCTOR_SETTABLE_STATUSES, DOCTOR_SETTABLE_STATUSES[0]);

            if (newStatus != null) {
                logger.log(Level.INFO, "Лікар {0} вибрав новий статус '{1}' для AppointmentID: {2}",
                        new Object[]{this.doctorId, newStatus, appointmentId});
                String updateSql = "UPDATE dbo.Appointments SET Status = ? WHERE AppointmentID = ? AND DoctorID = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, appointmentId);
                    stmt.setInt(3, this.doctorId);
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(this, "Статус прийому успішно оновлено.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadDoctorAppointments();
                    } else {
                        logger.log(Level.WARNING, "Не вдалося оновити статус для AppointmentID: {0}. Можливо, запис не належить лікарю {1}.",
                                new Object[]{appointmentId, this.doctorId});
                        JOptionPane.showMessageDialog(this, "Не вдалося оновити статус.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Помилка SQL при оновленні статусу для AppointmentID: " + appointmentId, e);
                    JOptionPane.showMessageDialog(this, "Помилка бази даних:\n" + e.getMessage(), "Помилка SQL", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.log(Level.INFO, "Лікар {0} скасував зміну статусу для AppointmentID: {1}", new Object[]{this.doctorId, appointmentId});
            }
        } else {
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть запис для зміни статусу!", "Увага", JOptionPane.WARNING_MESSAGE);
        }
    }

    // --- Метод видалення (залишається майже без змін) ---
    private void deleteSelectedAppointment() {
        int selectedRowView = appointmentTable.getSelectedRow();
        if (selectedRowView >= 0) {
            int selectedRowModel = appointmentTable.convertRowIndexToModel(selectedRowView);
            int appointmentId = (int) tableModel.getValueAt(selectedRowModel, 0);
            String patientName = tableModel.getValueAt(selectedRowModel, 1).toString();
            String appDate = tableModel.getValueAt(selectedRowModel, 2).toString();

            int confirmation = JOptionPane.showConfirmDialog(this,
                    String.format("Видалити запис №%d?\nПацієнт: %s\nДата: %s", appointmentId, patientName, appDate),
                    "Підтвердження Видалення", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirmation == JOptionPane.YES_OPTION) {
                logger.log(Level.INFO, "Лікар {0} підтвердив видалення AppointmentID: {1}", new Object[]{this.doctorId, appointmentId});
                String deleteSql = "DELETE FROM dbo.Appointments WHERE AppointmentID = ? AND DoctorID = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setInt(1, appointmentId);
                    stmt.setInt(2, this.doctorId);
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(this, "Запис успішно видалено.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadDoctorAppointments();
                    } else {
                        logger.log(Level.WARNING, "Не вдалося видалити AppointmentID: {0}. Можливо, запис не належить лікарю {1}.",
                                new Object[]{appointmentId, this.doctorId});
                        JOptionPane.showMessageDialog(this, "Не вдалося видалити запис.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Помилка SQL при видаленні AppointmentID: " + appointmentId, e);
                    JOptionPane.showMessageDialog(this, "Помилка бази даних:\n" + e.getMessage(), "Помилка SQL", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.log(Level.INFO, "Лікар {0} скасував дію видалення запису AppointmentID: {1}", new Object[]{this.doctorId, appointmentId});
            }
        } else {
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть запис для видалення!", "Увага", JOptionPane.WARNING_MESSAGE);
        }
    }

} // --- Кінець класу ---