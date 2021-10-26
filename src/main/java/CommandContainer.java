import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommandContainer extends ListenerAdapter {
    public List<CommandData> cmdList = new ArrayList<CommandData>();
    private Bot bot;
    private User selfUser;
    private HashMap<String, UserData> allUserData;

    public CommandContainer(Bot bot) {
        // Get required fields from bot
        this.bot = bot;
        selfUser = bot.selfUser;
        allUserData = bot.getAllUserData();

        // Add all bot commands in the constructor
        cmdList.add(new CommandData("ping", "Ping the bot (does not work sometimes, fuck if I know why)"));
        cmdList.add(new CommandData("confused", "Confused a guy")
                .addOption(OptionType.USER, "user", "The confused guy", true));
        cmdList.add(new CommandData("user_stats", "Check a user's stats")
                .addOption(OptionType.USER, "user", "The user's stats you want to check", true));

//            // Testing with Guild commands
//            Guild guild = jda.getGuildById(900233285974761483l);
//            System.out.println(guild.getRoles());
//
//            guild.updateCommands()
//                    .queue(); // Reset guild commands to empty
//
////            guild.updateCommands().addCommands(new CommandData("user_stats", "Check a user's stats (guild cmd)")
////                            .addOption(OptionType.USER, "user", "The user's stats you want to check", true))
////                    .queue();
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event)
    {
        System.out.println("Event received: " + event.getName());

        /* TODO: Known issue - only this command i.e. "ping" produces "Invalid interaction application command" in
            Discord with no error thrown in console
         */
        // ping command
        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                    .flatMap(v ->
                            event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                    ).queue(); // Queue both reply and edit
        }

        // confused command
        if (event.getName().equals("confused")) {
            event.deferReply().queue();
            event.getChannel().sendTyping().queue();

            Member member = event.getOption("user").getAsMember();
            String name = member.getEffectiveName();
            String memberId = member.getId();

            if (memberId.equals(selfUser.getId())) {
                // TODO: Figure out why UTF encoding cannot be sent eg. "( •_•)"
                String reply = selfUser.getName() + " cannot be confused ( ._.)";

                event.getHook().sendMessage(reply).queue();
                return;
            }

            String reply = name + " hurt itself in its own confusion!";
            long confused_n = 1;

            if (allUserData == null) allUserData = new HashMap<>();

            // Check if user exists within allUserData
            if (allUserData.containsKey(memberId)) {
                UserData userData = allUserData.get(memberId);
                if (userData.dataContainsKey("confused_n")) {
                    confused_n = ((Number) userData.getData("confused_n")).longValue();
                    userData.setData("confused_n", ++confused_n);
                }
                else userData.setData("confused_n", confused_n);
            } else { // If user doesn't exist in allUserData yet, add the user and initialise the confusion count
                UserData userData = new UserData(memberId, name, new HashMap<>());
                userData.setData("confused_n", confused_n);
                allUserData.put(memberId, userData);
            }

            // Write the update to file
            try {
                bot.writeDataToFile(allUserData, bot.type, bot.dataFile);

                event.getHook().sendMessage(reply).queue();
                event.getChannel().sendMessage(confused_n + " times!").queue();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        // userStats command
        if (event.getName().equals("user_stats")) {
            event.deferReply().queue();
            event.getChannel().sendTyping().queue();

            Member member = event.getOption("user").getAsMember();
            String name = member.getEffectiveName();
            String memberId = member.getId();

            if (allUserData == null) {
                event.getHook().sendMessage("Something has went wrong - no user data has been detected").queue();
                return;
            }

            if (allUserData.containsKey(memberId)) {
                UserData userData = allUserData.get(memberId);
                event.getHook().sendMessage(userData.userStatsToString()).queue();
            } else {
                event.getHook().sendMessage("No data for " + name + " has been recorded yet.").queue();
            }
        }
    }
}
