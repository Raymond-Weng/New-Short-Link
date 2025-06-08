package com.raymondweng.newshortlink;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class RenewLink implements Runnable {
    private final String name;

    public RenewLink(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        try {
            Connection connection = LinkManager.getConnection();
            Statement statement = connection.createStatement();
            statement.executeQuery("UPDATE LINKS SET LAST_USED=CURRENT_TIMESTAMP WHERE NAME = '" + name + "';");
            statement.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
