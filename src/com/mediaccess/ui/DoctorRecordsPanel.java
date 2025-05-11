package com.mediaccess.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*; // Потрібно для SQLException, Connection, Statement, ResultSet
import java.util.logging.Level; // Додано для логування
import java.util.logging.Logger; // Додано для логування
import java.sql.CallableStatement; // Потрібно для виклику SP

public class DoctorRecordsPanel extends JFrame {

    private JTable doctorTable;
    private DefaultTableModel tableModel;
    // Логер для цього класу
    private static final Logger logger = Logger.getLogger(DoctorRecordsPanel.class.getName());

    // Конструктор панелі
    public DoctorRecordsPanel() {
        logger.log(Level.INFO, "Створення DoctorRecordsPanel.");
        setTitle("Картотека Лікарів");
        setSize(700, 400); // Розмір вікна
        setLocationRelativeTo(null); // По центру
        // Важливо: DISPOSE_ON_CLOSE, щоб закривалося тільки це вікно, а не вся програма
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Використовуємо BorderLayout

        // Створення моделі таблиці з назвами колонок
        tableModel = new DefaultTableModel(new String[]{"ID", "Ім'я", "Прізвище", "Спеціалізація", "Email"}, 0) {
            // Робимо комірки нередагованими
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        doctorTable = new JTable(tableModel);
        doctorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Вибір одного рядка
        doctorTable.setAutoCreateRowSorter(true); // Вмикаємо сортування

        // Завантажуємо дані лікарів при створенні панелі
        loadDoctors();

        // Створення кнопки "Додати лікаря"
        JButton addButton = new JButton("Додати Нового Лікаря");
        // Додаємо обробник події, який викликає оновлений діалог
        addButton.addActionListener(e -> showAddDoctorDialog());

        // Додаємо компоненти на панель
        // Таблицю з прокруткою додаємо в центр
        add(new JScrollPane(doctorTable), BorderLayout.CENTER);
        // Кнопку додаємо вниз
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Панель для кнопки
        bottomPanel.add(addButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Робимо вікно видимим
        setVisible(true);
    }

    // Метод завантаження списку лікарів з бази даних
    private void loadDoctors() {
        logger.log(Level.INFO, "Завантаження списку лікарів.");
        // Очищуємо таблицю перед завантаженням нових даних
        tableModel.setRowCount(0);
        String sql = "SELECT DoctorID, FirstName, LastName, Specialization, Email FROM dbo.Doctors ORDER BY LastName, FirstName"; // Запит для отримання лікарів

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Додаємо рядки в таблицю
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("DoctorID"),
                        rs.getString("FirstName"),
                        rs.getString("LastName"),
                        rs.getString("Specialization"),
                        rs.getString("Email")
                });
            }
            logger.log(Level.INFO, "Успішно завантажено {0} лікарів.", tableModel.getRowCount());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка SQL при завантаженні лікарів.", e);
            JOptionPane.showMessageDialog(this, "Помилка бази даних при завантаженні списку лікарів!\n" + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Неочікувана помилка при завантаженні лікарів.", ex);
            JOptionPane.showMessageDialog(this, "Сталася неочікувана помилка: " + ex.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- ОНОВЛЕНИЙ МЕТОД ДЛЯ ДОДАВАННЯ ЛІКАРЯ (ВИКЛИКАЄ SP) ---
    private void showAddDoctorDialog() {
        logger.log(Level.INFO, "Відкриття діалогу додавання лікаря.");
        // Створюємо компоненти діалогу
        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField specializationField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        // Можна додати поле для телефону, якщо процедура sp_RegisterDoctor його приймає
        // JTextField phoneField = new JTextField(20);

        // Створюємо панель для розміщення компонентів
        // Використовуємо BoxLayout для простого вертикального розміщення
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Вертикальний BoxLayout
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Додаємо відступи

        // Додаємо компоненти з мітками на панель
        panel.add(new JLabel("Ім'я:*")); panel.add(firstNameField);
        panel.add(Box.createVerticalStrut(5)); // Невеликий вертикальний відступ
        panel.add(new JLabel("Прізвище:*")); panel.add(lastNameField);
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JLabel("Спеціалізація:*")); panel.add(specializationField);
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JLabel("Email:*")); panel.add(emailField);
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JLabel("Пароль для входу:*")); panel.add(passwordField);
        // Якщо додали телефон:
        // panel.add(Box.createVerticalStrut(5));
        // panel.add(new JLabel("Телефон (опціонально):")); panel.add(phoneField);

