package com.spendwise.transaction.dto;

public class EmailContent {
    private final String subject;
    private final String htmlBody;
    private final String textBody;

    public EmailContent(String subject, String htmlBody, String textBody) {
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.textBody = textBody;
    }

    public String getSubject() { return subject; }
    public String getHtmlBody() { return htmlBody; }
    public String getTextBody() { return textBody; }
}
