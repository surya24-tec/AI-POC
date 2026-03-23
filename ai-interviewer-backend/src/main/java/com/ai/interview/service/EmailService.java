package com.ai.interview.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String fromEmail;

    /**
     * Send onboarding/welcome email to a user when they start a session with an email address.
     */
    public void sendWelcomeEmail(String toEmail) {
        if (mailSender == null || fromEmail == null || fromEmail.isEmpty() || fromEmail.equals("your-email@gmail.com")) {
            System.err.println(">>> Email Service: SKIPPED (Not configured). Configure spring.mail in application.properties to enable sending to " + toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to AI Coach!");
            
            String emailContent = "Dear User,\n\n" +
                    "Welcome to AI Coach!\n\n" +
                    "We are excited to have you on board. AI Coach is designed to help you improve your communication skills and prepare effectively for interviews using intelligent, real-time feedback.\n\n" +
                    "With our platform, you can:\n" +
                    "* Practice interview questions powered by AI\n" +
                    "* Receive instant scores, detailed feedback, and improved answers\n" +
                    "* Track your performance and progress over time\n" +
                    "* Use voice input to practice speaking and enhance communication skills\n\n" +
                    "Our goal is to support your growth and help you build confidence for real-world interviews.\n\n" +
                    "Get started now and take the next step toward your career success!\n\n" +
                    "If you have any questions or need assistance, feel free to reach out to us.\n\n" +
                    "Best regards,\n" +
                    "AI Coach Team";

            message.setText(emailContent);
            mailSender.send(message);
            System.out.println(">>> Email Service: Welcome email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println(">>> Email Service Error sending to " + toEmail + ": " + e.getMessage());
        }
    }
}
