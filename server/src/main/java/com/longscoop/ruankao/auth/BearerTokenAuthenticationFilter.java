package com.longscoop.ruankao.auth;

import com.longscoop.ruankao.auth.persistence.UserAccountMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSessionService sessionService;
    private final UserAccountMapper userAccountMapper;

    public BearerTokenAuthenticationFilter(
            AuthSessionService sessionService,
            UserAccountMapper userAccountMapper) {
        this.sessionService = sessionService;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = header.substring(7).trim();
                sessionService.resolve(token).ifPresent(userId -> {
                    var user = userAccountMapper.selectById(userId);
                    String role = user == null || user.getRole() == null || user.getRole().isBlank()
                            ? "USER"
                            : user.getRole().trim().toUpperCase();
                    var authentication = new UsernamePasswordAuthenticationToken(
                            new RuankaoPrincipal(userId),
                            "n/a",
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
