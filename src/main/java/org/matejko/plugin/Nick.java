package main.java.org.matejko.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

public class Nick extends JavaPlugin {
    private static Logger logger;
    private NickColorManager nickColorManager;
    private Map<String, String> originalNicknames;
    private Map<String, String> usedNicknames;
    private Map<String, Long> nicknameCooldowns; // Cooldown for nickname command
    private Map<String, Long> colorCooldowns;    // Cooldown for color command
    private Map<String, Long> resetCooldowns;     // Cooldown for reset command

    @Override
    public void onEnable() {
        logger = Logger.getLogger("logger");
        nickColorManager = new NickColorManager(this);
        usedNicknames = new HashMap<>();
        originalNicknames = new HashMap<>();
        nicknameCooldowns = new HashMap<>();
        colorCooldowns = new HashMap<>();
        resetCooldowns = new HashMap<>();

        getCommand("nickname").setExecutor(this);
        getCommand("rename").setExecutor(this);
        getCommand("color").setExecutor(this);
        getCommand("nickreset").setExecutor(this);
        getCommand("realname").setExecutor(this);

        getServer().getPluginManager().registerEvents(nickColorManager, this);

        loadUsedNicknames();
        logger.info("Nick plugin enabled.");
    }

    @Override
    public void onDisable() {
        saveUsedNicknames();
        logger.info("Nick plugin disabled.");
    }

