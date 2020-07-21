package com.destructio.discordbot;

import com.destructio.discordbot.audio.LavaPlayerAudioProvider;
import com.destructio.discordbot.audio.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ExampleBot {

    private static final Map<String, Command> commands = new HashMap<>();
    // Creates AudioPlayer instances and translates URLs to AudioTrack instances
    final static AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    // Create an AudioPlayer so Discord4J can receive audio data
    final static AudioPlayer player = playerManager.createPlayer();

    // Creating LavaPlayerAudioProvider
    final static AudioProvider provider = new LavaPlayerAudioProvider(player);

    static Logger LOG = Logger.getLogger(ExampleBot.class);


    public static void main(final String[] args) {

        LOG.info("Starting the Bot");

        // This is an optimization strategy that Discord4J can utilize. It is not important to understand
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);

        final GatewayDiscordClient bot = DiscordClientBuilder.create(args[0]).build()
                .login()
                .block();

        bot.getEventDispatcher().on(MessageCreateEvent.class)


        // subscribe is like block, in that it will *request* for action
        // to be done, but instead of blocking the thread, waiting for it
        // to finish, it will just execute the results asynchronously.

        .subscribe(event -> {
            final String content = event.getMessage().getContent();
            for (final Map.Entry<String, Command> entry : commands.entrySet()) {

                if (content.startsWith('!' + entry.getKey())) {
                    entry.getValue().execute(event);
                    break;
                }
            }
        });

        bot.onDisconnect().block();
    }
    static {
        commands.put("ping", event -> event.getMessage()
                .getChannel().block()
                .createMessage("pong!")
                .block());

        commands.put("help", event -> event.getMessage()
                .getChannel().block()
                .createMessage("More information about the commands and other stuff you can check out here: " +
                        "https://github.com/Destructio/DiscordExampleBot/blob/master/README.md")
                .block());

        commands.put("join", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null) {

                        // join returns a VoiceConnection which would be required if we were
                        // adding disconnection features, but for now we are just ignoring it.
                        channel.join(spec -> spec.setProvider(provider)).block();

                    }
                }
            }
        });
        final TrackScheduler scheduler = new TrackScheduler(player);

        commands.put("play", event -> {
            final String content = event.getMessage().getContent();
            final List<String> command = Arrays.asList(content.split(" "));
            playerManager.loadItem(command.get(1), scheduler);
        });

        commands.put("stop", event -> {
                player.stopTrack();
        });


        commands.put("roll", event -> {
            final String content = event.getMessage().getContent();
            final List<String> command = Arrays.asList(content.split(" "));
            String out;
            int wordCount = command.size();
            switch (wordCount)
            {
                case 1:
                    {
                        int random = ThreadLocalRandom.current().nextInt(); //TODO Recode this cause numbers are too strange?
                        LOG.info("Random number is RandomThread int - " + random);
                        out = String.valueOf(random);
                        break;
                    }
                case 2:
                    {
                        int random = new Random().nextInt((Integer.parseInt(command.get(1))) + 1);
                        LOG.info("Random number in range " + command.get(1) + " is - " + random);
                        out = String.valueOf(random);
                        break;
                    }
                default:
                    out = "Please enter correct range! e.g !roll 100";
            }

            event.getMessage().getChannel()
                    .block()
                    .createMessage(out)
                    .block();
        });
    }

}
