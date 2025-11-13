package com.raymondweng.newshortlink;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

@Component
public class TokenFilter extends OncePerRequestFilter {
    public static synchronized void initUser(Connection connection, String user) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT OWNER FROM TOKENS WHERE OWNER = ?");
        preparedStatement.setString(1, user);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            resultSet.close();
            preparedStatement.close();
            return;
        }
        resultSet.close();
        preparedStatement.close();
        preparedStatement = connection.prepareStatement("INSERT INTO TOKENS(OWNER) VALUES (?)");
        preparedStatement.setString(1, user);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static synchronized String getToken(String user) throws SQLException {
        Connection connection = LinkManager.hikariDataSource.getConnection();
        initUser(connection, user);
        String token = null;
        while (token == null) {
            SecureRandom random = new SecureRandom();
            byte[] tokenBytes = new byte[32];
            random.nextBytes(tokenBytes);
            token = Base64.getEncoder().withoutPadding().encodeToString(tokenBytes);
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT TOKEN FROM TOKENS WHERE TOKEN = ?");
            preparedStatement.setString(1, token);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                token = null;
            }
            resultSet.close();
            preparedStatement.close();
        }
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE TOKENS SET TOKEN = ? WHERE OWNER = ?");
        preparedStatement.setString(1, token);
        preparedStatement.setString(2, user);
        preparedStatement.executeUpdate();
        preparedStatement.close();
        connection.close();
        return token;
    }

    public static synchronized boolean useQuota(Connection connection, String user) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT QUOTA FROM TOKENS WHERE OWNER = ?");
        preparedStatement.setString(1, user);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int quota = resultSet.getInt(1);
        resultSet.close();
        preparedStatement.close();
        if (quota > 0) {
            preparedStatement = connection.prepareStatement("UPDATE TOKENS SET QUOTA = QUOTA - 1 WHERE OWNER = ?");
            preparedStatement.setString(1, user);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/create") && !path.startsWith("/create/free")) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                try {
                    Connection connection = LinkManager.hikariDataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement("SELECT OWNER FROM TOKENS WHERE TOKEN = ?");
                    preparedStatement.setString(1, token);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        String user = resultSet.getString("OWNER");
                        resultSet.close();
                        preparedStatement.close();
                        if (useQuota(connection, user)) {
                            response.setStatus(HttpServletResponse.SC_OK);
                        } else {
                            response.setStatus(429);
                            response.getWriter().write("Out of quota. Please try again later.");
                            return;
                        }
                    } else {
                        resultSet.close();
                        preparedStatement.close();
                        connection.close();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Token not found. Please try again later.");
                        return;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized. Please try again later.");
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            filterChain.doFilter(request, response);
        }
    }
}
