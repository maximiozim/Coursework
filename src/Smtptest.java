import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Smtptest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("mediaccess6@gmail.com", "wqjq wtfv mxxx pajx ");
            }
        });

        try {
            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", "mediaccess6@gmail.com", "wqjq wtfv mxxx pajx ");
            System.out.println("✅ З'єднання успішне!");
            transport.close();
        } catch (MessagingException e) {
            System.out.println("❌ Помилка: " + e.getMessage());
        }
    }
}
