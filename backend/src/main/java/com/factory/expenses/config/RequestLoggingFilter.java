package com.factory.expenses.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs one INFO line per request: method, path, status, duration. Never logs bodies or query. */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long start = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      long millis = (System.nanoTime() - start) / 1_000_000;
      LOG.info(
          "{} {} -> {} in {} ms",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          millis);
    }
  }
}
