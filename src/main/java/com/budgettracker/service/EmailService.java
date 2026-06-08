package com.budgettracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Budget Tracker");
            helper.setTo(toEmail);
            helper.setSubject("🔐 Reset Your Password — Budget Tracker");

            String resetLink = frontendUrl + "/index.html?reset=" + resetToken;
            String htmlContent = buildPasswordResetEmail(fullName, resetLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendBudgetAlertEmail(String toEmail, String fullName, String categoryName,
                                      double percentageUsed, double limitAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Budget Tracker");
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Budget Alert — " + categoryName);

            String htmlContent = buildBudgetAlertEmail(fullName, categoryName, percentageUsed, limitAmount);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Budget alert email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send budget alert email: {}", e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Budget Tracker");
            helper.setTo(toEmail);
            helper.setSubject("🎉 Welcome to Budget Tracker!");

            String htmlContent = buildWelcomeEmail(fullName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    private String buildPasswordResetEmail(String fullName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Arial,sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:linear-gradient(135deg,#1e1b4b,#312e81);border-radius:20px;overflow:hidden;box-shadow:0 25px 50px rgba(0,0,0,0.5);">
                    <div style="background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:40px;text-align:center;">
                      <h1 style="color:white;margin:0;font-size:28px;letter-spacing:-0.5px;">💰 Budget Tracker</h1>
                      <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;">Your Personal Finance Companion</p>
                    </div>
                    <div style="padding:40px;">
                      <h2 style="color:white;margin:0 0 16px;">Hi %s,</h2>
                      <p style="color:#a5b4fc;line-height:1.7;margin:0 0 24px;">We received a request to reset your password. Click the button below to create a new password. This link will expire in <strong style="color:#f59e0b;">1 hour</strong>.</p>
                      <div style="text-align:center;margin:32px 0;">
                        <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#6366f1,#8b5cf6);color:white;text-decoration:none;padding:16px 40px;border-radius:12px;font-size:16px;font-weight:600;letter-spacing:0.5px;">Reset My Password</a>
                      </div>
                      <p style="color:#64748b;font-size:13px;line-height:1.6;">If you didn't request a password reset, please ignore this email. Your password will remain unchanged.</p>
                      <hr style="border:none;border-top:1px solid rgba(255,255,255,0.1);margin:24px 0;">
                      <p style="color:#64748b;font-size:12px;text-align:center;">This link expires in 1 hour &bull; Budget Tracker Security Team</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName, resetLink);
    }

    private String buildBudgetAlertEmail(String fullName, String categoryName, double pct, double limit) {
        String color = pct >= 100 ? "#ef4444" : "#f59e0b";
        String emoji = pct >= 100 ? "🚨" : "⚠️";
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Arial,sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:linear-gradient(135deg,#1e1b4b,#312e81);border-radius:20px;overflow:hidden;">
                    <div style="background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:32px;text-align:center;">
                      <h1 style="color:white;margin:0;">%s Budget Alert</h1>
                    </div>
                    <div style="padding:40px;">
                      <h2 style="color:white;">Hi %s,</h2>
                      <p style="color:#a5b4fc;">Your <strong style="color:%s;">%s</strong> budget has reached <strong style="color:%s;">%.0f%%</strong> of your ₹%.2f limit.</p>
                      <div style="background:rgba(255,255,255,0.05);border-radius:12px;padding:20px;margin:24px 0;">
                        <div style="background:#1e293b;border-radius:8px;height:12px;overflow:hidden;">
                          <div style="background:%s;width:%.0f%%;height:100%%;border-radius:8px;"></div>
                        </div>
                        <p style="color:#64748b;text-align:center;margin:12px 0 0;">%.0f%% used</p>
                      </div>
                      <p style="color:#a5b4fc;">Consider reviewing your spending in this category.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(emoji, fullName, color, categoryName, color, pct, limit, color, Math.min(pct, 100), pct);
    }

    private String buildWelcomeEmail(String fullName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Arial,sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:linear-gradient(135deg,#1e1b4b,#312e81);border-radius:20px;overflow:hidden;">
                    <div style="background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:40px;text-align:center;">
                      <h1 style="color:white;margin:0;font-size:32px;">🎉 Welcome!</h1>
                    </div>
                    <div style="padding:40px;">
                      <h2 style="color:white;">Hi %s,</h2>
                      <p style="color:#a5b4fc;line-height:1.7;">Welcome to <strong style="color:#818cf8;">Budget Tracker</strong>! Your account is ready. Start managing your finances smarter today.</p>
                      <ul style="color:#a5b4fc;line-height:2;">
                        <li>✅ Track income &amp; expenses</li>
                        <li>📊 View beautiful analytics charts</li>
                        <li>🎯 Set savings goals</li>
                        <li>🔔 Get budget alerts</li>
                        <li>📱 Works on all devices</li>
                      </ul>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName);
    }
}
