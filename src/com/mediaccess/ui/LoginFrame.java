package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level; // Імпорт для логування
import java.util.logging.Logger; // Імпорт для логування

public class LoginFrame extends JFrame {

    // Поля для компонентів вводу
    private JTextField emailField;
    private JPasswordField passwordField;

    // Статичний логер для цього класу
    private static final Logger logger = Logger.getLogger(LoginFrame.class.getName());

    // Конструктор фрейму (залишається як у вас)
    public LoginFrame() {
        setTitle("MediAccess - Авторизація");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // запускається у фулскрін
        // setResizable(false); // Розкоментуйте, якщо дійсно хочете ЗАБОРОНИТИ зміну розміру
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Закривати програму при закритті вікна
        setLayout(new BorderLayout()); // Використовуємо BorderLayout

        // Додаємо панель з логотипом зверху
        add(createLogoPanel(), BorderLayout.NORTH);
        // Додаємо панель логіну по центру
        add(createLoginPanel(), BorderLayout.CENTER);

        // Робимо вікно видимим
        setVisible(true);
        logger.log(Level.INFO, "Вікно авторизації LoginFrame створено та відображено.");
    }

    // Метод створення панелі з логотипом (залишається як у вас)
    private JPanel createLogoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(Color.WHITE); // Білий фон

