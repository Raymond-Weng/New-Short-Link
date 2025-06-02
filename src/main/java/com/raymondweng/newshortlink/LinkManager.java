package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.NoEnoughQuotaException;
import com.raymondweng.newshortlink.exception.TokenNotFoundException;

import java.sql.*;
import java.util.List;
import java.util.Random;

public class LinkManager {
    public static final List<String> BAN_KEYS = List.of("api", "discord", "create", "free", "contacts", "404");

    public static Connection getDataConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:./database/data.db");
    }

    public static Connection getLinkConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:./database/links.db");
    }

    /**
     * check if the name usable and not used
     *
     * @param link       the new link, write in if not used
     * @param name       the name to create link
     * @param connection connection to database links
     * @return if name usable and written
     * @throws SQLException when sql write goes wrong
     */
    public static synchronized boolean useName(String link, String name, Connection connection) throws SQLException {
        if (BAN_KEYS.contains(name)) {
            return false;
        }
        if (!name.matches("\\w+")) {
            return false;
        }
        if(!link.matches("https?://\\S+")) {
            return false;
        }
        PreparedStatement ps = connection.prepareStatement("SELECT LINK FROM LINKS WHERE KEY = ?");
        ps.setString(1, name);
        ResultSet resultSet = ps.executeQuery();
        boolean exist = resultSet.next();
        resultSet.close();
        ps.close();
        if (exist) {
            return false;
        }
        ps = connection.prepareStatement("INSERT INTO LINKS (KEY, LINK) VALUES (?, ?)");
        ps.setString(1, name);
        ps.setString(2, link);
        ps.execute();
        ps.close();
        return true;
    }

    /**
     * check if the name usable and not used
     *
     * @param link the new link, write in if not used
     * @param name the name to create link
     * @return if name usable and written
     * @throws SQLException when sql write goes wrong
     */
    public static boolean useName(String link, String name) throws SQLException {
        Connection connection = getLinkConnection();
        boolean res = useName(link, name, connection);
        connection.close();
        return res;
    }

    /**
     * get a random link without checking its availability
     *
     * @param connection connection to data database
     * @return a random name
     * @throws SQLException if something wrong while writing database
     */
    public synchronized static String getLink(Connection connection) throws SQLException {
        // fill unused keys
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM KEYS");
        resultSet.next();
        int cnt = resultSet.getInt(1);
        resultSet.close();
        statement.close();
        if (cnt < 100) {
            Thread thread = new Thread(new RefillKeys());
            thread.start();
        }
        statement = connection.createStatement();
        resultSet = statement.executeQuery("SELECT KEY FROM (SELECT * FROM KEYS ORDER BY ID LIMIT " + new Random().nextInt(cnt - 1) + ") ORDER BY ID DESC LIMIT 1");
        String res = resultSet.getString(1);
        resultSet.close();
        statement.close();
        statement = connection.createStatement();
        statement.execute("DELETE FROM KEYS WHERE KEY = \"" + res + "\"");
        statement.close();
        connection.close();
        return res;
    }

    /**
     * get a random link without checking its availability
     *
     * @return a random name
     * @throws SQLException if something wrong while writing database
     */
    public static String getLink() throws SQLException {
        Connection connection = getDataConnection();
        String res = getLink(connection);
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

    public static String getURL(String key) throws SQLException {
        Connection connection = getLinkConnection();
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