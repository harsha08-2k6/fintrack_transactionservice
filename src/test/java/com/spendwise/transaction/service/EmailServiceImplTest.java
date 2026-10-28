package com.spendwise.transaction.service;

import com.spendwise.transaction.dto.EmailSendResult;
import com.spendwise.transaction.service.impl.EmailServiceImpl;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.SocketTimeoutException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "sender@gmail.com");
        ReflectionTestUtils.setField(emailService, "fromName", "FinTrack Notifications");
        ReflectionTestUtils.setField(emailService, "smtpEnabled", true);
    }

    private MimeMessage createDummyMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    @DisplayName("When SMTP is disabled, email sending should be skipped and return DISABLED")
    void testDisabledSmtpBehavior() {
        ReflectionTestUtils.setField(emailService, "smtpEnabled", false);

        EmailSendResult result = emailService.sendEmail(
                "user@example.com", "Test User", "Subject", "<p>Html</p>", "Text");

        assertFalse(result.isSuccess());
        assertEquals("DISABLED", result.getStatus());
        assertEquals("DISABLED", result.getErrorCategory());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Invalid or missing recipient email should return INVALID_RECIPIENT immediately")
    void testInvalidRecipientHandling() {
        EmailSendResult resultEmpty = emailService.sendEmail("", "User", "Subject", "Body", "Body");
        assertFalse(resultEmpty.isSuccess());
        assertEquals("INVALID_RECIPIENT", resultEmpty.getErrorCategory());

        EmailSendResult resultNoAt = emailService.sendEmail("invalid_email_string", "User", "Subject", "Body", "Body");
        assertFalse(resultNoAt.isSuccess());
        assertEquals("INVALID_RECIPIENT", resultNoAt.getErrorCategory());

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Successful SMTP delivery returns SENT with attempt count 1")
    void testSuccessfulDelivery() {
        when(mailSender.createMimeMessage()).thenReturn(createDummyMimeMessage());
        doNothing().when(mailSender).send(any(MimeMessage.class));

        EmailSendResult result = emailService.sendEmail(
                "valid@example.com", "Valid User", "Test Alert", "<p>Hello</p>", "Hello");

        assertTrue(result.isSuccess());
        assertEquals("SENT", result.getStatus());
        assertEquals("NONE", result.getErrorCategory());
        assertEquals(1, result.getAttempts());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Authentication failure (535) is categorized as AUTH_FAILURE and does not retry endlessly")
    void testAuthenticationFailureCategorization() {
        when(mailSender.createMimeMessage()).thenReturn(createDummyMimeMessage());
        doThrow(new MailSendException("535 5.7.8 Username and Password not accepted"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailSendResult result = emailService.sendEmail(
                "user@example.com", "User", "Alert", "<p>Hello</p>", "Hello");

        assertFalse(result.isSuccess());
        assertEquals("FAILED", result.getStatus());
        assertEquals("AUTH_FAILURE", result.getErrorCategory());
        assertEquals(1, result.getAttempts());
    }

    @Test
    @DisplayName("Transient timeout retries up to max attempts and categorizes as TIMEOUT")
    void testTimeoutRetryBehavior() {
        when(mailSender.createMimeMessage()).thenReturn(createDummyMimeMessage());
        doThrow(new MailSendException("Read timed out"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailSendResult result = emailService.sendEmail(
                "user@example.com", "User", "Alert", "<p>Hello</p>", "Hello");

        assertFalse(result.isSuccess());
        assertEquals("FAILED", result.getStatus());
        assertEquals("TIMEOUT", result.getErrorCategory());
        assertEquals(3, result.getAttempts());
        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }
}
