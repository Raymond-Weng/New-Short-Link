package com.raymondweng.newshortlink;

import java.sql.*;

public class RefillKeys implements Runnable {
    @Override
    public synchronized void run() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/data.db");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM KEYS");
            resultSet.next();
            int cnt = resultSet.getInt(1);
            resultSet.close();
            statement.close();
            if (cnt >= 50) {
                return;
            }
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
            for (int i = 0; i < 101; i++) {
                for (int r = arr.length - 1; r >= -1; r--) {
                    if (r >= 0) {
                        arr[r] += 1;
                        if (arr[r] <= (int) '9') {
                            break;
                        } else if (arr[r] <= (int) 'z') {
                            if (arr[r] >= (int) 'a') {
                                break;
                            } else {
                                arr[r] = '0';
                            }
                        } else {
                            arr[r] = 'a';
                        }
                    } else {
                        int[] newArr = new int[arr.length + 1];
                        newArr[0] = '0';
                        System.arraycopy(arr, 0, newArr, 1, arr.length);
                        arr = newArr;
                    }
                }
                StringBuilder res = new StringBuilder();
                for (int j : arr) {
                    res.append((char) j);
                }
                statement = connection.createStatement();
                statement.execute("INSERT INTO KEYS (KEY) VALUES (\"" + res + "\")");
                statement.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
