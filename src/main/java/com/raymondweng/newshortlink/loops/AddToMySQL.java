package com.raymondweng.newshortlink.loops;

import com.raymondweng.newshortlink.LinkManager;
import redis.clients.jedis.Jedis;

public class AddToMySQL implements Runnable {

    @Override
    public void run() {
        Jedis jedis = LinkManager.jedisPool.getResource();
        while (true) {
            
        }
    }
}
