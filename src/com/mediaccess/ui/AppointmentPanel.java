package com.mediaccess.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat; // Додано для можливого форматування дати
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

// Ця панель показує записи для КОНКРЕТНОГО пацієнта
public class AppointmentPanel extends JFrame {
    private JTable appointmentTable;
    private DefaultTableModel tableModel;
    private int patientId; // ID авторизованого пацієнта
    private static final Logger logger = Logger.getLogger(AppointmentPanel.class.getName());

    // --- КОНСТРУКТОР, ЩО ПРИЙМАЄ patientId ---
    public AppointmentPanel(int patientId) {
        // Перевірка, чи ID пацієнта валідний (більше 0)
        if (patientId <= 0) {
            logger.log(Level.SEVERE, "Спроба створити AppointmentPanel з невалідним PatientID: {0}", patientId);
            // Можна кинути виняток або показати повідомлення і закрити
            JOptionPane.showMessageDialog(null, "Помилка: Некоректний ID пацієнта.", "Критична Помилка", JOptionPane.ERROR_MESSAGE);
            // Закриваємо вікно, бо воно не може працювати без валідного ID
            SwingUtilities.invokeLater(this::dispose); // Закрити пізніше в потоці EDT
            // throw new IllegalArgumentException("Некоректний patientId: " + patientId); // Альтернатива - кинути виняток
            return; // Виходимо з конструктора
        }

        this.patientId = patientId;
        logger.log(Level.INFO, "Створення AppointmentPanel для PatientID: {0}", this.patientId);

        setTitle("Мої Записи на Прийом");
        setSize(800, 500);
        setLocationRelativeTo(null); // По центру відносно батька/екрану
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Закривати тільки це вікно
        setLayout(new BorderLayout(10, 10));

        // Модель таблиці (нередагована)
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        appointmentTable = new JTable(tableModel);
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appointmentTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(appointmentTable);

        // Кнопки для пацієнта
        JButton addAppointmentButton = new JButton("Додати Новий Запис");
        JButton deleteAppointmentButton = new JButton("Скасувати Вибраний Запис"); // Змінено текст
        JButton refreshButton = new JButton("Оновити Список"); // Кнопка оновлення

        // Обробники кнопок
        addAppointmentButton.addActionListener(e -> openAddAppointmentDialog());
        deleteAppointmentButton.addActionListener(e -> deleteSelectedAppointment());
        refreshButton.addActionListener(e -> loadAppointments()); // Оновлення при натисканні

        // Панель для кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addAppointmentButton);
        buttonPanel.add(deleteAppointmentButton);
        buttonPanel.add(refreshButton);

        // Додавання компонентів
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Завантаження записів при відкритті
        loadAppointments();

        setVisible(true);
    }
    // --- КІНЕЦЬ КОНСТРУКТОРА ---

