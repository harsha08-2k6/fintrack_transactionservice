package com.spendwise.transaction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.concurrent.Executor;

@Configuration
public class EmailConfig {

    @Value("${fintrack.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${fintrack.mail.port:587}")
    private int port;

    @Value("${fintrack.mail.username:}")
    private String username;

    @Value("${fintrack.mail.password:}")
    private String password;

    @Value("${fintrack.mail.auth:true}")
    private boolean auth;

    @Value("${fintrack.mail.starttls.enable:true}")
    private boolean starttlsEnable;

    @Value("${fintrack.mail.starttls.required:true}")
    private boolean starttlsRequired;

    @Value("${fintrack.mail.timeout-ms:10000}")
    private int timeoutMs;

    @Value("${fintrack.mail.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Value("${fintrack.mail.write-timeout-ms:10000}")
    private int writeTimeoutMs;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username != null ? username.trim() : "");
        mailSender.setPassword(password != null ? password.trim() : "");
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        props.put("mail.smtp.starttls.required", String.valueOf(starttlsRequired));
        props.put("mail.smtp.connectiontimeout", connectionTimeoutMs);
        props.put("mail.smtp.timeout", timeoutMs);
        props.put("mail.smtp.writetimeout", writeTimeoutMs);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        return mailSender;
    }

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("FinTrackEmail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
