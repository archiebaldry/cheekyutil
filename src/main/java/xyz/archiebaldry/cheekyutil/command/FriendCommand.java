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

import java.util.UUID;

public class FriendCommand implements CommandExecutor {

    private final CheekyUtil PLUGIN = CheekyUtil.getPlugin(CheekyUtil.class);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { // Sender must be a player
            return false;
        }

        // Get player data
        PersistentDataContainer playerData = player.getPersistentDataContainer();

        // Make friends key
        NamespacedKey friendsKey = new NamespacedKey(PLUGIN, "friends");

        if (args.length == 0) { // No recipient passed so list friends
            // Get player's friends
            String friends = playerData.get(friendsKey, PersistentDataType.STRING);

            if (friends == null) { // Player has no friends
                sender.sendMessage(Component.text("You have no friends.").style(Style.style(NamedTextColor.RED)));
                return true;
            }

            String[] friendsArray = friends.split(",");

            for (int i = 0; i < friendsArray.length; i++) {
                UUID uuid;

                try {
                    uuid = UUID.fromString(friendsArray[i]);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                Player p = Bukkit.getPlayer(uuid);

                if (p != null) {
                    friendsArray[i] = p.getName();
                }
            }

            sender.sendMessage(Component.text("Friends: ").append(Component.text(String.join(", ", friendsArray)).style(Style.style(NamedTextColor.YELLOW))));

            return true;
        }

        if (args[0].equals(player.getName())) {
            sender.sendMessage(Component.text("You cannot add yourself as a friend.").style(Style.style(NamedTextColor.RED)));
            return true;
        }

        // Get recipient
        Player recipient = Bukkit.getPlayer(args[0]);

        if (recipient == null) { // Recipient is offline
            sender.sendMessage(Component.text("This player is offline.").style(Style.style(NamedTextColor.RED)));
            return true;
        }

        // Get player's friends
        String friends = playerData.getOrDefault(friendsKey, PersistentDataType.STRING, "");

        // Get recipient's UUID as a string
        String recipientUUID = recipient.getUniqueId().toString();

        if (friends.contains(recipientUUID)) { // Player already has recipient as a friend
            sender.sendMessage(Component.text("You are already friends with this player.").style(Style.style(NamedTextColor.RED)));
        } else { // They are not already friends
            if (friends.isEmpty()) {
                playerData.set(friendsKey, PersistentDataType.STRING, recipientUUID);
            } else {
                playerData.set(friendsKey, PersistentDataType.STRING, friends + "," + recipientUUID);
            }

            sender.sendMessage(Component.text("You are now friends with this player.").style(Style.style(NamedTextColor.GREEN)));
        }

        return true;
    }

}
