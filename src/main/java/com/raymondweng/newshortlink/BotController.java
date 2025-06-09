package com.raymondweng.newshortlink;

import com.raymondweng.newshortlink.response.CreateResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@RestController
public class BotController implements EventListener {
    public static JDA jda;

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof MessageReceivedEvent && !((MessageReceivedEvent) genericEvent).isFromGuild() && !((MessageReceivedEvent) genericEvent).getAuthor().isBot()) {
            ((MessageReceivedEvent) genericEvent).getMessage()
                    .reply("感謝您的訊息！但是我們僅有提供斜槓指令，請輸入'/'開始嘗試他們。\n" +
                            "另外，也請看看我們的[官網](https://rwlink.us.kg)！")
                    .queue();
        }
        if (genericEvent instanceof SlashCommandInteractionEvent) {
            switch (((SlashCommandInteractionEvent) genericEvent).getName()) {
                case "create-token":
                    try {
                        ((SlashCommandInteractionEvent) genericEvent).reply(TokenFilter.getToken(((SlashCommandInteractionEvent) genericEvent).getUser().getId())).setEphemeral(true).queue();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "shorten-link":
                case "custom-link":
//                    if(((SlashCommandInteractionEvent) genericEvent).getName().equals("custom-link")) {
//                        ((SlashCommandInteractionEvent) genericEvent).reply("此服務維修中").setEphemeral(true).queue();
//                        break;
//                    }

                    String link = ((SlashCommandInteractionEvent) genericEvent).getOption("link").getAsString();
                    if (!link.matches("https?://\\S+")) {
                        ((SlashCommandInteractionEvent) genericEvent).reply("請輸入正確的網址。").setEphemeral(true).queue();
                    }
                    try {
                        if (((SlashCommandInteractionEvent) genericEvent).getName().equals("shorten-link")) {
                            ((SlashCommandInteractionEvent) genericEvent).reply(new RequestController()
                                    .create(
                                            "free",
                                            link,
                                            ((SlashCommandInteractionEvent) genericEvent).getOption("preview_prevent") != null && ((SlashCommandInteractionEvent) genericEvent).getOption("preview_prevent").getAsBoolean()
                                    )
                                    .getShort_link()).setEphemeral(true).queue();
                        } else {
                            Connection connection = LinkManager.getConnection();
                            TokenFilter.initUser(connection, ((SlashCommandInteractionEvent) genericEvent).getUser().getId());
                            if(TokenFilter.useQuota(connection, ((SlashCommandInteractionEvent) genericEvent).getUser().getId())) {
                                CreateResponse l = new RequestController()
                                        .create(
                                                ((SlashCommandInteractionEvent) genericEvent).getOption("name").getAsString(),
                                                link,
                                                ((SlashCommandInteractionEvent) genericEvent).getOption("preview_prevent") != null && ((SlashCommandInteractionEvent) genericEvent).getOption("preview_prevent").getAsBoolean()
                                        );
                                if (l.getShort_link() != null) {
                                    ((SlashCommandInteractionEvent) genericEvent).reply(l.getShort_link()).setEphemeral(true).queue();
                                } else {
                                    ((SlashCommandInteractionEvent) genericEvent).reply("這個連結可能已經被佔用了，請嘗試其他連結").setEphemeral(true).queue();
                                    PreparedStatement preparedStatement = connection.prepareStatement("UPDATE TOKENS SET QUOTA = QUOTA + 1 WHERE OWNER = ?");
                                    preparedStatement.executeUpdate();
                                    preparedStatement.close();
                                }
                            }else{
                                ((SlashCommandInteractionEvent) genericEvent).reply("你本月的額度已經用完了！請下個月在試（月初重置可能會有約兩個小時的處理時間）\n若同時有請求被拒絕，可能需要等待幾秒後再重新請求").setEphemeral(true).queue();
                            }
                            connection.close();
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        }
    }
}
