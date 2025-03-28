package com.mediaccess.ui;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.util.Properties;
import java.security.MessageDigest;

public class DatabaseManager {
    static final String URL = "jdbc:sqlserver://192.168.0.207:1433;databaseName=MediAccess;integratedSecurity=true;encrypt=false;trustServerCertificate=true";

    static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return true;
        } catch (SQLException e) {
            System.err.println("❌ DB ERROR: " + e.getMessage());
            return false;
        }
    }

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Помилка хешування пароля", e);
        }
    }

    static String authenticateUserAndGetRole(String email, String password) {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_LoginUser(?, ?)}")) {
            String hashedPassword = hashPassword(password);
            stmt.setString(1, email);
            stmt.setString(2, hashedPassword);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("Role");
                if (role != null && !role.equalsIgnoreCase("Login Failed")) {
                    return role;
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void createConfirmationCode(String email, String code) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM PasswordResetRequests WHERE Email = ?;" +
                             "INSERT INTO PasswordResetRequests (Email, ResetCode, ExpiryTime) VALUES (?, ?, DATEADD(MINUTE, 15, GETDATE()));")) {
            stmt.setString(1, email);
            stmt.setString(2, email);
            stmt.setString(3, code);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static boolean validateConfirmationCode(String email, String code) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM PasswordResetRequests WHERE Email = ? AND ResetCode = ? AND ExpiryTime > GETDATE()")) {
            stmt.setString(1, email);
            stmt.setString(2, code);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static String requestPasswordReset(String email) {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_RequestPasswordReset(?)}")) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("ResetCode");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static boolean resetPassword(String email, String code, String newPassword, String confirmPassword) {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_ResetPassword(?, ?, ?, ?)}")) {
            stmt.setString(1, email);
            stmt.setString(2, code);
            stmt.setString(3, newPassword);
            stmt.setString(4, confirmPassword);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static final String SENDER_EMAIL = "mediaccess6@gmail.com";
    private static final String SENDER_PASSWORD = "wqjq wtfv mxxx pajx";

    static void sendEmail(String recipient, String code) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("Код підтвердження Email");
            message.setText("Ваш код підтвердження: " + code);
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
