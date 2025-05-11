package com.mediaccess.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

// Панель для лікаря: Перегляд історії пацієнта + Додавання нового запису
public class DoctorPatientHistoryPanel extends JFrame {

    private JTable historyTable;
    private DefaultTableModel tableModel;
    private int patientId;
    private int doctorId; // ID лікаря, який переглядає/додає
    private JLabel patientNameLabel;
    private static final Logger logger = Logger.getLogger(DoctorPatientHistoryPanel.class.getName());

    public DoctorPatientHistoryPanel(int doctorId, int patientId) {
        // Перевірка ID
        if (patientId <= 0 || doctorId <= 0) {
            logger.log(Level.SEVERE, "Спроба створити DoctorPatientHistoryPanel з невалідним ID: DoctorID={0}, PatientID={1}", new Object[]{doctorId, patientId});
            JOptionPane.showMessageDialog(null, "Помилка: Некоректний ID лікаря або пацієнта.", "Критична Помилка", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        this.patientId = patientId;
        this.doctorId = doctorId;
        logger.log(Level.INFO, "Створення DoctorPatientHistoryPanel: DoctorID={0}, PatientID={1}", new Object[]{this.doctorId, this.patientId});


        setTitle("Медична Історія Пацієнта");
        setSize(800, 600); // Трохи більше місця для кнопки додавання
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- Верхня панель з іменем пацієнта ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        patientNameLabel = new JLabel("Завантаження...");
        topPanel.add(new JLabel("Пацієнт:"));
        topPanel.add(patientNameLabel);
        add(topPanel, BorderLayout.NORTH);

        // --- Таблиця історії ---
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        historyTable = new JTable(tableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- Нижня панель з кнопками ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addEntryButton = new JButton("Додати Запис в Історію"); // Кнопка для лікаря
        JButton refreshButton = new JButton("Оновити");
        JButton closeButton = new JButton("Закрити");

        // Обробники кнопок
        addEntryButton.addActionListener(e -> openAddHistoryEntryDialog()); // Виклик діалогу додавання
        refreshButton.addActionListener(e -> {
            fetchPatientName();
            loadMedicalHistory();
        });
        closeButton.addActionListener(e -> dispose());

        bottomPanel.add(addEntryButton);
        bottomPanel.add(refreshButton);
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Завантаження даних
        fetchPatientName();
        loadMedicalHistory();

        setVisible(true);
    }

    // --- Отримання імені пацієнта ---
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
                logger.log(Level.WARNING, "Пацієнта з PatientID {0} не знайдено.", this.patientId);
                patientNameLabel.setText("Пацієнта не знайдено (ID: " + this.patientId + ")");
                patientNameLabel.setForeground(Color.RED);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при отриманні імені пацієнта PatientID: " + this.patientId, e);
            patientNameLabel.setText("Помилка завантаження імені");
            patientNameLabel.setForeground(Color.RED);
        }
    }

    // --- Завантаження історії хвороби ---
    private void loadMedicalHistory() {
        logger.log(Level.INFO, "Завантаження мед. історії для PatientID: {0} (лікар {1})", new Object[]{this.patientId, this.doctorId});
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        String sql = "SELECT mh.HistoryID, mh.DateAdded, mh.Diagnosis, mh.Treatment, " +
                "ISNULL(d.FirstName + ' ' + d.LastName, 'N/A') AS DoctorFullName " +
                "FROM dbo.MedicalHistory mh " +
                "LEFT JOIN dbo.Doctors d ON mh.DoctorID = d.DoctorID " +
                "WHERE mh.PatientID = ? " +
                "ORDER BY mh.DateAdded DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, this.patientId);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();

            // Встановлюємо назви колонок
            tableModel.addColumn("ID Запису");
            tableModel.addColumn("Дата");
            tableModel.addColumn("Діагноз");
            tableModel.addColumn("Лікування");
            tableModel.addColumn("Лікар");

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("HistoryID"));
                Timestamp dateAdded = rs.getTimestamp("DateAdded");
                row.add(dateAdded != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(dateAdded) : "N/A");
                row.add(rs.getString("Diagnosis"));
                row.add(rs.getString("Treatment"));
                row.add(rs.getString("DoctorFullName"));
                tableModel.addRow(row);
            }
            logger.log(Level.INFO, "Завантажено {0} записів історії для PatientID: {1}", new Object[]{tableModel.getRowCount(), this.patientId});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при завантаженні мед. історії для PatientID: " + this.patientId, e);
            JOptionPane.showMessageDialog(this, "Помилка бази даних при завантаженні історії:\n" + e.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Відкриття діалогу для ДОДАВАННЯ запису лікарем ---
    private void openAddHistoryEntryDialog() {
        logger.log(Level.INFO, "DoctorID: {0} відкриває діалог додавання запису в історію PatientID: {1}", new Object[]{this.doctorId, this.patientId});

        // Створюємо компоненти діалогу
        JTextArea diagnosisArea = new JTextArea(5, 30); // Поле для діагнозу
        diagnosisArea.setLineWrap(true); // Перенос рядків
        diagnosisArea.setWrapStyleWord(true); // Перенос по словах
        JTextArea treatmentArea = new JTextArea(5, 30); // Поле для лікування
        treatmentArea.setLineWrap(true);
        treatmentArea.setWrapStyleWord(true);

        // Панель для діалогу
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // Панель для міток
        labelsPanel.add(new JLabel("Діагноз:*"));
        labelsPanel.add(Box.createVerticalStrut(60)); // Відступ для вирівнювання з полем лікування
        labelsPanel.add(new JLabel("Призначене Лікування:"));


        JPanel fieldsPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // Панель для полів вводу
        fieldsPanel.add(new JScrollPane(diagnosisArea)); // Додаємо прокрутку для діагнозу
        fieldsPanel.add(new JScrollPane(treatmentArea)); // Додаємо прокрутку для лікування

        panel.add(labelsPanel, BorderLayout.WEST);
        panel.add(fieldsPanel, BorderLayout.CENTER);


        // Показуємо діалог
        int result = JOptionPane.showConfirmDialog(this, panel,
                "Додати Запис до Медичної Історії",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        // Якщо натиснуто "OK"
        if (result == JOptionPane.OK_OPTION) {
            String diagnosis = diagnosisArea.getText().trim();
            String treatment = treatmentArea.getText().trim();

            // Перевірка, чи введено діагноз (обов'язкове поле)
            if (diagnosis.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Поле 'Діагноз' є обов'язковим для заповнення!",
                        "Помилка вводу", JOptionPane.WARNING_MESSAGE);
                // Можна рекурсивно викликати діалог знову, передавши введені дані
                // openAddHistoryEntryDialog(diagnosis, treatment);
                return;
            }

            // Викликаємо метод збереження
            saveHistoryEntry(diagnosis, treatment);
        } else {
            logger.log(Level.INFO, "Додавання запису в історію скасовано лікарем {0}", this.doctorId);
        }
    }

    // --- Метод для ЗБЕРЕЖЕННЯ нового запису в історію ---
    private void saveHistoryEntry(String diagnosis, String treatment) {
        logger.log(Level.INFO, "Спроба зберегти запис в історію: DoctorID={0}, PatientID={1}", new Object[]{this.doctorId, this.patientId});

        // SQL запит для вставки нового запису
        // DateAdded встановлюється автоматично (DEFAULT GETDATE())
        String insertSql = "INSERT INTO dbo.MedicalHistory (PatientID, DoctorID, Diagnosis, Treatment) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setInt(1, this.patientId); // ID пацієнта
            stmt.setInt(2, this.doctorId); // ID лікаря, який додав запис
            stmt.setString(3, diagnosis);  // Діагноз
            // Обробляємо порожнє поле лікування як NULL
            if (treatment.isEmpty()) {
                stmt.setNull(4, Types.NVARCHAR);
            } else {
                stmt.setString(4, treatment); // Лікування
            }


            int rowsAffected = stmt.executeUpdate(); // Виконуємо вставку

            if (rowsAffected > 0) {
                logger.log(Level.INFO,"Новий запис успішно додано до історії PatientID: {0} лікарем {1}", new Object[]{this.patientId, this.doctorId});
                JOptionPane.showMessageDialog(this, "Новий запис успішно додано до медичної історії.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                loadMedicalHistory(); // Оновлюємо таблицю, щоб побачити новий запис
            } else {
                logger.log(Level.WARNING, "Не вдалося додати запис до історії (RowsAffected=0) для PatientID: {0}", this.patientId);
                JOptionPane.showMessageDialog(this, "Не вдалося зберегти запис.", "Помилка Збереження", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при збереженні запису в історію для PatientID: " + this.patientId, e);
            JOptionPane.showMessageDialog(this, "Помилка бази даних при збереженні запису в історію:\n" + e.getMessage(), "Помилка SQL", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Неочікувана помилка при збереженні запису в історію для PatientID: " + this.patientId, ex);
            JOptionPane.showMessageDialog(this, "Сталася неочікувана помилка: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

} // --- Кінець класу DoctorPatientHistoryPanel ---