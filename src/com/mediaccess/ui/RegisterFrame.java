package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.CallableStatement; // Потрібен для виклику SP
import java.sql.Connection;
import java.sql.Date;       // Важливо: використовуємо java.sql.Date
import java.sql.SQLException;
import java.util.logging.Level; // Для логування
import java.util.logging.Logger; // Для логування

public class RegisterFrame extends JFrame {

    // Поля класу для компонентів вводу
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField dobField; // Поле для дати народження
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    // Логер для цього класу
    private static final Logger logger = Logger.getLogger(RegisterFrame.class.getName());

    // Конструктор фрейму
    public RegisterFrame() {
        setTitle("MediAccess - Реєстрація Нового Пацієнта"); // Уточнений заголовок
        setSize(500, 500); // Розмір вікна
        setLocationRelativeTo(null); // Вікно по центру екрану
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Закривати тільки це вікно
        setLayout(new GridBagLayout()); // Використовуємо GridBagLayout для гнучкості

        // Налаштування для GridBagConstraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10); // Відступи між компонентами
        gbc.fill = GridBagConstraints.HORIZONTAL; // Розтягувати компоненти по горизонталі
        gbc.anchor = GridBagConstraints.WEST;     // Вирівнювання міток по лівому краю (для міток)

        // Створення міток та полів вводу
        JLabel firstNameLabel = createFixedLabel("Ім'я:");
        firstNameField = new JTextField(20);
        JLabel lastNameLabel = createFixedLabel("Прізвище:");
        lastNameField = new JTextField(20);
        JLabel phoneLabel = createFixedLabel("Телефон:");
        phoneField = new JTextField(20);
        JLabel emailLabel = createFixedLabel("Email:");
        emailField = new JTextField(20);
        JLabel dobLabel = createFixedLabel("Дата народження (РРРР-ММ-ДД):"); // Вказали формат
        dobField = new JTextField(20);
        JLabel passLabel = createFixedLabel("Пароль:");
        passwordField = new JPasswordField(20);
        JLabel confirmPassLabel = createFixedLabel("Підтвердіть пароль:");
        confirmPasswordField = new JPasswordField(20);

        // Створення кнопок
        JButton registerButton = new JButton("Зареєструватися");
        registerButton.addActionListener(e -> registerUser()); // Додаємо обробник події

        JButton backButton = new JButton("Назад до Входу");
        backButton.addActionListener(e -> {
            logger.log(Level.INFO, "Користувач натиснув 'Назад' у вікні реєстрації.");
            dispose(); // Просто закриваємо вікно реєстрації
        });

        // Розміщення компонентів на панелі за допомогою GridBagLayout
        int row = 0;
        // Розміщуємо мітку і поле в кожному рядку
        addRow(gbc, firstNameLabel, firstNameField, row++);
        addRow(gbc, lastNameLabel, lastNameField, row++);
        addRow(gbc, phoneLabel, phoneField, row++);
        addRow(gbc, emailLabel, emailField, row++);
        addRow(gbc, dobLabel, dobField, row++);
        addRow(gbc, passLabel, passwordField, row++);
        addRow(gbc, confirmPassLabel, confirmPasswordField, row++);

        // Кнопка реєстрації займає дві колонки
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; // Кнопку по центру
        gbc.fill = GridBagConstraints.NONE; // Не розтягувати кнопку
        add(registerButton, gbc);

        // Кнопка "Назад" під кнопкою реєстрації
        gbc.gridy = ++row;
        add(backButton, gbc);

        // Робимо вікно видимим
        setVisible(true);
    }

