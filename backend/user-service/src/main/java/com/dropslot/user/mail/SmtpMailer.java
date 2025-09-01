package com.dropslot.user.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mailer.type", havingValue = "smtp", matchIfMissing = false)
public class SmtpMailer implements Mailer {
  private final JavaMailSender sender;

  public SmtpMailer(JavaMailSender sender) {
    this.sender = sender;
  }

  @Override
  public void send(String to, String subject, String body) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(to);
    msg.setSubject(subject);
    msg.setText(body);
    sender.send(msg);
  }
}