    // Метод завантаження записів ТІЛЬКИ для ЦЬОГО пацієнта
    private void loadAppointments() {
        logger.log(Level.INFO, "Завантаження записів для PatientID: {0}", this.patientId);
        tableModel.setRowCount(0); // Очищуємо рядки
        tableModel.setColumnCount(0); // Очищуємо колонки

        // Запит вибирає записи пацієнта з іменем лікаря та статусом
        String sql = "SELECT a.AppointmentID, d.FirstName + ' ' + d.LastName AS DoctorFullName, " +
                "a.AppointmentDate, a.Status " +
                "FROM dbo.Appointments a " +
                "INNER JOIN dbo.Doctors d ON a.DoctorID = d.DoctorID " + // INNER JOIN - лікар має існувати
                "WHERE a.PatientID = ? " + // !!! ФІЛЬТР по PatientID !!!
                "ORDER BY a.AppointmentDate"; // Сортування

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, patientId); // !!! Встановлюємо ID пацієнта в запит !!!
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Додаємо колонки (один раз)
            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(meta.getColumnLabel(i));
            }

            // Додаємо рядки
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    // Якщо потрібно форматувати дату
                    if (value instanceof Timestamp) {
                        value = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(value);
                    }
                    row.add(value);
                }
                tableModel.addRow(row);
            }
            logger.log(Level.INFO, "Успішно завантажено {0} записів для PatientID: {1}", new Object[]{tableModel.getRowCount(), this.patientId});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при завантаженні записів для PatientID: " + this.patientId, e);
            JOptionPane.showMessageDialog(this, "Помилка бази даних при завантаженні записів:\n" + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Неочікувана помилка при завантаженні записів для PatientID: " + this.patientId, ex);
            JOptionPane.showMessageDialog(this, "Сталася неочікувана помилка: " + ex.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Метод відкриття діалогу ДОДАВАННЯ запису (пацієнт додає для себе)
    private void openAddAppointmentDialog() {
        logger.log(Level.INFO, "PatientID: {0} відкриває діалог додавання запису", this.patientId);
        JDialog dialog = new JDialog(this, "Створити Новий Запис на Прийом", true);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Компоненти діалогу ---
        // Випадаючий список для лікарів
        JComboBox<DoctorItem> doctorComboBox = new JComboBox<>();
        // Поле для дати (можна використовувати JDatePicker або аналог для зручності)
        JTextField appointmentDateField = new JTextField(16);
        // Статус при створенні пацієнтом завжди "Заплановано"
        JLabel statusLabelInfo = new JLabel("Статус буде встановлено як 'Заплановано'");

        // Завантажуємо лікарів у список
        loadDoctorsIntoComboBox(doctorComboBox);
        // Якщо лікарів немає, можливо, не варто відкривати діалог або показати повідомлення
        if(doctorComboBox.getItemCount() == 0){
            JOptionPane.showMessageDialog(this, "На даний момент немає доступних лікарів для запису.", "Інформація", JOptionPane.INFORMATION_MESSAGE);
            return; // Не відкриваємо діалог, якщо немає лікарів
        }

        // --- Розміщення компонентів ---
        int row = 0;
        gbc.gridwidth = 1;
        addRowDialog(dialog, gbc, new JLabel("Оберіть Лікаря:"), doctorComboBox, row++);
        addRowDialog(dialog, gbc, new JLabel("Дата та Час (РРРР-ММ-ДД ГГ:ХХ):"), appointmentDateField, row++);
        addRowDialog(dialog, gbc, statusLabelInfo, null, row++); // Просто мітка статусу

        JButton saveButton = new JButton("Зберегти Запис");
        saveButton.addActionListener(e -> {
            DoctorItem selectedDoctorItem = (DoctorItem) doctorComboBox.getSelectedItem();
            String dateText = appointmentDateField.getText().trim();
            String status = "Заплановано"; // Статус за замовчуванням

            // Перевірки вводу
            if (selectedDoctorItem == null) {
                JOptionPane.showMessageDialog(dialog, "Будь ласка, оберіть лікаря!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (dateText.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Будь ласка, введіть дату та час прийому!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
                appointmentDateField.requestFocus();
                return;
            }

            // Валідація та конвертація дати
            Timestamp appointmentTimestamp;
            try {
                // Додаємо ':00' якщо користувач ввів тільки години та хвилини
                if (dateText.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                    dateText += ":00";
                }
                // Перевіряємо повний формат РРРР-ММ-ДД ГГ:ХХ:СС
                else if (!dateText.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                    throw new IllegalArgumentException("Невірний формат дати/часу.");
                }
                appointmentTimestamp = Timestamp.valueOf(dateText);

                // Перевірка, чи дата не в минулому
                if (appointmentTimestamp.before(new Timestamp(System.currentTimeMillis() - 60000))) { // -60000ms для невеликої похибки
                    JOptionPane.showMessageDialog(dialog, "Дата та час прийому не можуть бути в минулому!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
                    appointmentDateField.requestFocus();
                    return;
                }
            } catch (IllegalArgumentException exDate) {
                logger.log(Level.WARNING, "Некоректний формат дати/часу в діалозі додавання: {0}", dateText);
                JOptionPane.showMessageDialog(dialog, "Некоректний формат дати/часу!\nВведіть у форматі РРРР-ММ-ДД ГГ:ХХ", "Помилка формату", JOptionPane.ERROR_MESSAGE);
                appointmentDateField.requestFocus();
                return;
            }

            int doctorId = selectedDoctorItem.getId(); // ID вибраного лікаря

            logger.log(Level.INFO, "Спроба додати запис: PatientID={0}, DoctorID={1}, Date={2}, Status={3}",
                    new Object[]{this.patientId, doctorId, appointmentTimestamp, status});

            // --- Збереження в БД ---
            String insertSql = "INSERT INTO dbo.Appointments (PatientID, DoctorID, AppointmentDate, Status) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                stmt.setInt(1, this.patientId); // ID поточного пацієнта
                stmt.setInt(2, doctorId);
                stmt.setTimestamp(3, appointmentTimestamp);
                stmt.setString(4, status);

                stmt.executeUpdate();
                logger.log(Level.INFO, "Запис успішно додано для PatientID={0}", this.patientId);
                JOptionPane.showMessageDialog(dialog, "Запис на прийом успішно створено!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadAppointments(); // Оновлюємо список записів пацієнта

            } catch (SQLException exSQL) {
                logger.log(Level.SEVERE, "Помилка SQL при додаванні запису для PatientID: " + this.patientId, exSQL);
                // Перевірка на специфічні помилки БД (наприклад, обмеження)
                if (exSQL.getMessage().contains("constraint")) { // Приклад перевірки
                    JOptionPane.showMessageDialog(dialog, "Помилка збереження: Можливий конфлікт даних або порушення обмежень.", "Помилка Бази Даних", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Помилка бази даних при збереженні запису:\n" + exSQL.getMessage(), "Помилка SQL", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) { // Інші помилки
                logger.log(Level.SEVERE, "Неочікувана помилка при додаванні запису для PatientID: " + this.patientId, ex);
                JOptionPane.showMessageDialog(dialog, "Сталася неочікувана помилка: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Додаємо кнопку Зберегти
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        dialog.add(saveButton, gbc);

        dialog.setVisible(true); // Показуємо діалог
    }

    // Метод завантаження лікарів у JComboBox
    private void loadDoctorsIntoComboBox(JComboBox<DoctorItem> comboBox) {
        logger.log(Level.INFO, "Завантаження списку лікарів для ComboBox");
        comboBox.removeAllItems(); // Очищення перед заповненням
        // Запит вибирає ID, імена та спеціалізацію лікарів
        String sql = "SELECT DoctorID, FirstName, LastName, Specialization FROM dbo.Doctors ORDER BY LastName, FirstName";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("DoctorID");
                String name = rs.getString("LastName") + " " + rs.getString("FirstName");
                String specialization = rs.getString("Specialization");
                // Створюємо і додаємо об'єкт DoctorItem
                comboBox.addItem(new DoctorItem(id, name + " (" + specialization + ")"));
            }
            logger.log(Level.INFO, "Завантажено {0} лікарів у ComboBox", comboBox.getItemCount());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при завантаженні лікарів для ComboBox", e);
            JOptionPane.showMessageDialog(this, "Помилка завантаження списку лікарів:\n" + e.getMessage(),
                    "Помилка Бази Даних", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Метод видалення (скасування) ВИБРАНОГО запису пацієнтом
    private void deleteSelectedAppointment() {
        int selectedRowView = appointmentTable.getSelectedRow(); // Індекс рядка як його бачить користувач
        if (selectedRowView >= 0) {
            // Конвертуємо індекс View в індекс Model (важливо при сортуванні)
            int selectedRowModel = appointmentTable.convertRowIndexToModel(selectedRowView);
            // Отримуємо дані з моделі таблиці
            int appointmentId = (int) tableModel.getValueAt(selectedRowModel, 0); // ID запису
            String doctorName = tableModel.getValueAt(selectedRowModel, 1).toString(); // Ім'я лікаря
            String appDateStr = tableModel.getValueAt(selectedRowModel, 2).toString(); // Дата як рядок (вже відформатована)

            // Запитуємо підтвердження у користувача
            int confirmation = JOptionPane.showConfirmDialog(
                    this,
                    String.format("Ви впевнені, що хочете скасувати запис №%d?\nЛікар: %s\nДата: %s",
                            appointmentId, doctorName, appDateStr),
                    "Підтвердження Скасування Запису",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            // Якщо користувач підтвердив
            if (confirmation == JOptionPane.YES_OPTION) {
                logger.log(Level.INFO, "PatientID: {0} підтвердив скасування запису AppointmentID: {1}", new Object[]{this.patientId, appointmentId});
                // SQL запит на видалення, з перевіркою PatientID
                String deleteSql = "DELETE FROM dbo.Appointments WHERE AppointmentID = ? AND PatientID = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(deleteSql)) {

                    stmt.setInt(1, appointmentId);
                    stmt.setInt(2, this.patientId); // Переконуємось, що видаляємо СВІЙ запис

                    int rowsAffected = stmt.executeUpdate(); // Виконуємо запит
                    if (rowsAffected > 0) {
                        // Успішно видалено
                        JOptionPane.showMessageDialog(this, "Запис успішно скасовано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadAppointments(); // Оновлюємо таблицю
                    } else {
                        // Не вдалося видалити (можливо, вже видалено або ID не той)
                        logger.log(Level.WARNING, "Не вдалося видалити запис AppointmentID: {0} для PatientID: {1}. RowsAffected=0.", new Object[]{appointmentId, this.patientId});
                        JOptionPane.showMessageDialog(this, "Не вдалося скасувати запис.\nМожливо, його вже було скасовано раніше.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    // Помилка SQL під час видалення
                    logger.log(Level.SEVERE, "Помилка SQL при скасуванні запису AppointmentID: " + appointmentId, e);
                    JOptionPane.showMessageDialog(this, "Помилка бази даних при скасуванні запису:\n" + e.getMessage(), "Помилка SQL", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Користувач натиснув "Ні"
                logger.log(Level.INFO, "PatientID: {0} скасував дію видалення запису AppointmentID: {1}", new Object[]{this.patientId, appointmentId});
            }
        } else {
            // Якщо жоден рядок не вибрано
            JOptionPane.showMessageDialog(this, "Будь ласка, спочатку виберіть запис зі списку для скасування!", "Увага", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Допоміжний метод для додавання рядків у діалозі
    private void addRowDialog(JDialog dialog, GridBagConstraints gbc, JLabel label, JComponent field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = (field == null) ? 2 : 1; // Мітка займає 2 колонки, якщо немає поля
        gbc.anchor = GridBagConstraints.EAST;
        if(field == null) gbc.anchor = GridBagConstraints.WEST; // Мітку статусу ліворуч
        gbc.fill = GridBagConstraints.NONE;
        dialog.add(label, gbc);

        if (field != null) {
            gbc.gridx = 1;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            dialog.add(field, gbc);
        }
    }

    // Внутрішній клас для зберігання ID та імені лікаря в ComboBox
    // Дозволяє легко отримати ID вибраного лікаря
    private static class DoctorItem {
        private final int id;
        private final String name;

        public DoctorItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        // Метод toString() визначає, що буде відображатися в списку JComboBox
        @Override
        public String toString() {
            return name;
        }

        // Бажано також перевизначити equals() та hashCode(), якщо об'єкти
        // порівнюватимуться, але для простого відображення це не обов'язково.
    }
}