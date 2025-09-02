package com.dropslot.user.util;

import org.slf4j.MDC;

public final class LogUtils {
  private static final String USER_ID = "userId";

  private LogUtils() {}

  public static String maskEmail(String email) {
    if (email == null) return null;
    String e = email.trim();
    int at = e.indexOf('@');
    if (at <= 1) return "***";
    String name = e.substring(0, at);
    String domain = e.substring(at + 1);
    String visible = name.substring(0, 1);
    return visible + "***@" + domain;
  }

  public static void putUserContext(String userId) {
    if (userId != null) MDC.put(USER_ID, userId);
  }

  public static void removeUserContext() {
    MDC.remove(USER_ID);
  }
}
