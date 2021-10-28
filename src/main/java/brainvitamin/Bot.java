package brainvitamin;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/** Bot invite link:
 * https://discord.com/api/oauth2/authorize?client_id=900233708290854972&permissions=2415929344&scope=bot%20applications.commands
 */

public class Bot {
//    private HashMap<String, UserData> allUserData = new HashMap<>(); // HashMap of all user data
//    public final Type type = new TypeToken<HashMap<String, UserData>>() {}.getType(); // Used for JSON Serialisation
//    public final Path dataFile = Paths.get("Data\\allUserData.json"); // JSON file containing user data
    private final String connUrl = "jdbc:sqlite:data\\allUserData.db"; // URL for DB connection
    public User selfUser;
    private JDA jda = null;

    public String getConnUrl() {
        return connUrl;
    }

    public void initialise() {
//        // Initialise JSON file or retrieve user data from it
//        if (Files.isRegularFile(dataFile)) {
//            try (Reader reader = Files.newBufferedReader(dataFile)) {
//                allUserData = new Gson().fromJson(reader, type);
//            } catch (IOException e) {
//                e.printStackTrace();
//                System.out.println("An IO error occurred when reading the data file");
//                return;
//            }
//        } else {
//            try {
//                writeDataToFile(allUserData, type, dataFile);
//            } catch (IOException e) {
//                e.printStackTrace();
//                System.out.println("An IO error occurred when writing to the data file");
//                return;
//            }
//        }

        // Get credentials
        File creds = new File("Credentials\\token.txt");
        String token;
        try (Scanner myReader = new Scanner(creds)) {
            token = myReader.nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("No credentials file found, ensure that a token.txt file exists within a \"Credentials\" subdirectory");
            return;
        }

        // Initialise the bot
        JDABuilder builder = JDABuilder.createDefault(token);
        try {
            builder.setActivity(Activity.playing("Type /confused"));
            jda = builder.build();
            jda.awaitReady();

            selfUser = jda.getSelfUser();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void addCommands(CommandContainer cmds) {
        // Add the commands (bot commands can take up to 1 hour to propagate; use Guild commands for testing)
        jda.addEventListener(cmds);
        CommandListUpdateAction cmdAction = jda.updateCommands();
        for (CommandData cmd : cmds.cmdList) {
            cmdAction.addCommands(cmd);
        }
        cmdAction.queue();
    }

    public ResultSet getAllUserData() {
        try (Connection conn = DriverManager.getConnection(connUrl)){
            return conn.createStatement().executeQuery("SELECT * FROM USERS");
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return null;
        }
    }

//    /**
//     * Simple utility function to write an object to file using GSON
//     */
//    public void writeDataToFile(Object data, Type type, Path path) throws IOException {
//        try (Writer writer = Files.newBufferedWriter(path)) {
//            new Gson().toJson(data, type, writer);
//            writer.flush();
//        }
//    }

    public static void main(String[] args){
        Bot bot = new Bot();
        bot.initialise();

        CommandContainer cmdContainer = new CommandContainer(bot);
        bot.addCommands(cmdContainer);
    }
}
