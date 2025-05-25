package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.NoEnoughQuotaException;
import com.raymondweng.newshortlink.exception.TokenNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.sql.SQLException;

@Component
public class TokenFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/create") && !path.startsWith("/create/free")) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                try {
                    LinkManager.useToken(token);
                } catch (NoEnoughQuotaException e) {
                    response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                    return;
                } catch (TokenNotFoundException e) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            filterChain.doFilter(request, response);
        }
    }
}
