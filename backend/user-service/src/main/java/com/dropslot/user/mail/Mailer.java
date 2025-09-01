package com.dropslot.user.mail;

public interface Mailer {
  void send(String to, String subject, String body);
}
