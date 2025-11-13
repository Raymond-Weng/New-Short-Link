package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.exception.InvalidLinkException;
import com.raymondweng.newshortlink.exception.InvalidNameException;
import com.raymondweng.newshortlink.exception.LinkNotFoundException;
import io.github.cdimascio.dotenv.Dotenv;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.Jedis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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

    public static Connection getConnection() throws SQLException {
        Dotenv dotenv = Dotenv.configure().directory("./env/.env").load();
        return DriverManager.getConnection("jdbc:mysql://" + dotenv.get("DB_ADDRESS") + "/mydb?serverTimezone=Asia/Taipei", dotenv.get("DB_ACC"), dotenv.get("DB_PASS"));
    }

    /**
     * find link by name
     * @param name name of the link
     * @return (link, previewPrevent) if found else null
     */
    public static Pair<String, Boolean> find(String name) throws LinkNotFoundException {
        Jedis jedis = new Jedis("localhost");
        jedis.select(1);
        if(!jedis.exists(name)){
            //TODO find data and add into it
        }
        String link = jedis.get(name);
        if(link.isBlank()){
            jedis.close();
            throw new LinkNotFoundException();
        }
        jedis.close();
        return new Pair<>(link, true);
    }

    /**
     * register a link, another thread will add it to db later
     * @param name name of link, leave it empty to get a random one,
     *             custom link should contain at least a caption letter
     * @param link link
     * @param previewPrevent should we prevent social to preview it
     * @throws InvalidNameException will be thrown if name is invalid
     * @throws InvalidLinkException will be thrown if link is invalid
     */
    public static String register(String name, String link, boolean previewPrevent) throws InvalidNameException, InvalidLinkException {
        if(BAN_KEYS.contains(name) || (name.isEmpty() && !NAME_PATTERN.matcher(name).matches())) {
            throw new InvalidNameException();
        }
        try{
            if(!Set.of("http", "https").contains(new URI(link).getScheme()) || !new URI(link).isAbsolute()) {
                throw new InvalidLinkException();
            }
            new URI(link).toURL();
        }catch (InvalidLinkException | MalformedURLException | URISyntaxException e){
            throw new InvalidLinkException();
        }
        String n = name.isEmpty() ? getName() : name;

        Jedis jedis = new Jedis("localhost");
        jedis.select(1);
        AbstractTransaction abstractTransaction = jedis.multi();
        abstractTransaction.sadd("toAdd", n);
        abstractTransaction.set("l_"+n, link);
        abstractTransaction.set("p_"+n, previewPrevent ? "1" : "0");
        abstractTransaction.exec();
        abstractTransaction.close();
        jedis.close();

        return n;
    }

    /**
     * get a random name
     * @return yes, random name
     */
    private static String getName(){
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.select(0);
        String res = jedis.spop("keys");
        jedis.close();
        return res;
    }
}