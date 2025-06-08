package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.NoEnoughQuotaException;
import com.raymondweng.newshortlink.exception.TokenNotFoundException;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.List;
import java.util.Random;

class Pair<K, V> {
    K key;
    V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}

public class LinkManager {
    public static final List<String> BAN_KEYS = List.of("api", "discord", "create", "free", "contacts", "404");

    public static Connection getLinkConnection() throws SQLException {
        Dotenv dotenv = Dotenv.configure().directory("./env").load();
        return DriverManager.getConnection("jdbc:mysql://" + dotenv.get("DB_ADDRESS") + "/mydb?serverTimezone=Asia/Taipei", dotenv.get("DB_ACC"), dotenv.get("DB_PASS"));
    }

    public static Connection getConnection() throws SQLException {
        Dotenv dotenv = Dotenv.configure().directory("./env").load();
        return DriverManager.getConnection("jdbc:mysql://" + dotenv.get("DB_ADDRESS") + "/mydb?serverTimezone=Asia/Taipei", dotenv.get("DB_ACC"), dotenv.get("DB_PASS"));
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
    public static synchronized boolean useName(String link, String name, Connection connection, boolean preventPreview) throws SQLException {
        if (BAN_KEYS.contains(name)) {
            return false;
        }
        if (!name.matches("\\w+.\\w")) {
            return false;
        }
        if (!link.matches("https?://\\S+")) {
            return false;
        }
        PreparedStatement ps = connection.prepareStatement("SELECT LINK FROM LINKS WHERE NAME = ?");
        ps.setString(1, name);
        ResultSet resultSet = ps.executeQuery();
        boolean exist = resultSet.next();
        resultSet.close();
        ps.close();
        if (exist) {
            return false;
        }
        ps = connection.prepareStatement("INSERT INTO LINKS (NAME, LINK, PREVIEW_PREVENT) VALUES (?, ?, ?)");
        ps.setString(1, name);
        ps.setString(2, link);
        ps.setBoolean(3, preventPreview);
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
    public static boolean useName(String link, String name, boolean preventPreview) throws SQLException {
        Connection connection = getConnection();
        boolean res = useName(link, name, connection, preventPreview);
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
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM NAMES");
        resultSet.next();
        int cnt = resultSet.getInt(1);
        resultSet.close();
        statement.close();
        if (cnt < 100) {
            Thread.startVirtualThread(new RefillKeys());
        }
        statement = connection.createStatement();
        resultSet = statement.executeQuery("SELECT NAME FROM (SELECT * FROM NAMES ORDER BY ID LIMIT " + new Random().nextInt(cnt - 1) + ") AS temp ORDER BY ID DESC LIMIT 1");
        resultSet.next();
        String res = resultSet.getString(1);
        resultSet.close();
        statement.close();
        statement = connection.createStatement();
        statement.execute("DELETE FROM NAMES WHERE NAME = \"" + res + "\"");
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
        Connection connection = getConnection();
        String res = getLink(connection);
        connection.close();
        return res;
    }

    public static void useToken(String token) throws NoEnoughQuotaException, TokenNotFoundException, SQLException {
        Connection connection = LinkManager.getConnection();
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

    public static Pair<String, Boolean> getURL(String key) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT LINK, PREVIEW_PREVENT FROM LINKS WHERE NAME = ?");
        statement.setString(1, key);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            String s = resultSet.getString("LINK");
            Boolean previewPrevent = resultSet.getBoolean("PREVIEW_PREVENT");
            resultSet.close();
            statement.close();
            connection.close();
            Thread.startVirtualThread(new RenewLink(key));
            return new Pair<>(s, previewPrevent);
        } else {
            resultSet.close();
            statement.close();
            connection.close();
            return null;
        }
    }
}