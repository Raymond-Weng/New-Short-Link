package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.request.Link;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class BotController implements EventListener {
    public static JDA jda;


    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof MessageReceivedEvent && !((MessageReceivedEvent) genericEvent).isFromGuild() && !((MessageReceivedEvent) genericEvent).getAuthor().isBot()) {
            ((MessageReceivedEvent) genericEvent).getMessage()
                    .reply("感謝您的訊息！但是我們僅有提供斜槓指令，請輸入'/'開始嘗試他們。\n" +
                            "另外，也請看看我們的[官網](https://rwlink.us.kg)！\n" +
                            "---\n" +
                            "English: \n" +
                            "Thanks for your message! However, we only offer slash commands for functions, try them by typing '/'.\n" +
                            "Also, visit our [website](https://rwlink.us.kg)!")
                    .queue();
        }
        if (genericEvent instanceof SlashCommandInteractionEvent) {
            switch (((SlashCommandInteractionEvent) genericEvent).getName()) {
                case "create-token":
                    ((SlashCommandInteractionEvent) genericEvent).reply("Developing this feature. We will notify you when things ready.").setEphemeral(true).queue();
                    break;
                case "shorten-link":
                    String shortenLink;
                    try {
                        shortenLink = new RequestController().create("free", new Link(((SlashCommandInteractionEvent) genericEvent).getOption("link").getAsString())).getShort_link();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    ((SlashCommandInteractionEvent) genericEvent).reply(shortenLink).setEphemeral(true).queue();
                    break;
                case "custom-link":
                    ((SlashCommandInteractionEvent) genericEvent).reply("Developing this feature. We will notify you when things ready.").setEphemeral(true).queue();
                    break;
            }
        }
    }
}
