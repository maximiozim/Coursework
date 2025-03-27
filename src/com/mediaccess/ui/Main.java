package com.mediaccess.ui;

import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        if (DatabaseManager.testConnection()) {
            SwingUtilities.invokeLater(LoginFrame::new);
        } else {
            JOptionPane.showMessageDialog(null, "Помилка підключення до бази даних!", "Помилка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}

// 📌 Вікно авторизації
class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("MediAccess - Авторизація");
        setExtendedState(JFrame.MAXIMIZED_BOTH);  // 🟢 Відновлено повноекранний режим
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createLogoPanel(), BorderLayout.NORTH);
        add(createLoginPanel(), BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel createLogoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(Color.WHITE);

        try {
            ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("com/mediaccess/ui/logo.png"));
            JLabel logoLabel = new JLabel(icon);
            panel.add(logoLabel);
        } catch (Exception e) {
            System.err.println("❌ Лого не знайдено! Перевір шлях: /com/mediaccess/ui/logo.png");
        }

        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel emailLabel = new JLabel("Email:");
        emailField = new JTextField(20);
        JLabel passLabel = new JLabel("Пароль:");
        passwordField = new JPasswordField(20);

        JButton loginButton = new JButton("Увійти");
        loginButton.setBackground(Color.decode("#4CAF50"));
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(e -> loginUser());

        JLabel forgotPasswordLabel = createLinkLabel("Забули пароль?", () -> new ForgotPasswordFrame());
        JLabel createAccountLabel = createLinkLabel("Створити акаунт", () -> new RegisterFrame());

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(emailLabel, gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(passLabel, gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(forgotPasswordLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(createAccountLabel, gbc);

        return panel;
    }

    private JLabel createLinkLabel(String text, Runnable action) {
        JLabel label = new JLabel("<HTML><U>" + text + "</U></HTML>");
        label.setForeground(Color.BLUE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return label;
    }

    private void loginUser() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        String role = DatabaseManager.authenticateUserAndGetRole(email, password);
        if (role != null && !role.equalsIgnoreCase("Login Failed")) {
            switch (role.toLowerCase()) {
                case "dbo": new AdminPanel(); break;
                case "admin": new AdminPanel(); break;
                case "doctor": new DoctorPanel(); break;
                case "patient": new PatientPanel(); break;
                default:
                    JOptionPane.showMessageDialog(this, "Невідома роль!", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Невірний email або пароль!", "Помилка", JOptionPane.ERROR_MESSAGE);
        }

    }

}

// 📌 Оновлений менеджер бази даних
class DatabaseManager {
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

    static String authenticateUserAndGetRole(String email, String password) {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_LoginUser(?, ?)}")) {
            stmt.setString(1, email);
            stmt.setString(2, password);
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



    private static boolean isInTable(Connection conn, String tableName, String email) throws SQLException {
        String query = "SELECT 1 FROM " + tableName + " WHERE Email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }


    static String requestPasswordReset(String email) {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("{call sp_RequestPasswordReset(?)}")) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery(); // обов'язково SELECT у процедурі

            if (rs.next()) {
                return rs.getString("ResetCode"); // name в SELECT: AS ResetCode
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


    // 🟢 Поля для пошти SMTP
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
            message.setSubject("Код відновлення пароля");
            message.setText("Ваш код для відновлення пароля: " + code);
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
