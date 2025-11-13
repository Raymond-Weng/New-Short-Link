package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.InvalidLinkException;
import com.raymondweng.newshortlink.exception.InvalidNameException;
import com.raymondweng.newshortlink.exception.LinkNotFoundException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.raymondweng.newshortlink.NewShortLinkApplication.dotenv;

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
    public static final Pattern NAME_PATTERN = Pattern.compile("^(?=.*[A-Z])[a-zA-Z0-9]{2,100}");
    public static final JedisPool jedisPool = new JedisPool("localhost", 6379);
    public static final HikariDataSource hikariDataSource;

    static {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + dotenv.get("DB_ADDRESS") + "/mydb?serverTimezone=Asia/Taipei");
        hikariConfig.setUsername(dotenv.get("DB_ACC"));
        hikariConfig.setPassword(dotenv.get("DB_PASS"));
        hikariDataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * find link by name
     *
     * @param name name of the link
     * @return (link, previewPrevent) if found else null
     */
    public static Pair<String, Boolean> find(String name) throws LinkNotFoundException, SQLException {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.exists(name)) {
                try (Connection connection = hikariDataSource.getConnection()) {
                    PreparedStatement statement = connection.prepareStatement("SELECT LINK, PREVIEW_PREVENT FROM LINKS WHERE NAME = ?");
                    statement.setString(1, name);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            AbstractTransaction at = jedis.multi();
                            at.setex(name, 10 * 60, resultSet.getString("LINK"));
                            at.setex("cp_" + name, 10 * 60, resultSet.getBoolean("PREVIEW_PREVENT") ? "1" : "0");
                            at.exec();
                            at.close();
                        } else {
                            jedis.set(name, "");
                        }
                    }
                    statement.close();
                }
            }
            String link = jedis.get(name);
            if (link.isBlank()) {
                throw new LinkNotFoundException();
            }
            boolean previewPrevent = "1".equals(jedis.get("p_" + name));
            return new Pair<>(link, previewPrevent);
        }
    }

    /**
     * register a link, another thread will add it to db later
     *
     * @param name           name of link, leave it empty to get a random one,
     *                       custom link should contain at least a caption letter
     * @param link           link
     * @param previewPrevent should we prevent social to preview it
     * @throws InvalidNameException will be thrown if name is invalid
     * @throws InvalidLinkException will be thrown if link is invalid
     */
    public static String register(String name, String link, boolean previewPrevent) throws InvalidNameException, InvalidLinkException {
        if (BAN_KEYS.contains(name) || (!name.isEmpty() && !NAME_PATTERN.matcher(name).matches())) {
            throw new InvalidNameException();
        }
        try {
            if (!Set.of("http", "https").contains(new URI(link).getScheme()) || !new URI(link).isAbsolute()) {
                throw new InvalidLinkException();
            }
            new URI(link).toURL();
        } catch (InvalidLinkException | MalformedURLException | URISyntaxException e) {
            throw new InvalidLinkException();
        }
        String n = name.isEmpty() ? getName() : name;
        try {
            find(n);
        } catch (LinkNotFoundException e) {
            try (Jedis jedis = jedisPool.getResource()) {
                AbstractTransaction at = jedis.multi();
                at.setex(n, 10 * 60, link);
                at.setex("cp_" + n, 10 * 60, previewPrevent ? "1" : "0");
                at.exec();
                at.close();
                at = jedis.multi();
                at.lpush("toAdd", n);
                at.set("l_" + n, link);
                at.set("p_" + n, previewPrevent ? "1" : "0");
                at.exec();
                at.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return n;
    }

    /**
     * get a random name
     *
     * @return yes, random name
     */
    private static String getName() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.spop("keys");
        }
    }
}