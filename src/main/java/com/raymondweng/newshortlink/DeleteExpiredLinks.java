package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.request.Link;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.*;
import java.util.Date;

public class DeleteExpiredLinks implements Runnable {
    Date lastUpdated = Date.from(Instant.now());
    @Override
    public void run() {
        while(true){
            if(lastUpdated.compareTo(Date.from(Instant.now())) <= 0){
                try {
                    Thread.sleep(1000*60*60);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else{
                try {
                    Connection connection = LinkManager.getConnection();
                    Statement statement = connection.createStatement();
                    statement.executeUpdate("DELETE FROM LINKS WHERE DATE(LAST_USED) < DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR);");
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
