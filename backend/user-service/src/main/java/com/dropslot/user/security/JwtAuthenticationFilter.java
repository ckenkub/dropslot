package com.dropslot.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        if (jwtService.isTokenValid(token)) {
          String sub = jwtService.extractSubject(token);
          List<String> roles =
              Optional.ofNullable(jwtService.extractRoles(token)).orElse(List.of());
          var authorities =
              roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
          var auth = new UsernamePasswordAuthenticationToken(sub, null, authorities);
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (Exception ignored) {
        // invalid token -> proceed unauthenticated
      }
    }
    filterChain.doFilter(request, response);
  }
}
