package com.raymondweng.newshortlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootApplication
public class NewShortLinkApplication {

	public static void main(String[] args) {

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
						"KEY TEXT NOT NULL)");
				statement.close();
				statement = connection.createStatement();
				statement.execute("INSERT INTO KEYS (KEY) VALUES ('aaa')");
				statement.close();
				// tokens
				statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE TOKENS" +
						"(OWNER TEXT PRIMARY KEY NOT NULL, " +
						"TOKEN TEXT UNIQUE NOT NULL, " +
						"THREE_MONTH_QUOTA INTEGER NOT NULL," +
						"NO_EXPIRATION_QUOTA INTEGER NOT NULL," +
						"QUOTA_RESET DATE NOT NULL, " +
						"TOKEN_TYPE INTEGER NOT NULL)");
				statement.close();
				statement = connection.createStatement();
				statement.execute("CREATE UNIQUE INDEX token_index ON TOKENS (OWNER, TOKEN)");
				statement.close();
				// tickets (in discord)
				statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE CHANNELS" +
						"(CHANNEL_ID TEXT PRIMARY KEY NOT NULL, " +
						"EXPIRED_TIME DATETIME NOT NULL)");
				statement.close();
				connection.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		SpringApplication.run(NewShortLinkApplication.class, args);
    }

}
