package com.raymondweng.newshortlink.loops;

import com.raymondweng.newshortlink.LinkManager;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddToMySQL implements Runnable {

    @Override
    public void run() {
        try(Jedis jedis = LinkManager.jedisPool.getResource()) {
            while (true) {
                String name = jedis.blpop(0, "toAdd").get(1);
                String link = jedis.get("l_"+name);
                jedis.del("l_"+name);
                String previewPrevent = jedis.get("p_"+name);
                try(Connection connection = LinkManager.hikariDataSource.getConnection()){
                    PreparedStatement statement = connection.prepareStatement("insert into LINKS(NAME, LINK, PREVIEW_PREVENT) values(?,?,?)");
                    statement.setString(1, name);
                    statement.setString(2, link);
                    statement.setString(3, previewPrevent);
                    statement.execute();
                    statement.close();
                }catch(SQLException e){
                    //TODO
                    e.printStackTrace();
                }
            }
        }
    }
}
