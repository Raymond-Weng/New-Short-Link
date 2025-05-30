package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.NoEnoughQuotaException;
import com.raymondweng.newshortlink.exception.TokenNotFoundException;

import java.sql.*;
import java.util.List;
import java.util.Random;

public class LinkManager {
    public static final List<String> BAN_KEYS = List.of("api", "discord", "create", "random");

    public static String getLink() throws SQLException {
        // fill unused keys
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/data.db");
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM KEYS");
        resultSet.next();
        int cnt = resultSet.getInt(1);
        resultSet.close();
        statement.close();
        if (cnt < 50) {
            Thread thread = new Thread(new RefillKeys());
            thread.start();
            cnt += 100;
        }
        statement = connection.createStatement();
        resultSet = statement.executeQuery("SELECT * FROM (SELECT * FROM KEYS ORDER BY ID LIMIT " + new Random().nextInt(cnt - 1) + ") ORDER BY ID DESC LIMIT 1");
        String res = resultSet.getString(1);
        resultSet.close();
        statement.close();
        statement = connection.createStatement();
        statement.execute("DELETE FROM KEYS WHERE KEY = \"" + res + "\"");
        statement.close();
        if (isLinkExist(connection, res)) {
            connection.close();
            return getLink();
        }
        connection.close();
        return res;
    }

    public static void useToken(String token) throws NoEnoughQuotaException, TokenNotFoundException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/data.db");
        PreparedStatement statement = connection.prepareStatement("SELECT QUOTA FROM TOKENS WHERE TOKEN = ?");
        statement.setString(1, token);
        ResultSet resultSet = statement.executeQuery();
        boolean hasQuotaLimit;
        if (resultSet.next()) {
            hasQuotaLimit = resultSet.getInt(1) == -1;
            if (hasQuotaLimit && resultSet.getInt(1) == 0) {
                resultSet.close();
                statement.close();
                connection.close();
                throw new NoEnoughQuotaException();
            }
        } else {
            resultSet.close();
            statement.close();
            connection.close();
            throw new TokenNotFoundException();
        }
        resultSet.close();
        statement.close();
        if (hasQuotaLimit) {
            statement = connection.prepareStatement("UPDATE TOKENS SET QUOTA = QUOTA - 1 WHERE TOKEN = ?");
            statement.setString(1, token);
            statement.executeUpdate();
            statement.close();
        }
        connection.close();
    }

    public static boolean isLinkExist(Connection connection, String key) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS WHERE KEY = ?");
        statement.setString(1, key);
        ResultSet resultSet = statement.executeQuery();
        boolean res = resultSet.next();
        resultSet.close();
        statement.close();
        return res;
    }

    public static boolean isLinkExist(String key) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/links.db");
        boolean res = isLinkExist(connection, key);
        connection.close();
        return res;
    }

    public static String getURL(String key) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/links.db");
        PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS WHERE KEY = ?");
        statement.setString(1, key);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            String s = resultSet.getString("LINK");
            resultSet.close();
            statement.close();
            connection.close();
            return s;
        } else {
            resultSet.close();
            statement.close();
            connection.close();
            return null;
        }
    }
}