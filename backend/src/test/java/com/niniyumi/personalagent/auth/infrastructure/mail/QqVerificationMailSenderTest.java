package com.niniyumi.personalagent.auth.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.niniyumi.personalagent.auth.application.VerificationMailSendException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(OutputCaptureExtension.class)
class QqVerificationMailSenderTest {
    private static final String FROM = "sender@qq.com";
    private static final String TO = "receiver@example.com";
    private static final String CODE = "123456";

    @Test
    void sendsRegistrationVerificationCopy(CapturedOutput output) {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        QqRegistrationVerificationMailSender sender =
                new QqRegistrationVerificationMailSender(javaMailSender, FROM);

        sender.send(TO, CODE);

        SimpleMailMessage message = capturedMessage(javaMailSender);
        assertThat(message.getFrom()).isEqualTo(FROM);
        assertThat(message.getTo()).containsExactly(TO);
        assertThat(message.getSubject()).isEqualTo("[Personal Agent] 注册验证码");
        assertThat(message.getText()).isEqualTo("""
                您好：

                您正在注册 Personal Agent，验证码为：

                123456

                验证码 10 分钟内有效，请勿转发给他人。
                如果不是您本人操作，请忽略此邮件。

                Personal Agent""");
        assertThat(output).contains("Registration verification email sent")
                .contains("re***@example.com")
                .doesNotContain(TO)
                .doesNotContain(CODE);
    }

    @Test
    void sendsPasswordResetCopy(CapturedOutput output) {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        QqPasswordResetMailSender sender = new QqPasswordResetMailSender(javaMailSender, FROM);

        sender.send(TO, CODE);

        SimpleMailMessage message = capturedMessage(javaMailSender);
        assertThat(message.getFrom()).isEqualTo(FROM);
        assertThat(message.getTo()).containsExactly(TO);
        assertThat(message.getSubject()).isEqualTo("[Personal Agent] 密码重置验证码");
        assertThat(message.getText()).isEqualTo("""
                您好：

                我们收到了您的密码重置请求，验证码为：

                123456

                验证码 10 分钟内有效，请勿转发给他人。
                如果不是您本人操作，请忽略此邮件，您的密码不会被修改。

                Personal Agent""");
        assertThat(output).contains("Password reset email sent")
                .contains("re***@example.com")
                .doesNotContain(TO)
                .doesNotContain(CODE);
    }

    @Test
    void reportsRegistrationMailFailureWithoutLoggingSecrets(CapturedOutput output) {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp unavailable"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));
        QqRegistrationVerificationMailSender sender =
                new QqRegistrationVerificationMailSender(javaMailSender, FROM);

        assertThatThrownBy(() -> sender.send(TO, CODE))
                .isInstanceOf(VerificationMailSendException.class);

        assertThat(output).contains("Registration verification email failed")
                .contains("re***@example.com")
                .doesNotContain(TO)
                .doesNotContain(CODE)
                .doesNotContain("smtp unavailable");
    }

    @Test
    void reportsPasswordResetMailFailureWithoutLoggingSecrets(CapturedOutput output) {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp unavailable"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));
        QqPasswordResetMailSender sender = new QqPasswordResetMailSender(javaMailSender, FROM);

        assertThatThrownBy(() -> sender.send(TO, CODE))
                .isInstanceOf(VerificationMailSendException.class);

        assertThat(output).contains("Password reset email failed")
                .contains("re***@example.com")
                .doesNotContain(TO)
                .doesNotContain(CODE)
                .doesNotContain("smtp unavailable");
    }

    private SimpleMailMessage capturedMessage(JavaMailSender javaMailSender) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        return captor.getValue();
    }
}
