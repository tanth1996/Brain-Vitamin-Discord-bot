import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;
import java.nio.file.Files;

/** Bot invite link:
 * https://discord.com/api/oauth2/authorize?client_id=900233708290854972&permissions=2415929344&scope=bot%20applications.commands
 */

public class Bot extends ListenerAdapter {
    private static String token; // Token for Discord bot
    private static HashMap<String, UserData> allUserData = new HashMap<>(); // HashMap of all user data
    private static Type type = new TypeToken<HashMap<String, UserData>>() {}.getType(); // Used for JSON Serialisation
    private static File dataFile = new File("Data\\allUserData.json"); // JSON file containing user data
    private static User selfUser;

    /**
     * Simple utility function to write an object to file using GSON
     */
    private static void writeDataToFile(Object data, Type type, File file) throws IOException {

        FileWriter writer = new FileWriter(file);
        new Gson().toJson(data, type, writer);
        writer.flush();
        writer.close();
    }

    public static void main(String[] args) {
        // Initialise JSON file or retrieve user data from it
        try {
            if (dataFile.exists() && !dataFile.isDirectory()) {
                Reader reader = Files.newBufferedReader(dataFile.toPath());
                allUserData = new Gson().fromJson(reader, type);
                reader.close();
            } else {
                writeDataToFile(allUserData, type, dataFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get credentials
        try {
            File myObj = new File("Credentials\\token.txt");
            Scanner myReader = new Scanner(myObj);
            token = myReader.nextLine();
            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Initialise the bot
        JDABuilder builder = JDABuilder.createDefault(token);
        JDA jda;

        try {
            builder.setActivity(Activity.playing("Type /confused"));
            jda = builder.build();
            jda.awaitReady();

            selfUser = jda.getSelfUser();

            // Add the commands (bot commands can take up to 1 hour to propagate; use Guild commands for testing)
            jda.addEventListener(new Bot());
            jda.updateCommands()
                    .addCommands(new CommandData("ping", "Ping the bot"))
                    .addCommands(new CommandData("confused", "Confused a guy")
                            .addOption(OptionType.USER, "user", "The confused guy", true))
                    .addCommands(new CommandData("user_stats", "Check a user's stats")
                            .addOption(OptionType.USER, "user", "The user's stats you want to check", true))
                    .queue();

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
        catch(Exception e) {
            e.printStackTrace();
        }
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

            if (allUserData.containsKey(memberId)) {
                HashMap<Object, Object> userData = allUserData.get(memberId).getData();
                if (userData.containsKey("confused_n")) {
                    confused_n = ((Number)userData.get("confused_n")).longValue();
                    userData.put("confused_n", ++confused_n);
                }
                else userData.put("confused_n", confused_n);
            } else {
                UserData userData = new UserData(memberId, name, new HashMap<>());
                userData.setData("confused_n", confused_n);
                allUserData.put(memberId, userData);
            }

            // Update the confusion count
            try {
                writeDataToFile(allUserData, type, dataFile);

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
                HashMap<Object, Object> userData = allUserData.get(memberId).getData();
                StringBuilder replySb = new StringBuilder("Stats for " + name + "\n");
                for (Map.Entry entry : userData.entrySet()) {
                    replySb.append("- " + entry.getKey() + ": " + entry.getValue() + "\n");
                }
                event.getHook().sendMessage(replySb.toString()).queue();
                
            } else {
                event.getHook().sendMessage("No data for " + name + " has been recorded yet.").queue();
            }
        }
    }
}
