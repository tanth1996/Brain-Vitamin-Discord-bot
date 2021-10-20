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
import java.util.*;
import java.nio.file.Files;

public class Bot extends ListenerAdapter {
    private static String token; // Token for Discord bot
    private static HashMap<String, UserData> allUserData = new HashMap<>(); // HashMap of all user data
    private static Type type = new TypeToken<HashMap<String, UserData>>() {}.getType(); // Used for JSON Serialisation
    private static File dataFile = new File("Data\\allUserData.json"); // JSON file containing user data

    public static void main(String[] args) {
        // Initialise JSON file or retrieve user data from it
        try {
            if (dataFile.exists() && !dataFile.isDirectory()) {
                Reader reader = Files.newBufferedReader(dataFile.toPath());
                allUserData = new Gson().fromJson(reader, type);
                reader.close();
            } else {
                FileWriter writer = new FileWriter(dataFile);
                new Gson().toJson(allUserData, type, writer);
                writer.flush();
                writer.close();
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

            jda.addEventListener(new Bot());
            jda.updateCommands()
                    .addCommands(new CommandData("ping", "Ping the bot"))
                    .addCommands(new CommandData("confused", "Confused a guy")
                            .addOption(OptionType.USER, "user", "The confused guy", true))
                    .queue();

            // Testing with Guild commands
//            Guild guild = jda.getGuildById(900233285974761483l);
//            System.out.println(guild.getRoles());
//            guild.updateCommands()
//                    .queue();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event)
    {
        System.out.println("Event received: " + event.getName());

        // Ping command
        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                    .flatMap(v ->
                            event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                    ).queue(); // Queue both reply and edit
        }

        // Confused command
        if (event.getName().equals("confused")) {
            event.getChannel().sendTyping().queue();

            Member member = event.getOption("user").getAsMember();
            String name = member.getEffectiveName();
            String memberId = member.getId();

            String reply = name + " hurt itself in its own confusion!";
            event.deferReply().queue();

            long confused_n = 1;

            if (allUserData == null) allUserData = new HashMap<>();

            if (allUserData.containsKey(memberId)) {
                HashMap<Object, Object> userData = allUserData.get(memberId).getData();
                if (userData.containsKey("confused_n")) {
                    confused_n = ((Number)userData.get("confused_n")).longValue();
                    userData.put("confused_n", ++confused_n);
                }
                else userData.put("confused_n", Long.valueOf(1l));
            } else {
                UserData userData = new UserData(memberId, name, new HashMap<>());
                userData.setData("confused_n", confused_n);
                allUserData.put(memberId, userData);
            }

            // Update the confusion count
            try {
                FileWriter writer = new FileWriter(dataFile);
                new Gson().toJson(allUserData, type, writer);
                writer.flush();
                writer.close();

                event.getHook().sendMessage(reply).queue();
                event.getChannel().sendMessage(confused_n + " times!").queue();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}
