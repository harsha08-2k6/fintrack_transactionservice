package com.spendwise.transaction.service;

import com.spendwise.transaction.dto.EmailContent;
import com.spendwise.transaction.dto.EmailSendResult;

public interface EmailService {

    boolean isSmtpEnabled();

    EmailSendResult sendEmail(String toEmail, String recipientName, EmailContent content);

    EmailSendResult sendEmail(String toEmail, String recipientName, String subject, String htmlBody, String textBody);
}
