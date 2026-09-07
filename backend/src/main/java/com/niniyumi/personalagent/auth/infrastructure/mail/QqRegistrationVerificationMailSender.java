package com.niniyumi.personalagent.auth.infrastructure.mail;

import com.niniyumi.personalagent.auth.application.RegistrationVerificationMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class QqRegistrationVerificationMailSender implements RegistrationVerificationMailSender {
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
        message.setSubject("Personal Agent 注册验证码");
        message.setText("你的注册验证码是：" + code + "\n验证码 10 分钟内有效。");
        mailSender.send(message);
    }
}
