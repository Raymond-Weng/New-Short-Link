package com.raymondweng.newshortlink;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootApplication
public class NewShortLinkApplication {

    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        SpringApplication.run(NewShortLinkApplication.class, args);
        Dotenv dotenv = Dotenv.configure().directory("./env").load();
        BotController.jda = JDABuilder
                .createDefault(dotenv.get("BOT_TOKEN"))
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
        Thread.startVirtualThread(new RefillKeys());
    }

}
