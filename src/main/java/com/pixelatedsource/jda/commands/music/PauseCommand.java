package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class PauseCommand extends Command {

    public PauseCommand() {
        this.commandName = "pause";
        this.description = "pause the queue without stopping or deleting songs";
        this.category = Category.MUSIC;
        this.usage = PREFIX + commandName + " [on/enable/true | off/disable/false]";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    String s = player.getPaused() ? "paused" : "playing";
                    event.reply("Music is **" + s + "**.");
                } else if (args.length == 1) {
                    switch (args[0]) {
                        case "on":
                        case "enable":
                        case "true":
                            player.getAudioPlayer().setPaused(true);
                            event.reply("Paused by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                            break;
                        case "off":
                        case "disable":
                        case "false":
                            player.getAudioPlayer().setPaused(false);
                            event.reply("Resumed by **" + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator() + "**");
                            break;
                        default:
                            event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                            break;
                    }

                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}