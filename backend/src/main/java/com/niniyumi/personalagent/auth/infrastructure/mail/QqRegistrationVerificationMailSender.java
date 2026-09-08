package com.niniyumi.personalagent.auth.infrastructure.mail;

import com.niniyumi.personalagent.auth.application.RegistrationVerificationMailSender;
import com.niniyumi.personalagent.auth.application.VerificationMailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class QqRegistrationVerificationMailSender implements RegistrationVerificationMailSender {
    private static final Logger log = LoggerFactory.getLogger(QqRegistrationVerificationMailSender.class);
    private final JavaMailSender mailSender;
    private final String from;

    public QqRegistrationVerificationMailSender(JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[Personal Agent] 注册验证码");
        message.setText("""
                您好：

                您正在注册 Personal Agent，验证码为：

                %s

                验证码 10 分钟内有效，请勿转发给他人。
                如果不是您本人操作，请忽略此邮件。

                Personal Agent""".formatted(code));
        String maskedEmail = mask(email);
        log.info("Sending registration verification email, recipient={}", maskedEmail);
        try {
            mailSender.send(message);
            log.info("Registration verification email sent, recipient={}", maskedEmail);
        } catch (MailException exception) {
            log.warn("Registration verification email failed, recipient={}", maskedEmail);
            throw new VerificationMailSendException(exception);
        }
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
}
