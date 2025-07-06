
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.core.events.guild.GuildReadyEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.managers.GuildManager;
import net.dv8tion.jda.core.requests.Route;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;
import net.dv8tion.jda.core.requests.restaction.GuildAction;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

//This class runs a Discord Bot capable of independently sending messages to a Discord server specified in the build.gradle file.
public class DiscordBot implements EventListener
{
    //Link to invite bot = "https://discordapp.com/oauth2/authorize?client_id=534861471637700639&scope=bot&permissions=8";
    public static final String token = "NTM0ODYxNDcxNjM3NzAwNjM5.DyFA4g.El9viK2BVWfGcF-uombppcKL-ig";
    public JDA jda;
    public Guild guild;
    public List<TextChannel> textChannels;
    public boolean entered;

    //[new DiscordBot.setupBot(guildId)] starts the bot.
    public DiscordBot(){
        jda = null;
        guild = null;
        textChannels = new ArrayList<>();
        entered = false;
    }
    public DiscordBot setupBot(String guildId){
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(token).addEventListener(new DiscordBot()).build();
            jda.awaitReady();
        } catch (LoginException e) {e.printStackTrace();} catch (InterruptedException e) {e.printStackTrace();}
        guild = jda.getGuildById(guildId);
        textChannels = guild.getTextChannels();
        return this;
    }

    //Doesn't do anything right now, but you can program the bot to respond to any "event".
    @Override
    public void onEvent(Event event)
    {

    }

    //Sends a message to a text channel named "bot", creates the channel if it doesn't exist.
    public void sendMessage(String message){
        for (TextChannel text : textChannels)
            if(text.getName().toLowerCase().equals("bot")){
                text.sendMessage(message).queue();
                return;
            }
        GuildController guildController = new GuildController(guild);
        TextChannel newChannel = (TextChannel) guildController.createTextChannel("bot").complete();
        newChannel.sendMessage(message).queue();
    }
    
    //Sends a message to a specified text channel, creates the channel if it doesn't exist.
    public void sendMessage(String message, String channel){
        for (TextChannel text : textChannels)
            if(text.getName().toLowerCase().equals(channel)){
                text.sendMessage(message).queue();
                return;
            }
        GuildController guildController = new GuildController(guild);
        TextChannel newChannel = (TextChannel) guildController.createTextChannel(channel).complete();
        newChannel.sendMessage(message).queue();
    }
}