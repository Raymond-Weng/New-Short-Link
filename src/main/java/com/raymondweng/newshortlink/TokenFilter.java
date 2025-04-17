package com.raymondweng.newshortlink;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/create") && !path.startsWith("/create/free")) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                try {
                    int type;
                    switch (path) {
                        case "/create/three_months":
                            type = LinkManager.THREE_MONTH_LINK;
                            break;
                        case "/create/no_expiration":
                            type = LinkManager.NO_EXPIRATION_LINK;
                            break;
                        default:
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            return;
                    }
                }catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
