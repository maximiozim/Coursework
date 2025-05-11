package com.mediaccess.ui;

import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.sql.*; // Змінено імпорт для використання java.sql.*
import java.util.Properties;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
// import java.util.Base64; // Base64 більше не потрібен для паролів

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    // Переконайтесь, що URL правильний для вашого середовища
    static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=MediAccess;integratedSecurity=true;encrypt=false;trustServerCertificate=true";
    static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка з'єднання з БД", e);
            return false;
        }
    }

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Метод хешування залишається, якщо він потрібен деінде,
    // але для логіну/реєстрації хешування робить SP
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Переконуємось, що використовуємо UTF-8 для консистентності
            byte[] hash = md.digest(password.getBytes("UTF-8"));

            // Конвертуємо байти в шістнадцятковий рядок у ВЕРХНЬОМУ РЕГІСТРІ
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = String.format("%02X", b); // %X - великі літери
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) { // Ловимо ширший Exception для NoSuchAlgorithmException та UnsupportedEncodingException
            logger.log(Level.SEVERE, "Помилка хешування пароля", e);
            // У реальному додатку краще кидати більш специфічний виняток або обробляти інакше
            throw new RuntimeException("Помилка хешування пароля", e);
        }
    }

    // --- Оновлений метод автентифікації ---
    static AuthenticationResult authenticateUser(String email, String password) {
        // Викликаємо оновлену процедуру sp_LoginUser
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_LoginUser(?, ?)}")) {

            stmt.setString(1, email);
            // !!! Передаємо ЗВИЧАЙНИЙ пароль, БЕЗ Base64 !!!
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("Role");
                // Отримуємо UserID, який повернула процедура
                int userId = rs.getInt("UserID");
                // Повертаємо об'єкт з результатом
                return new AuthenticationResult(role, userId);
            }
        } catch (SQLException e) {
            // Логуємо помилку
            logger.log(Level.SEVERE, "Помилка авторизації для email: " + email, e);
        }
        // Повертаємо неуспішний результат, якщо сталася помилка або запис не знайдено
        return new AuthenticationResult("Login Failed", -1);
    }

    // --- Методи для скидання пароля (залишаються без змін, бо SP очікують звичайні дані) ---
    static void createConfirmationCode(String email, String code) {
        try (Connection conn = getConnection()) {
            // Спочатку видаляємо попередній код
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM PasswordResetRequests WHERE Email = ?")) {
                deleteStmt.setString(1, email);
                deleteStmt.executeUpdate();
            }
            // Потім вставляємо новий
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO PasswordResetRequests (Email, ResetCode, ExpiryTime) " +
                            "VALUES (?, ?, DATEADD(MINUTE, 15, GETDATE()))")) {
                insertStmt.setString(1, email);
                insertStmt.setString(2, code);
                insertStmt.executeUpdate();
            }
            System.out.println("✅ Код підтвердження створено для " + email);
        } catch (SQLException e) {
            logger.log(Level.SEVERE,"Не вдалося створити код підтвердження для email: " + email, e);
            JOptionPane.showMessageDialog(null, "❌ Не вдалося створити код підтвердження", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    static boolean validateConfirmationCode(String email, String code) {
        String sql = "SELECT 1 FROM PasswordResetRequests WHERE Email = ? AND ResetCode = ? AND ExpiryTime > GETDATE()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, code);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Повертає true, якщо знайдено валідний код
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Помилка валідації коду або код не знайдено/прострочено для email: " + email, e);
            return false;
        }
    }

    static String requestPasswordReset(String email) {
        // Викликаємо процедуру sp_RequestPasswordReset
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_RequestPasswordReset(?)}")) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Повертаємо згенерований код
                return rs.getString("ResetCode");
            }
        } catch (SQLException e) {
            // Логуємо помилку, якщо процедура повернула помилку (напр., email не знайдено)
            logger.log(Level.WARNING, "Помилка запиту на скидання пароля для email: " + email, e);
            // Можна отримати повідомлення про помилку з SQLException, якщо потрібно
            // JOptionPane.showMessageDialog(null, "Помилка БД: " + e.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
        }
        return null; // Повертаємо null, якщо код не вдалося згенерувати
    }

    static boolean resetPassword(String email, String code, String newPassword, String confirmPassword) {
        // Перевірка на співпадіння паролів (хоча вона є і в SP, можна зробити і тут)
        if (!newPassword.equals(confirmPassword)) {
            logger.log(Level.WARNING,"Спроба скидання пароля: паролі не співпадають для email: " + email);
            // Можна повернути false або кинути виняток
            return false;
        }
        // Викликаємо процедуру sp_ResetPassword
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_ResetPassword(?, ?, ?, ?)}")) {
            stmt.setString(1, email);
            stmt.setString(2, code);
            // Передаємо звичайний новий пароль
            stmt.setString(3, newPassword);
            stmt.setString(4, confirmPassword);
            stmt.execute(); // Виконуємо процедуру
            // Якщо процедура не повернула помилку (RAISERROR), вважаємо успіхом
            return true;
        } catch (SQLException e) {
            // Логуємо помилку, якщо процедура повернула помилку
            logger.log(Level.SEVERE, "Помилка скидання пароля для email: " + email, e);
            // Показуємо користувачу повідомлення про помилку
            JOptionPane.showMessageDialog(null, "Помилка скидання пароля: " + e.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // --- Метод для надсилання Email (залишається без змін) ---
    private static final String SENDER_EMAIL = "mediaccess6@gmail.com"; // Перевірте актуальність
    private static final String SENDER_PASSWORD = "wqjq wtfv mxxx pajx"; // Зберігайте пароль безпечніше!

    static void sendEmail(String recipient, String code) {
        // Налаштування властивостей для Gmail
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587"); // Порт TLS
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // Увімкнути TLS

        // Створення сесії з автентифікатором
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });
        session.setDebug(false); // Встановіть true для детального логування надсилання

        try {
            // Створення повідомлення
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("MediAccess: Код підтвердження"); // Тема листа
            message.setText("Ваш код для скидання/підтвердження пароля: " + code + "\nКод дійсний 15 хвилин."); // Текст листа

            // Надсилання повідомлення
            Transport.send(message);
            logger.log(Level.INFO, "Email з кодом успішно надіслано на: " + recipient);

        } catch (MessagingException e) {
            // Логування помилки надсилання
            logger.log(Level.SEVERE, "Помилка надсилання email на: " + recipient, e);
            // Повідомлення користувачу (можливо, не тут, а в місці виклику)
            // JOptionPane.showMessageDialog(null, "Не вдалося надіслати email з кодом.", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// --- Клас для зберігання результату автентифікації ---
// Можна винести в окремий файл AuthenticationResult.java
class AuthenticationResult {
    private final String role;
    private final int userId; // PatientID або DoctorID, або 0/-1 для інших

    public AuthenticationResult(String role, int userId) {
        this.role = role;
        this.userId = userId;
    }

    public String getRole() { return role; }
    public int getUserId() { return userId; }

    // Метод для перевірки, чи логін був успішним
    public boolean isLoginSuccessful() {
        // Перевіряємо чи роль не null і не містить помилку
        return role != null && !role.equalsIgnoreCase("Login Failed");
    }
}