        // Показуємо діалог JOptionPane для вводу даних
        int result = JOptionPane.showConfirmDialog(this, panel,
                "Додати Нового Лікаря", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // Якщо користувач натиснув "OK"
        if (result == JOptionPane.OK_OPTION) {
            // Отримуємо введені дані, видаляючи зайві пробіли
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String specialization = specializationField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword()); // Пароль як рядок
            // String phone = phoneField.getText().trim(); // Якщо додали поле телефону

            // --- Перевірка введених даних ---
            if (firstName.isEmpty() || lastName.isEmpty() || specialization.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Будь ласка, заповніть всі поля, позначені зірочкою (*).",
                        "Помилка вводу", JOptionPane.WARNING_MESSAGE);
                return; // Виходимо, якщо є порожні поля
            }
            // Проста перевірка формату email
            if (!email.contains("@") || !email.contains(".")) {
                JOptionPane.showMessageDialog(this, "Некоректний формат Email!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
                emailField.requestFocus();
                return;
            }
            // Перевірка довжини пароля
            if (password.length() < 6) { // Або ваші правила складності
                JOptionPane.showMessageDialog(this, "Пароль повинен містити щонайменше 6 символів!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
                passwordField.requestFocus();
                return;
            }

            logger.log(Level.INFO, "Спроба додати лікаря через SP: Email={0}", email);

            // --- Викликаємо збережену процедуру sp_RegisterDoctor ---
            try (Connection conn = DatabaseManager.getConnection();
                 // Готуємо виклик процедури
                 // !!! Переконайтесь, що кількість знаків '?' відповідає кількості параметрів у вашій SP sp_RegisterDoctor
                 // Якщо SP приймає @Phone, то має бути 6 знаків '?'
                 CallableStatement stmt = conn.prepareCall("{call sp_RegisterDoctor(?, ?, ?, ?, ?, ?)}")) { // Припускаємо 6 параметрів (з телефоном)

                // Встановлюємо параметри процедури
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, specialization);
                stmt.setString(4, email);
                stmt.setNull(5, Types.NVARCHAR); // Встановлюємо NULL для телефону, якщо поле не використовується
                // stmt.setString(5, phone); // Якщо використовуєте поле телефону
                // !!! Передаємо ЗВИЧАЙНИЙ пароль !!! Процедура сама його хешує.
                stmt.setString(6, password); // Пароль - останній параметр

                // Виконуємо процедуру
                stmt.execute();

                // Якщо процедура виконалась без помилок SQL (RAISERROR)
                logger.log(Level.INFO, "Лікар успішно доданий через SP: Email={0}", email);
                JOptionPane.showMessageDialog(this, "Лікаря '" + firstName + " " + lastName + "' успішно додано!",
                        "Успіх", JOptionPane.INFORMATION_MESSAGE);
                // Оновлюємо таблицю лікарів у поточному вікні
                loadDoctors();

            } catch (SQLException e) {
                // Обробляємо помилки, які повернула процедура (RAISERROR) або інші помилки SQL
                logger.log(Level.SEVERE, "Помилка SQL при додаванні лікаря: Email=" + email, e);
                // Показуємо повідомлення про помилку з процедури або загальне повідомлення
                JOptionPane.showMessageDialog(this, "Помилка при додаванні лікаря:\n" + e.getMessage(),
                        "Помилка Бази Даних", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                // Інші неочікувані помилки (наприклад, ClassNotFoundException, якщо драйвер не знайдено)
                logger.log(Level.SEVERE, "Неочікувана помилка при додаванні лікаря: Email=" + email, ex);
                JOptionPane.showMessageDialog(this, "Сталася неочікувана помилка:\n" + ex.getMessage(),
                        "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Користувач натиснув "Cancel"
            logger.log(Level.INFO, "Додавання лікаря скасовано користувачем.");
        }
    } // --- Кінець методу showAddDoctorDialog ---

} // --- Кінець класу DoctorRecordsPanel ---