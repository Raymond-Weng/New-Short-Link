package com.raymondweng.newshortlink.threads;

import com.raymondweng.newshortlink.LinkManager;
import redis.clients.jedis.Jedis;

public class RefillKeys implements Runnable {
    private static volatile boolean keyRefilling = false;

    public synchronized boolean canRefill() {
        if (keyRefilling) {
            return false;
        }
        keyRefilling = true;
        return true;
    }

    @Override
    public synchronized void run() {
        if (!canRefill()) {
            return;
        }
        try (Jedis jedis = LinkManager.jedisPool.getResource()) {
            jedis.select(0);
            if (jedis.scard("keys") >= 1000) {
                keyRefilling = false;
                return;
            }
            String next = jedis.get("NEXT");
            for (int i = 0; i < 1000; i++) {
                jedis.sadd("keys", next);
                boolean added = false;
                StringBuilder sb = new StringBuilder();
                for (int r = next.length() - 1; r >= 0; r--) {
                    if (next.charAt(r) == 'z') {
                        sb.append('a');
                    } else {
                        sb.append((char) (next.charAt(r) + 1));
                        sb.append(new StringBuilder(next.substring(0, r)).reverse());
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    sb.append('a');
                }
                next = sb.reverse().toString();
            }
            jedis.set("NEXT", next);
        }
        keyRefilling = false;
    }
}
