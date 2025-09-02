package com.dropslot.user.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

  @Bean
  public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
    FilterRegistrationBean<RequestIdFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new RequestIdFilter());
    reg.addUrlPatterns("/*");
    reg.setOrder(Integer.MIN_VALUE);
    return reg;
  }
}
