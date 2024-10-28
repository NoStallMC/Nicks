package main.java.org.matejko.plugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NickColorManager implements Listener {
    private static Logger logger;
    private final Nick plugin;
    private File playerDataFile;
    private Map<String, String[]> playerData;

    public NickColorManager(Nick plugin) {
        this.plugin = plugin;
        this.logger = Logger.getLogger("logger");
        this.playerData = new HashMap<>();

        File nicksFolder = plugin.getDataFolder();
        if (!nicksFolder.exists()) {
            nicksFolder.mkdirs();
        }

        this.playerDataFile = new File(nicksFolder, "playerData.txt");
        loadPlayerData();
    }

    private void loadPlayerData() {
        if (playerDataFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(playerDataFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=");
                    if (parts.length == 3) {
                        String playerName = parts[0];
                        String nickname = parts[1];
                        String color = parts[2];
                        playerData.put(playerName, new String[] { nickname, color });
                    }
                }
            } catch (IOException e) {
                logger.severe("Could not read player data file.");
            }
        }
    }

    private void savePlayerData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(playerDataFile))) {
            for (Map.Entry<String, String[]> entry : playerData.entrySet()) {
                String playerName = entry.getKey();
                String[] data = entry.getValue();
                writer.write(playerName + "=" + data[0] + "=" + data[1]);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.severe("Could not save player data file.");
        }
    }

    public void setPlayerNickname(Player player, String nickname) {
        String currentColor = getPlayerColor(player);
        String nickWithTilde = "~" + nickname;
        try {
            player.setDisplayName(ChatColor.valueOf(currentColor.toUpperCase()) + nickWithTilde + ChatColor.WHITE);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid color: " + currentColor);
            player.sendMessage(ChatColor.RED + "Invalid color specified.");
            return;
        }
        playerData.put(player.getName(), new String[] { nickWithTilde, currentColor });
        savePlayerData();
    }

    public String getOriginalNickname(Player player) {
        String[] data = playerData.get(player.getName());
        return (data != null && data[0] != null) ? data[0] : null;
    }

    public void setPlayerColor(Player player, String color) {
        String currentNickname = getPlayerNickname(player);
        try {
            player.setDisplayName(ChatColor.valueOf(color.toUpperCase()) + currentNickname + ChatColor.WHITE);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid color: " + color);
            player.sendMessage(ChatColor.RED + "Invalid color specified.");
            return;
        }
        playerData.put(player.getName(), new String[] { currentNickname, color });
        savePlayerData();
    }

    public void setPlayerColor(Player player, String color, String originalName) {
        try {
            player.setDisplayName(ChatColor.valueOf(color.toUpperCase()) + originalName + ChatColor.WHITE);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid color: " + color);
            player.sendMessage(ChatColor.RED + "Invalid color specified.");
            return;
        }
        playerData.put(player.getName(), new String[] { originalName, color });
        savePlayerData();
    }

    public void resetPlayerNickname(Player player) {
        String originalName = player.getName();
        player.setDisplayName(originalName);
        playerData.put(player.getName(), new String[] { null, "WHITE" }); // Resetting nickname and color
        savePlayerData();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        String[] data = playerData.get(playerName);
        if (data != null) {
            String nickname = data[0];
            String color = data[1];
            player.setDisplayName(ChatColor.valueOf(color.toUpperCase()) + nickname + ChatColor.WHITE);
        }
    }

    public String getPlayerColor(Player player) {
        String[] data = playerData.get(player.getName());
        return (data != null && data[1] != null) ? data[1] : "WHITE";
    }

    public String getPlayerNickname(Player player) {
        String[] data = playerData.get(player.getName());
        return (data != null && data[0] != null) ? data[0] : player.getName();
    }

    public String getNicknameColor(String nickname) {
        // Iterate through player data to find the color associated with the nickname
        for (Map.Entry<String, String[]> entry : playerData.entrySet()) {
            String[] data = entry.getValue();
            if (data[0].equals(nickname)) {
                return data[1]; // Return the color associated with this nickname
            }
        }
        return "WHITE"; // Default color if not found
    }

    public static boolean isValidColor(String color) {
        try {
            ChatColor.valueOf(color.toUpperCase());
            return isValidColorName(ChatColor.valueOf(color.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValidColorName(ChatColor color) {
        switch (color) {
            case BLACK: case DARK_BLUE: case DARK_GREEN: case DARK_AQUA: case DARK_RED: case DARK_PURPLE: case GOLD:
            case GRAY: case DARK_GRAY: case BLUE: case GREEN: case AQUA: case RED: case LIGHT_PURPLE: case YELLOW: case WHITE:
                return true;
            default:
                return false;
        }
    }
}
