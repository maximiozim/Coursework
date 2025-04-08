package com.mediaccess.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

public class LoginFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(LoginFrame.class.getName());

    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("MediAccess - Авторизація");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
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
            logger.warning("❌ Лого не знайдено! Перевір шлях: /com/mediaccess/ui/logo.png");
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

        JLabel forgotPasswordLabel = createLinkLabel("Забули пароль?", ForgotPasswordFrame::new);
        JLabel createAccountLabel = createLinkLabel("Створити акаунт", RegisterFrame::new);

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

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(forgotPasswordLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
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

        logger.info("Спроба входу для: " + email);

        String role = DatabaseManager.authenticateUserAndGetRole(email, password);
        if (role != null && !role.equalsIgnoreCase("Login Failed")) {
            logger.info("Успішна авторизація з роллю: " + role);
            switch (role.toLowerCase()) {
                case "dbo": new DBOPanel(); break;
                case "admin": new AdminPanel(); break;
                case "doctor": new DoctorPanel(); break;
                case "patient": new PatientPanel(); break;
                default:
                    JOptionPane.showMessageDialog(this, "Невідома роль!", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
            dispose();
        } else {
            logger.warning("❌ Авторизація не вдалася для: " + email);
            JOptionPane.showMessageDialog(this, "Невірний email або пароль!", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
