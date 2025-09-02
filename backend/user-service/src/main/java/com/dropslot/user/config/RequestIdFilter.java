package com.dropslot.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter extends HttpFilter {

  private static final String REQUEST_ID = "requestId";

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String id = req.getHeader("X-Request-Id");
    if (id == null || id.isEmpty()) {
      id = UUID.randomUUID().toString();
    }
    MDC.put(REQUEST_ID, id);
    try {
      chain.doFilter(req, res);
    } finally {
      MDC.remove(REQUEST_ID);
    }
  }
}
