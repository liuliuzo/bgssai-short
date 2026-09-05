package com.bgssai.shortai.login.mail;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.login.oauth.OAuthIds;

@Service
public class SmtpMailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);
    private static final String TIMEOUT_MS = "10000";

    private final SmtpSecrets smtpSecrets;

    public SmtpMailSender(SmtpSecrets smtpSecrets) {
        this.smtpSecrets = smtpSecrets;
    }

    public void sendLoginCode(String to, String code) {
        if (OAuthIds.blank(smtpSecrets.host()) || OAuthIds.blank(smtpSecrets.port())) {
            throw new BizException("5204", "邮箱通道暂不可用");
        }
        if (to == null || to.contains("\r") || to.contains("\n")) {
            throw new BizException("2001", "邮箱地址不合法");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpSecrets.host());
        sender.setPort(Integer.parseInt(smtpSecrets.port()));
        if (!OAuthIds.blank(smtpSecrets.username())) {
            sender.setUsername(smtpSecrets.username());
        }
        if (!OAuthIds.blank(smtpSecrets.password())) {
            sender.setPassword(smtpSecrets.password());
        }
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(!OAuthIds.blank(smtpSecrets.username())));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtpSecrets.sslEnable()));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpSecrets.starttlsEnable()));
        props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.put("mail.smtp.timeout", TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", TIMEOUT_MS);

        SimpleMailMessage message = new SimpleMailMessage();
        String from = OAuthIds.blank(smtpSecrets.fromAddress()) ? smtpSecrets.username() : smtpSecrets.fromAddress();
        if (!OAuthIds.blank(from)) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject("登录验证码");
        message.setText("您的登录验证码是 " + code + "，5 分钟内有效。");
        sender.send(message);
        log.info("SMTP 登录验证码已发送 to={}", mask(to));
    }

    static String mask(String email) {
        if (email == null) {
            return "null";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        return (local.length() <= 1 ? "***" : local.charAt(0) + "***") + email.substring(at);
    }
}
