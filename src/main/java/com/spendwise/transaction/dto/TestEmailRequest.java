package com.spendwise.transaction.dto;

public class TestEmailRequest {
    private String customNote;

    public TestEmailRequest() {}

    public TestEmailRequest(String customNote) {
        this.customNote = customNote;
    }

    public String getCustomNote() { return customNote; }
    public void setCustomNote(String customNote) { this.customNote = customNote; }
}
