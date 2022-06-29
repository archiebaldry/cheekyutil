package xyz.archiebaldry.cheekyutil.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.archiebaldry.cheekyutil.CheekyUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class UnfriendCommand implements CommandExecutor {

    private final CheekyUtil PLUGIN = CheekyUtil.getPlugin(CheekyUtil.class);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { // Sender must be a player
            return false;
        }

        if (args.length == 0) { // No recipient passed
            return false;
        }

        // Get player data
        PersistentDataContainer playerData = player.getPersistentDataContainer();

        // Make friends key
        NamespacedKey friendsKey = new NamespacedKey(PLUGIN, "friends");

        // Get player's friends
        String friends = playerData.getOrDefault(friendsKey, PersistentDataType.STRING, "");

        // Get recipient's UUID
        UUID recipientUUID = Bukkit.getPlayerUniqueId(args[0]);

        if (recipientUUID == null) { // Recipient does not exist
            sender.sendMessage(Component.text("This player does not exist.").style(Style.style(NamedTextColor.RED)));
        } else if (friends.contains(recipientUUID.toString())) { // Recipient is on player's friends list
            ArrayList<String> friendsArrayList = new ArrayList<>(Arrays.asList(friends.split(",")));

            friendsArrayList.remove(recipientUUID.toString());

            if (friendsArrayList.isEmpty()) {
                playerData.remove(friendsKey);
            } else {
                playerData.set(friendsKey, PersistentDataType.STRING, String.join(",", friendsArrayList));
            }

            sender.sendMessage(Component.text("You are no longer friends with this player.").style(Style.style(NamedTextColor.GREEN)));
        } else { // Recipient is not on player's friends list
            sender.sendMessage(Component.text("You are already not friends with this player.").style(Style.style(NamedTextColor.RED)));
        }

        return true;
    }

}
