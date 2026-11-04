package com.fullmall.platform.channel.sender;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * SMTP 邮件通道（Jakarta Mail 2.1，Spring Boot 的 starter-mail 提供实现）。
 *
 * <p>126 / QQ 是预设：运营商只填「发件邮箱 + 授权码」，host/port/加密方式写死在下面——
 * 让运营手填 smtp.126.com:465 只会填错。{@code smtp} 预设保留给自建服务器。
 *
 * <p>126/QQ 的「授权码」不是登录密码：填登录密码会得到 535 认证失败，这是最常见的报障原因。
 */
public class SmtpEmailChannel implements EmailChannel {

    private final String host;
    private final int port;
    private final boolean ssl;
    private final String username;
    private final String password;
    private final String fromName;

    public SmtpEmailChannel(String provider, ChannelSettings settings) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "126" -> {
                this.host = "smtp.126.com";
                this.port = 465;
                this.ssl = true;
            }
            case "qq" -> {
                this.host = "smtp.qq.com";
                this.port = 465;
                this.ssl = true;
            }
            case "smtp" -> {
                this.host = settings.required("host", "SMTP 服务器地址");
                this.port = parsePort(settings.required("port", "SMTP 端口"));
                this.ssl = this.port == 465;
            }
            default -> throw new IllegalStateException("不支持的邮件供应商：" + provider);
        }
        this.username = settings.required("username", "发件邮箱");
        this.password = settings.required("password", "授权码");
        this.fromName = settings.optional("from-name", username);
    }

    @Override
    public void send(String to, String subject, String body) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        if (ssl) {
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
        } else {
            properties.put("mail.smtp.starttls.enable", "true");
        }
        Session session = createSession(properties);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, fromName, StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject == null ? "" : subject, StandardCharsets.UTF_8.name());
            message.setContent(body == null ? "" : body, "text/plain; charset=UTF-8");
            Transport.send(message);
        } catch (MessagingException error) {
            throw new IllegalStateException("发送邮件失败（" + host + "）：" + error.getMessage(), error);
        } catch (java.io.UnsupportedEncodingException error) {
            throw new IllegalStateException("发件人名称编码失败：" + fromName, error);
        }
    }

    private Session createSession(Properties properties) {
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        return Session.getInstance(properties, authenticator);
    }

    private int parsePort(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0 || parsed > 65535) {
                throw new IllegalStateException("SMTP 端口超出范围：" + value);
            }
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalStateException("SMTP 端口必须是数字，实际=" + value, error);
        }
    }
}
