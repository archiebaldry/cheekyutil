package xyz.archiebaldry.cheekyutil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.archiebaldry.cheekyutil.command.*;

public final class CheekyUtil extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register listener
        getServer().getPluginManager().registerEvents(new CheekyUtilListener(), this);

        // Get commands
        PluginCommand friendCommand = getCommand("friend");
        PluginCommand unfriendCommand = getCommand("unfriend");
        PluginCommand claimCommand = getCommand("claim");
        PluginCommand unclaimCommand = getCommand("unclaim");
        PluginCommand trustCommand = getCommand("trust");
        PluginCommand untrustCommand = getCommand("untrust");
        PluginCommand deathCommand = getCommand("death");

        // Set command executors
        if (friendCommand != null) friendCommand.setExecutor(new FriendCommand());
        if (unfriendCommand != null) unfriendCommand.setExecutor(new UnfriendCommand());
        if (claimCommand != null) claimCommand.setExecutor(new ClaimCommand());
        if (unclaimCommand != null) unclaimCommand.setExecutor(new UnclaimCommand());
        if (trustCommand != null) trustCommand.setExecutor(new TrustCommand());
        if (untrustCommand != null) untrustCommand.setExecutor(new UntrustCommand());
        if (deathCommand != null) deathCommand.setExecutor(new DeathCommand());
    }

    public void sendDeathMessage(Player player) {
        // Get last death location
        Location loc = player.getLastDeathLocation();

        if (loc == null) { // Player does not have a last death
            // Send message
            player.sendMessage(Component.text("You have not died yet.").style(Style.style(NamedTextColor.YELLOW)));
        } else { // Player has a last death
            // Get world type
            String worldType;

            switch (loc.getWorld().getEnvironment()) {
                case NETHER -> worldType = "Nether";
                case NORMAL -> worldType = "Overworld";
                case THE_END -> worldType = "End";
                default -> worldType = "Custom";
            }

            // Send message
            player.sendMessage(Component.text(String.format("Last death: %d %d %d (%s)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), worldType)).style(Style.style(NamedTextColor.YELLOW)));
        }

    }

    public boolean areFriends(Player player1, Player player2) {
        // Get player data
        PersistentDataContainer data1 = player1.getPersistentDataContainer();
        PersistentDataContainer data2 = player2.getPersistentDataContainer();

        // Make friends key
        NamespacedKey friendsKey = new NamespacedKey(this, "friends");

        // 1 has 2 in their friends list
        boolean flag1 = data1.getOrDefault(friendsKey, PersistentDataType.STRING, "").contains(player2.getUniqueId().toString());

        // 2 has 1 in their friends list
        boolean flag2 = data2.getOrDefault(friendsKey, PersistentDataType.STRING, "").contains(player1.getUniqueId().toString());

        return flag1 && flag2;
    }

}