    // Допоміжний метод для створення міток фіксованої ширини
    private JLabel createFixedLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT); // Вирівнювання тексту в мітці
        // Встановлюємо бажаний розмір, щоб вирівняти поля вводу
        label.setPreferredSize(new Dimension(180, 25)); // Трохи збільшив висоту
        return label;
    }

    // Допоміжний метод для додавання рядка (мітка + поле)
    private void addRow(GridBagConstraints gbc, JLabel label, JComponent field, int row) {
        gbc.gridx = 0; // Колонка 0 для мітки
        gbc.gridy = row; // Поточний рядок
        gbc.gridwidth = 1; // Займає одну колонку
        gbc.anchor = GridBagConstraints.EAST; // Вирівнюємо мітку праворуч
        gbc.fill = GridBagConstraints.NONE;   // Не розтягувати мітку
        add(label, gbc);

        gbc.gridx = 1; // Колонка 1 для поля вводу
        gbc.anchor = GridBagConstraints.WEST; // Вирівнюємо поле ліворуч
        gbc.fill = GridBagConstraints.HORIZONTAL; // Розтягувати поле
        add(field, gbc);
    }

    // Основний метод реєстрації користувача
    private void registerUser() {
        // Отримуємо дані з полів, видаляючи зайві пробіли
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String dobText = dobField.getText().trim(); // Дата народження як текст
        String password = new String(passwordField.getPassword()); // Пароль як рядок
        String confirmPassword = new String(confirmPasswordField.getPassword()); // Підтвердження пароля

        // --- Початкові перевірки введених даних ---
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || dobText.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Всі поля (крім телефону) є обов'язковими!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Перевірка формату Email (проста перевірка наявності '@')
        if (!email.contains("@") || !email.contains(".")) {
            JOptionPane.showMessageDialog(this, "Некоректний формат Email!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Перевірка співпадіння паролів
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Паролі не співпадають!", "Помилка вводу", JOptionPane.ERROR_MESSAGE);
            // Очищуємо поля паролів для повторного вводу
            passwordField.setText("");
            confirmPasswordField.setText("");
            passwordField.requestFocus(); // Ставимо фокус на перше поле пароля
            return;
        }
        // Додаткова перевірка складності пароля (приклад: мінімум 6 символів)
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Пароль повинен містити щонайменше 6 символів!", "Помилка вводу", JOptionPane.WARNING_MESSAGE);
            passwordField.setText("");
            confirmPasswordField.setText("");
            passwordField.requestFocus();
            return;
        }

        // Перевірка формату дати (РРРР-ММ-ДД)
        Date dateOfBirth = null;
        try {
            // Конвертуємо текст в java.sql.Date
            dateOfBirth = Date.valueOf(dobText);
        } catch (IllegalArgumentException exDate) {
            // Якщо формат дати неправильний
            logger.log(Level.WARNING, "Некоректний формат дати народження: {0}", dobText);
            JOptionPane.showMessageDialog(this, "Некоректний формат дати народження!\nБудь ласка, введіть у форматі РРРР-ММ-ДД.", "Помилка даних", JOptionPane.ERROR_MESSAGE);
            dobField.requestFocus(); // Фокус на поле дати
            return;
        }

        // --- Логіка підтвердження Email ---
        logger.log(Level.INFO, "Ініціювання підтвердження email для: {0}", email);
        // Генеруємо випадковий 6-значний код
        String confirmationCode = String.format("%06d", (int) (Math.random() * 1000000));

        // Створюємо/оновлюємо запит на скидання (використовуємо ту ж таблицю)
        DatabaseManager.createConfirmationCode(email, confirmationCode); // Помилки обробляються всередині

        // Надсилаємо код на email
        DatabaseManager.sendEmail(email, confirmationCode); // Помилки логуються всередині

        // Запитуємо код у користувача
        String inputCode = JOptionPane.showInputDialog(this,
                "На ваш Email (" + email + ") надіслано 6-значний код підтвердження.\n" +
                        "Будь ласка, введіть його тут (код дійсний 15 хвилин):",
                "Підтвердження Email",
                JOptionPane.PLAIN_MESSAGE); // Змінено тип повідомлення

        // Перевіряємо введений код
        if (inputCode == null) {
            logger.log(Level.WARNING, "Користувач скасував введення коду підтвердження для email: {0}", email);
            // Нічого не робимо, користувач натиснув "Cancel"
            return;
        }
        if (!DatabaseManager.validateConfirmationCode(email, inputCode.trim())) {
            logger.log(Level.WARNING, "Введено невірний або прострочений код підтвердження для email: {0}", email);
            JOptionPane.showMessageDialog(this, "Невірний або прострочений код підтвердження!", "Помилка підтвердження", JOptionPane.ERROR_MESSAGE);
            return;
        }
        logger.log(Level.INFO, "Email {0} успішно підтверджено.", email);


        // --- Виклик збереженої процедури для реєстрації ---
        logger.log(Level.INFO, "Спроба реєстрації користувача: Email={0}", email);
        try (Connection conn = DatabaseManager.getConnection();
             // Викликаємо процедуру sp_RegisterUser
             CallableStatement stmt = conn.prepareCall("{call sp_RegisterUser(?, ?, ?, ?, ?, ?)}")) {

            // Встановлюємо параметри процедури
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setDate(3, dateOfBirth); // Передаємо java.sql.Date
            stmt.setString(4, phone);     // Телефон (може бути порожнім)
            stmt.setString(5, email);
            // !!! Передаємо ЗВИЧАЙНИЙ пароль, БЕЗ Base64 !!!
            // Процедура sp_RegisterUser сама його хешує
            stmt.setString(6, password);

            // Виконуємо процедуру
            stmt.execute();

            // Якщо процедура виконалась без помилок SQL
            logger.log(Level.INFO, "Новий пацієнт успішно зареєстрований: Email={0}", email);
            JOptionPane.showMessageDialog(this,
                    "Акаунт для " + firstName + " " + lastName + " успішно створено!\nТепер ви можете увійти в систему.",
                    "Реєстрація Успішна",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose(); // Закриваємо вікно реєстрації після успіху

        } catch (SQLException exSQL) {
            // Обробка помилок SQL
            logger.log(Level.SEVERE, "Помилка SQL при реєстрації користувача: " + email, exSQL);
            // Перевіряємо, чи це помилка унікальності Email (дублікат)
            if (exSQL.getMessage().toLowerCase().contains("duplicate key")
                    || exSQL.getMessage().toLowerCase().contains("unique constraint")
                    || exSQL.getMessage().toLowerCase().contains("primary key constraint")) { // Додав перевірку первинного ключа для AppUsers
                JOptionPane.showMessageDialog(this, "Помилка: Email '" + email + "' вже використовується в системі!", "Помилка реєстрації", JOptionPane.ERROR_MESSAGE);
                emailField.requestFocus(); // Фокус на поле Email
            } else {
                // Інша помилка бази даних
                JOptionPane.showMessageDialog(this, "Помилка бази даних під час реєстрації! Зверніться до адміністратора.", "Помилка SQL", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            // Обробка інших можливих винятків (наприклад, помилка з'єднання)
            logger.log(Level.SEVERE, "Неочікувана помилка при реєстрації користувача: " + email, ex);
            JOptionPane.showMessageDialog(this, "Сталася неочікувана помилка під час реєстрації!", "Критична Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}