        try {
            // Спробуємо завантажити логотип з ресурсів
            // Переконайтесь, що файл logo.png знаходиться в папці resources/com/mediaccess/ui/
            ImageIcon icon = new ImageIcon(getClass().getResource("/com/mediaccess/ui/logo.png"));
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                JLabel logoLabel = new JLabel(icon);
                panel.add(logoLabel);
                logger.log(Level.INFO, "Логотип успішно завантажено.");
            } else {
                logger.log(Level.WARNING, "Логотип завантажено з помилкою статусу: {0}", icon.getImageLoadStatus());
                panel.add(new JLabel("[Логотип не завантажено]")); // Заглушка
            }
        } catch (NullPointerException npe) {
            logger.log(Level.SEVERE, "Ресурс логотипу не знайдено! Перевірте шлях: /com/mediaccess/ui/logo.png", npe);
            panel.add(new JLabel("[Шлях до логотипу невірний]")); // Заглушка
        }
        catch (Exception e) {
            // Логуємо будь-які інші помилки завантаження лого
            logger.log(Level.SEVERE, "Помилка завантаження логотипу", e);
            panel.add(new JLabel("[Помилка завантаження лого]")); // Заглушка
        }
        return panel;
    }

    // Метод створення панелі логіну (залишається як у вас)
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout()); // Використовуємо GridBagLayout
        panel.setBackground(Color.WHITE); // Білий фон

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Відступи
        gbc.fill = GridBagConstraints.HORIZONTAL; // Розтягування по горизонталі

        // Створюємо компоненти
        JLabel emailLabel = createFixedLabel("Email:");
        emailField = new JTextField(20); // Поле для email
        JLabel passLabel = createFixedLabel("Пароль:");
        passwordField = new JPasswordField(20); // Поле для пароля
        JButton loginButton = new JButton("Увійти");
        JButton forgotPassButton = new JButton("Забули пароль?");
        JButton registerButton = new JButton("Реєстрація");

        // Додаємо слухачів подій до кнопок
        loginButton.addActionListener(e -> loginUser()); // Виклик оновленого методу loginUser
        forgotPassButton.addActionListener(e -> {
            logger.log(Level.INFO, "Користувач натиснув 'Забули пароль'.");
            new ForgotPasswordFrame(); // Відкриваємо вікно відновлення пароля
        });
        registerButton.addActionListener(e -> {
            logger.log(Level.INFO, "Користувач натиснув 'Реєстрація'.");
            new RegisterFrame(); // Відкриваємо вікно реєстрації
        });

        // Розміщуємо компоненти за допомогою GridBagLayout
        int row = 0;
        addRow(panel, gbc, emailLabel, emailField, row++); // Рядок Email
        addRow(panel, gbc, passLabel, passwordField, row++); // Рядок Пароль

        // Кнопка Увійти (займає 2 колонки)
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER; // Центруємо кнопку
        gbc.fill = GridBagConstraints.NONE; // Не розтягувати кнопку
        panel.add(loginButton, gbc);

        // Кнопка Забули пароль
        gbc.gridy = ++row; // Наступний рядок
        panel.add(forgotPassButton, gbc);

        // Кнопка Реєстрація
        gbc.gridy = ++row; // Наступний рядок
        panel.add(registerButton, gbc);

        return panel;
    }

    // Метод створення мітки фіксованої ширини (залишається як у вас)
    private JLabel createFixedLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT); // Вирівнювання тексту праворуч
        label.setPreferredSize(new Dimension(180, 25)); // Задаємо бажаний розмір
        return label;
    }

    // Метод додавання рядка (мітка + поле) (залишається як у вас)
    private void addRow(JPanel panel, GridBagConstraints gbc, JLabel label, JComponent field, int row) {
        // Налаштування для мітки
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        // Налаштування для поля вводу
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    // --- НОВИЙ, ОНОВЛЕНИЙ МЕТОД loginUser ---
    private void loginUser() {
        String email = emailField.getText().trim(); // Отримуємо email, видаляємо пробіли
        String password = new String(passwordField.getPassword()); // Отримуємо пароль

        // Перевірка на порожні поля
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email та пароль не можуть бути порожніми!", "Помилка входу", JOptionPane.WARNING_MESSAGE);
            return; // Виходимо з методу
        }

        logger.log(Level.INFO, "Спроба входу для Email: {0}", email);
        // Викликаємо оновлений метод автентифікації з DatabaseManager
        AuthenticationResult authResult = DatabaseManager.authenticateUser(email, password);

        // Перевіряємо результат автентифікації
        if (authResult != null && authResult.isLoginSuccessful()) {
            // Логін успішний
            String role = authResult.getRole();
            int userId = authResult.getUserId(); // Отримуємо ID користувача

            logger.log(Level.INFO, "Успішний вхід: Email={0}, Role={1}, UserID={2}", new Object[]{email, role, userId});

            // Відкриваємо відповідне вікно залежно від ролі
            switch (role.toLowerCase()) {
                case "dbo":
                    new DBOPanel(); // Відкриваємо панель DBO
                    break;
                case "doctor":
                    if (userId > 0) { // Перевіряємо, чи отримали валідний ID лікаря
                        new DoctorPanel(userId); // Створюємо DoctorPanel, передаючи ID
                    } else {
                        // Якщо ID не отримано (помилка даних в БД)
                        logger.log(Level.WARNING, "Не вдалося отримати валідний DoctorID для Email: {0}", email);
                        JOptionPane.showMessageDialog(this, "Помилка даних: Не знайдено відповідного профілю лікаря для цього акаунту.", "Помилка входу", JOptionPane.ERROR_MESSAGE);
                        return; // Залишаємо вікно логіну
                    }
                    break;
                case "patient":
                    if (userId > 0) { // Перевіряємо, чи отримали валідний ID пацієнта
                        new PatientPanel(userId); // Створюємо PatientPanel, передаючи ID
                    } else {
                        // Якщо ID не отримано (помилка даних в БД)
                        logger.log(Level.WARNING, "Не вдалося отримати валідний PatientID для Email: {0}", email);
                        JOptionPane.showMessageDialog(this, "Помилка даних: Не знайдено відповідного профілю пацієнта для цього акаунту.", "Помилка входу", JOptionPane.ERROR_MESSAGE);
                        return; // Залишаємо вікно логіну
                    }
                    break;
                default:
                    // Якщо роль невідома
                    logger.log(Level.WARNING, "Спроба входу з невідомою роллю: Role={0}, Email={1}", new Object[]{role, email});
                    JOptionPane.showMessageDialog(this, "Невідома роль користувача: " + role, "Помилка входу", JOptionPane.ERROR_MESSAGE);
                    return; // Залишаємо вікно логіну
            }
            // Якщо switch не вийшов через return, значить логін успішний
            dispose(); // Закриваємо вікно логіну

        } else {
            // Якщо логін не вдався (невірний email/пароль або помилка БД)
            logger.log(Level.WARNING, "Невдала спроба входу для Email: {0}", email);
            JOptionPane.showMessageDialog(this, "Невірний email або пароль!", "Помилка входу", JOptionPane.ERROR_MESSAGE);
            // Очищаємо поле пароля для зручності повторного вводу
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }
}