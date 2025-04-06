package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.LinkNotFoundException;
import com.raymondweng.newshortlink.exception.NoEnoughQuotaException;
import com.raymondweng.newshortlink.exception.TokenNotFoundException;

import java.sql.*;
import java.util.List;
import java.util.Random;

public class LinkManager {
    public static final List<String> BAN_KEYS = List.of("api", "discord");

    public static final int THREE_MONTH_LINK = 1;
    public static final int NO_EXPIRATION_LINK = 2;

    public static String getLink() throws SQLException {
        // fill un-used keys
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/data.db");
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM KEYS");
        resultSet.next();
        int cnt = resultSet.getInt(1);
        resultSet.close();
        statement.close();
        if (cnt < 50) {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT KEY FROM KEYS ORDER BY ID DESC LIMIT 1");
            resultSet.next();
            String last = resultSet.getString(1);
            resultSet.close();
            statement.close();
            int[] arr = new int[last.length()];
            for (int i = 0; i < last.length(); i++) {
                arr[i] = last.charAt(i);
            }
            for(int i = 0; i < 101; i++){
                for(int r = arr.length-1; r >= -1; r--){
                    if(r >= 0){
                        arr[r] += 1;
                        if(arr[r] <= (int)'z'){
                            break;
                        }else{
                            arr[r] = 'a';
                        }
                    }else{
                        int[] newArr = new int[arr.length+1];
                        newArr[0] = 'a';
                        System.arraycopy(arr, 0, newArr, 1, arr.length);
                        arr = newArr;
                    }
                }
                StringBuilder res = new StringBuilder();
                for (int j : arr) {
                    res.append((char) j);
                }
                if(BAN_KEYS.contains(res.toString())){
                    i--;
                    continue;
                }
                statement = connection.createStatement();
                statement.execute("INSERT INTO KEYS (KEY) VALUES (\"" + res + "\")");
                statement.close();
            }
            cnt += 100;
        }
        statement = connection.createStatement();
        resultSet = statement.executeQuery("SELECT * FROM (SELECT * FROM KEYS ORDER BY ID ASC LIMIT " + new Random().nextInt(cnt) + ") ORDER BY ID DESC LIMIT 1");
        String res = resultSet.getString(1);
        resultSet.close();
        statement.close();
        connection.close();
        return res;
    }

    private static void useToken(String token, int type) throws NoEnoughQuotaException, TokenNotFoundException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/data.db");
        PreparedStatement statement = switch (type) {
            case THREE_MONTH_LINK ->
                    connection.prepareStatement("SELECT THREE_MONTH_QUOTA FROM TOKENS WHERE TOKEN = ?");
            case NO_EXPIRATION_LINK ->
                    connection.prepareStatement("SELECT NO_EXPIRATION_QUOTA FROM TOKENS WHERE TOKEN = ?");
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        statement.setString(1, token);
        ResultSet resultSet = statement.executeQuery();
        boolean hasQuotaLimit;
        if (resultSet.next()) {
            hasQuotaLimit = resultSet.getInt(1) == -1;
            if(hasQuotaLimit && resultSet.getInt(1) == 0) {
                resultSet.close();
                statement.close();
                connection.close();
                throw new NoEnoughQuotaException();
            }
        }else{
            resultSet.close();
            statement.close();
            connection.close();
            throw new TokenNotFoundException();
        }
        resultSet.close();
        statement.close();
        if(hasQuotaLimit){
            statement = switch (type){
                case THREE_MONTH_LINK ->
                        connection.prepareStatement("UPDATE TOKENS SET THREE_MONTH_QUOTA = THREE_MONTH_QUOTA - 1 WHERE TOKEN = ?");
                case NO_EXPIRATION_LINK ->
                        connection.prepareStatement("UPDATE TOKENS SET NO_EXPIRATION_QUOTA = NO_EXPIRATION_QUOTA - 1 WHERE TOKEN = ?");
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
            statement.setString(1, token);
            statement.executeUpdate();
            statement.close();
        }
        connection.close();
    }

    public static String findToken(String discordID) throws SQLException, TokenNotFoundException {
        String token;
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/data.db");
        PreparedStatement statement = connection.prepareStatement("SELECT KEY FROM KEYS WHERE DISCORD_ID = ?");
        statement.setString(1, discordID);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            token = resultSet.getString("KEY");
        }else{
            resultSet.close();
            statement.close();
            connection.close();
            throw new TokenNotFoundException();
        }
        resultSet.close();
        statement.close();
        connection.close();
        return token;
    }

    public static String getURL(String key) throws LinkNotFoundException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/links.db");
        PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS WHERE KEY = ?");
        statement.setString(1, key);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            resultSet.close();
            statement.close();
            connection.close();
            return resultSet.getString("LINK");
        }else{
            resultSet.close();
            statement.close();
            connection.close();
            throw new LinkNotFoundException();
        }
    }
}