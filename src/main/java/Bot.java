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
    private static String token;
    private static HashMap<String, UserData> allUserData = new HashMap<>();
    private static Type type = new TypeToken<HashMap<String, UserData>>() {}.getType();
    private static File dataFile = new File("Data\\allUserData.json");

    public static void main(String[] args) {
        try {
            if (dataFile.exists() && !dataFile.isDirectory()) {
                Reader reader = Files.newBufferedReader(dataFile.toPath());
                allUserData = new Gson().fromJson(reader, type);

//                MyClass object = new Gson().fromJson(new Gson().toJson(((LinkedTreeMap<String, Object>) theLinkedTreeMapObject)), MyClass .class);
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

        try {
            File myObj = new File("Credentials\\token.txt");
            Scanner myReader = new Scanner(myObj);
            token = myReader.nextLine();
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

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
            Guild guild = jda.getGuildById(900233285974761483l);
            System.out.println(guild.getRoles());
            guild.updateCommands()
                    .queue();
        }
        catch(javax.security.auth.login.LoginException e) {
            e.printStackTrace();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event)
    {
        System.out.println("Event received: " + event.getName());

        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                    .flatMap(v ->
                            event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                    ).queue(); // Queue both reply and edit
        }

        if (event.getName().equals("confused")) {
            event.getChannel().sendTyping().queue();
            Member member = event.getOption("user").getAsMember();
            String name = member.getEffectiveName();
            String reply = name + " hurt itself in its own confusion!";
            event.deferReply().queue();

            String memberId = member.getId();
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
