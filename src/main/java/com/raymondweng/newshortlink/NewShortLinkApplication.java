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
        SpringApplication.run(NewShortLinkApplication.class, args);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Dotenv dotenv = Dotenv.configure().directory("./env").load();
        BotController.jda = JDABuilder
                .createDefault(dotenv.get("BOT_TOKEN"))
                .addEventListeners(new BotController())
                .build();
        BotController.jda
                .upsertCommand("create-token", "建立一個token").queue();
        BotController.jda
                .upsertCommand("shorten-link", "快速縮短網址")
                .addOption(OptionType.STRING, "link", "要被縮短的網址", true)
                .addOption(OptionType.BOOLEAN, "preview_prevent", "是否需要防止預覽（預設：否）", false)
                .queue();
        BotController.jda
                .upsertCommand("custom-link", "建立一個專屬於你的連結")
                .addOption(OptionType.STRING, "link", "要被縮短的網址", true)
                .addOption(OptionType.STRING, "name", "要在/後面出現的東西")
                .addOption(OptionType.BOOLEAN, "preview_prevent", "是否需要防止預覽（預設：否）", false)
                .queue();
        Thread.startVirtualThread(new RefillKeys());
        Thread.startVirtualThread(new DeleteExpiredLinks());
    }

}
