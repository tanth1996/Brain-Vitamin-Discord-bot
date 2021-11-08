package brainvitamin;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.managers.AudioManager;

public class AudioCommandContainer extends CommandContainer {
    private final AudioPlayerManager playerManager;
    public final AudioPlayer player;
    public final TrackScheduler scheduler;

    public AudioCommandContainer() {
        cmdList.add(new CommandData("play_audio", "Play audio in target channel from the specified source (in development)")
                .addOption(OptionType.STRING, "source", "Audio source", true)
                .addOption(OptionType.CHANNEL, "channel", "Target channel", true));

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        player = playerManager.createPlayer();
        scheduler = new TrackScheduler(player);
        player.addListener(scheduler);
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (event.getName().equals("play_audio")) {
            System.out.println("play_audio: This command is under development");

            // Get target channel and ensure it is a VoiceChannel
            GuildChannel channel = event.getOption("channel").getAsGuildChannel();
            if (channel.getType() != ChannelType.VOICE) {
                event.reply("Specified channel is not a voice channel; please select a voice channel").setEphemeral(true).queue();
                return;
            }
            VoiceChannel voiceChannel = (VoiceChannel) channel;

            // Defer reply
            event.deferReply(true).queue();

            // TODO: Audio stream gets sped up when this command is called on more than one server (siphoning)
            event.getGuild().getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

            String source = event.getOption("source").getAsString();

            System.out.format("source: %s %nchannel: %s", source, voiceChannel.getName());

            playerManager.loadItem(source, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    event.getHook().sendMessage("Adding to queue " + track.getInfo().title).queue();

                    play(track, voiceChannel, channel.getGuild());
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    AudioTrack firstTrack = playlist.getSelectedTrack();

                    if (firstTrack == null) {
                        firstTrack = playlist.getTracks().get(0);
                    }

                    event.getHook().sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                    play(firstTrack, voiceChannel, channel.getGuild());
                }

                @Override
                public void noMatches() {
                    event.getHook().sendMessage("Nothing found by " + source).queue();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    event.getHook().sendMessage("Could not play: " + exception.getMessage()).queue();
                }
            });
        }
    }

    private void play(AudioTrack track, VoiceChannel voiceChannel, Guild guild) {
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(voiceChannel);
        }
        scheduler.queue(track);
    }
}
