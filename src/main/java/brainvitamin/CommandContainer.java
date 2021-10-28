package brainvitamin;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommandContainer extends ListenerAdapter {
    public List<CommandData> cmdList = new ArrayList<>();
    private Bot bot;
    private User selfUser;
    private ResultSet allUserData;

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
//        cmdList.add(new CommandData("leaderboard", "Display the leaderboard of confusion"));

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

            long times_confused = 1;

            // Check if user exists within allUserData
            try (Connection conn = DriverManager.getConnection(bot.getConnUrl())){
                int idCount = getIdCount(conn, memberId);
                if (idCount == 1) { // Increment count for existing user
                    ResultSet rs = getData(conn, "SELECT * FROM USERS WHERE ID = " + memberId);
                    rs.next();
                    times_confused = rs.getLong("TIMES_CONFUSED") + 1;
                    updateData(conn, "UPDATE USERS " +
                            "SET TIMES_CONFUSED = " + times_confused +
                            " WHERE ID = " + memberId);
                } else { // Insert new user
                    updateData(conn, String.format("INSERT INTO USERS " +
                            "(ID, NAME, TIMES_CONFUSED) " +
                            " VALUES (%d, \"%s\", %d)", memberId, name, times_confused));
                }
                String reply = name + " hurt itself in its own confusion! \n" + times_confused + " times!";
                event.getHook().sendMessage(reply).queue();
            } catch (DuplicatedIdException e){
                String reply = "Error: More than one of this ID exists as an entry. If this gets printed, I fucked up.";
                event.getHook().sendMessage(reply).queue();
                System.out.println(reply);
                return;
            } catch (SQLException e) {
                System.out.println("SQLException: " + e.getMessage());
                e.printStackTrace();
                return;
            }

//            if (allUserData == null) allUserData = new HashMap<>();
//
//            // Check if user exists within allUserData
//            if (allUserData.containsKey(memberId)) {
//                UserData userData = allUserData.get(memberId);
//                if (userData.dataContainsKey("confused_n")) {
//                    confused_n = ((Number) userData.getData("confused_n")).longValue();
//                    userData.setData("confused_n", ++confused_n);
//                }
//                else userData.setData("confused_n", confused_n);
//            } else { // If user doesn't exist in allUserData yet, add the user and initialise the confusion count
//                UserData userData = new UserData(memberId, name, new HashMap<>());
//                userData.setData("confused_n", confused_n);
//                allUserData.put(memberId, userData);
//            }
//
//            // Write the update to file and send the message
//            try {
//                bot.writeDataToFile(allUserData, bot.type, bot.dataFile);
//
//                String reply = name + " hurt itself in its own confusion! \n" + confused_n + " times!";
//                event.getHook().sendMessage(reply).queue();
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
        }

        // userStats command
        if (event.getName().equals("user_stats")) {
            event.deferReply().queue();
            event.getChannel().sendTyping().queue();

            Member member = event.getOption("user").getAsMember();
            String name = member.getEffectiveName();
            String memberId = member.getId();

            try (Connection conn = DriverManager.getConnection(bot.getConnUrl())){
                int idCount = getIdCount(conn, memberId);

                if (idCount == 0) { // User does not exist in DB yet, insert new entry
                    updateData(conn, String.format("INSERT INTO USERS " +
                            "(ID, NAME) " +
                            " VALUES (%d, \"%s\", %d)", memberId, name));
                }

                // Get user stats and send message
                ResultSet rs = getData(conn, "SELECT * FROM USERS WHERE ID = " + memberId);
                rs.next();
                StringBuilder replySb = new StringBuilder("Stats for " + name + "\n");
                replySb.append(getUserStatsAsString(rs));
                event.getHook().sendMessage(replySb.toString()).queue();

            } catch (DuplicatedIdException e){
                String reply = "Error: More than one of this ID exists as an entry. If this gets printed, I fucked up.";
                event.getHook().sendMessage(reply).queue();
                System.out.println(reply);
                return;
            } catch (SQLException e) {
                System.out.println("SQLException: " + e.getMessage());
                e.printStackTrace();
                return;
            }

//            if (allUserData == null) {
//                event.getHook().sendMessage("Something has went wrong - no user data has been detected").queue();
//                return;
//            }
//
//            if (allUserData.containsKey(memberId)) {
//                UserData userData = allUserData.get(memberId);
//                if (userData.getName() != name) {
//                    userData.setName(name);
//                }
//                event.getHook().sendMessage(userData.userStatsToString()).queue();
//            } else {
//                event.getHook().sendMessage("No data for " + name + " has been recorded yet.").queue();
//            }
        }
    }

    /**
     * Convenience method to perform database query
     * @param conn SQL Connection to relevant database
     * @param sql SQL query string
     * @return ResultSet corresponding to query or null if SQLException is caught
     */
    private ResultSet getData(Connection conn, String sql) throws SQLException{
        System.out.println(sql);
        return conn.createStatement().executeQuery(sql);
    }

    /**
     * Convenience method to perform database update
     * @param conn SQL Connection to relevant database
     * @param sql SQL query string
     */
    private void updateData(Connection conn, String sql) throws SQLException{
        System.out.println(sql);
        conn.createStatement().executeUpdate(sql);
    }

    /**
     * Used to return count of ID
     * @param conn SQL Connection to user database
     * @param id ID to check for
     * @return int count of ID found, should only be 0 or 1 under normal database operation
     * @throws DuplicatedIdException if more than one of the provided ID is found in the database
     */
    private int getIdCount(Connection conn, String id) throws SQLException{
        ResultSet countRs = getData(conn, "SELECT COUNT(*) FROM USERS WHERE ID = " + id);
        countRs.next();
        int idCount = countRs.getInt(1);

        if (idCount > 1) throw new DuplicatedIdException(id);

        return idCount;
    }

    private String getUserStatsAsString(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ResultSetMetaData rsmd = rs.getMetaData();

        int count = rsmd.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String colName = rsmd.getColumnName(i);
            if (colName.equals("ID") || colName.equals("NAME")) continue;
            sb.append(rsmd.getColumnName(i) + " : " + rs.getString(i));
        }
        return sb.toString();
    }
}
