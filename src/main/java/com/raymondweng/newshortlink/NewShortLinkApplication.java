package com.raymondweng.newshortlink;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootApplication
public class NewShortLinkApplication {

    public static void main(String[] args) {
        System.out.print("Input the token: ");
        String token;
        try {
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(isr);
            token = br.readLine();
            br.close();
            isr.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BotController.jda = JDABuilder
                .createDefault(token)
                .addEventListeners(new BotController())
                .build();
        BotController.jda
                .upsertCommand("create-token", "Create a new token for api or custom token").queue();
        BotController.jda
                .upsertCommand("shorten-link", "Shorten a link.")
                .addOption(OptionType.STRING, "link", "The link to be shorten.", true)
                .queue();
        BotController.jda
                .upsertCommand("custom-link", "Create a custom shortened link. Create once every month for free.")
                .addOption(OptionType.STRING, "link", "The link to be shorten", true)
                .addOption(OptionType.STRING, "custom-name", "Link will be https://rwlink.us.kg/<custom-name>", true)
                .queue();

        boolean databaseExists = new File("./database/data.db").exists();
        if (!databaseExists) {
            try {
                // create database of links
                Connection connection = DriverManager.getConnection("jdbc:sqlite:./database/links.db");
                Statement statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE LINKS" +
                        "(KEY TEXT NOT NULL," +
                        "LINK TEXT NOT NULL, " +
                        "EXPIRED_DATE DATE)");
                statement.close();
                statement = connection.createStatement();
                statement.execute("CREATE UNIQUE INDEX key_index ON LINKS (KEY)");
                statement.close();
                connection.close();

                // create database of other things
                // un-used keys
                connection = DriverManager.getConnection("jdbc:sqlite:./database/data.db");
                statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE KEYS" +
                        "(ID INTEGER PRIMARY KEY AUTOINCREMENT ," +
                        "KEY TEXT NOT NULL, " +
                        "LAST_USED DATE NOT NULL DEFAULT CURRENT_DATE)");
                statement.close();
                statement = connection.createStatement();
                statement.execute("INSERT INTO KEYS (KEY) VALUES ('aaa')");
                statement.close();
                Thread thread = new Thread(new RefillKeys());
                thread.start();
                // tokens
                statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE TOKENS" +
                        "(OWNER TEXT PRIMARY KEY NOT NULL, " +
                        "TOKEN TEXT UNIQUE NOT NULL, " +
                        "QUOTA INTEGER NOT NULL, " +
                        "QUOTA_RESET DATE NOT NULL, " +
                        "TOKEN_TYPE INTEGER NOT NULL)");
                statement.close();
                statement = connection.createStatement();
                statement.execute("CREATE UNIQUE INDEX token_index ON TOKENS (OWNER, TOKEN)");
                statement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        SpringApplication.run(NewShortLinkApplication.class, args);
    }

}