    private void loadUsedNicknames() {
        File usedNicknamesFile = new File(getDataFolder(), "usedNicknames.txt");
        if (usedNicknamesFile.exists()) {
            try (Scanner scanner = new Scanner(usedNicknamesFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String playerName = parts[0];
                            String nickname = parts[1];
                            usedNicknames.put(playerName, nickname);
                            originalNicknames.put("~" + nickname, playerName); // Store original name with nickname prefixed by ~
                        }
                    }
                }
            } catch (IOException e) {
                logger.severe("Could not read used nicknames file.");
                e.printStackTrace();
            }
        }
    }

    private void saveUsedNicknames() {
        File usedNicknamesFile = new File(getDataFolder(), "usedNicknames.txt");
        try {
            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, String> entry : usedNicknames.entrySet()) {
                content.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            java.nio.file.Files.write(usedNicknamesFile.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            logger.severe("Could not save used nicknames to file.");
            e.printStackTrace();
        }
    }

    private boolean isOnNicknameCooldown(Player player) {
        Long lastUsed = nicknameCooldowns.get(player.getName());
        return lastUsed != null && System.currentTimeMillis() < lastUsed + TimeUnit.SECONDS.toMillis(15);
    }

    private boolean isOnColorCooldown(Player player) {
        Long lastUsed = colorCooldowns.get(player.getName());
        return lastUsed != null && System.currentTimeMillis() < lastUsed + TimeUnit.SECONDS.toMillis(15);
    }

    private boolean isOnResetCooldown(Player player) {
        Long lastUsed = resetCooldowns.get(player.getName());
        return lastUsed != null && System.currentTimeMillis() < lastUsed + TimeUnit.SECONDS.toMillis(15);
    }

    private long getRemainingNicknameCooldown(Player player) {
        Long lastUsed = nicknameCooldowns.get(player.getName());
        return lastUsed != null ? (lastUsed + TimeUnit.SECONDS.toMillis(15) - System.currentTimeMillis()) / 1000 : 0;
    }

    private long getRemainingColorCooldown(Player player) {
        Long lastUsed = colorCooldowns.get(player.getName());
        return lastUsed != null ? (lastUsed + TimeUnit.SECONDS.toMillis(15) - System.currentTimeMillis()) / 1000 : 0;
    }

    private long getRemainingResetCooldown(Player player) {
        Long lastUsed = resetCooldowns.get(player.getName());
        return lastUsed != null ? (lastUsed + TimeUnit.SECONDS.toMillis(15) - System.currentTimeMillis()) / 1000 : 0;
    }

    private void setNicknameCooldown(Player player) {
        nicknameCooldowns.put(player.getName(), System.currentTimeMillis());
    }

    private void setColorCooldown(Player player) {
        colorCooldowns.put(player.getName(), System.currentTimeMillis());
    }

    private void setResetCooldown(Player player) {
        resetCooldowns.put(player.getName(), System.currentTimeMillis());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "nickname":
                return handleNicknameCommand(sender, args);
            case "rename":
                return handleRenameCommand(sender, args);
            case "color":
                return handleColorCommand(sender, args);
            case "realname":
                return handleRealNameCommand(sender, args);
            case "nickreset":
                return handleNickResetCommand(sender);
            default:
                return false;
        }
    }

    private boolean handleNicknameCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return false;
        }

        Player player = (Player) sender;

        if (isOnNicknameCooldown(player)) {
            long remaining = getRemainingNicknameCooldown(player);
            player.sendMessage(ChatColor.RED + "You must wait " + remaining + " seconds before using /nickname again.");
            return false;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /nickname <nickname>");
            return false;
        }

        String nickname = args[0];
        String currentNickname = usedNicknames.get(player.getName());
        if (currentNickname != null) {
            usedNicknames.remove(player.getName());
        }

        if (usedNicknames.containsValue(nickname)) {
            player.sendMessage(ChatColor.RED + "The nickname " + "~" + nickname + " is already in use.");
            return false;
        }

        nickColorManager.setPlayerNickname(player, nickname);
        usedNicknames.put(player.getName(), nickname);
        saveUsedNicknames();

        player.sendMessage(ChatColor.GOLD + "Your nickname has been set to " + "~" + nickname + ".");
        setNicknameCooldown(player); // Set nickname cooldown
        return true;
    }

    private boolean handleRenameCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nicks.rename.others")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to rename other players.");
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /rename <player> <nickname>");
            return false;
        }

        String targetPlayerName = args[0];
        String newNickname = args[1];

        Player targetPlayer = getServer().getPlayerExact(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        if (usedNicknames.containsValue(newNickname)) {
            player.sendMessage(ChatColor.RED + "The nickname '" + "~" + newNickname + "' is already in use.");
            return false;
        }

        nickColorManager.setPlayerNickname(targetPlayer, newNickname);
        usedNicknames.put(targetPlayerName, newNickname);
        saveUsedNicknames();

        player.sendMessage(ChatColor.GRAY + "You have renamed " + targetPlayerName + " to " + "~" + newNickname + ".");
        targetPlayer.sendMessage(ChatColor.GRAY + "You have been renamed to " + "~" + newNickname + ".");
        return true;
    }

    private boolean handleColorCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can change their color.");
            return false;
        }

        Player player = (Player) sender;

        if (isOnColorCooldown(player)) {
            long remaining = getRemainingColorCooldown(player);
            player.sendMessage(ChatColor.RED + "You must wait " + remaining + " seconds before using /color again.");
            return false;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /color <color> or /color help");
            return false;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(ChatColor.GOLD + "Available nickname colors: " +
                    ChatColor.BLACK + "black, " + ChatColor.DARK_BLUE + "dark_blue, " +
                    ChatColor.DARK_GREEN + "dark_green, " + ChatColor.DARK_AQUA + "dark_aqua, " +
                    ChatColor.DARK_RED + "dark_red, " + ChatColor.DARK_PURPLE + "dark_purple, " +
                    ChatColor.GOLD + "gold, " + ChatColor.GRAY + "gray, " +
                    ChatColor.DARK_GRAY + "dark_gray, " + ChatColor.BLUE + "blue, " +
                    ChatColor.GREEN + "green, " + ChatColor.AQUA + "aqua, " +
                    ChatColor.RED + "red, " + ChatColor.LIGHT_PURPLE + "light_purple, " +
                    ChatColor.YELLOW + "yellow, " + ChatColor.WHITE + "white");
            return true;
        }

        String color = args[0];

        if (!NickColorManager.isValidColor(color)) {
            player.sendMessage(ChatColor.RED + "Invalid color. Use /color help to see available colors.");
            return false;
        }

        nickColorManager.setPlayerColor(player, color);
        setColorCooldown(player); // Set color cooldown
        player.sendMessage(ChatColor.GOLD + "Your color has been set to " + ChatColor.valueOf(color.toUpperCase()) + color + ".");
        return true;
    }

    private boolean handleRealNameCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /realname <nickname>");
            return false;
        }

        String nickname = "~" + args[0]; // Automatically add ~ in front
        String realName = originalNicknames.get(nickname); // Find the real name

        if (realName != null) {
            String color = nickColorManager.getNicknameColor(nickname); // Get the color for the nickname
            sender.sendMessage(ChatColor.GOLD + "Real name of " + ChatColor.valueOf(color.toUpperCase()) + nickname + (ChatColor.GOLD + " is ") + realName + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "No original name found for " + nickname + ".");
        }

        return true;
    }

    private boolean handleNickResetCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return false;
        }

        Player player = (Player) sender;

        if (isOnResetCooldown(player)) {
            long remaining = getRemainingResetCooldown(player);
            player.sendMessage(ChatColor.RED + "You must wait " + remaining + " seconds before using /nickreset again.");
            return false;
        }

        // Reset player's nickname and color
        String playerName = player.getName();
        usedNicknames.remove(playerName);
        nickColorManager.resetPlayerNickname(player);
        sender.sendMessage(ChatColor.GREEN + "Your nickname has been reset.");
        setResetCooldown(player); // Set reset cooldown
        return true;
    }
}
