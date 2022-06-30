package xyz.archiebaldry.cheekyutil.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.*;
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

public class UntrustCommand implements CommandExecutor {

    private final CheekyUtil PLUGIN = CheekyUtil.getPlugin(CheekyUtil.class);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length == 0) {
            // Sender was not a player OR didn't pass <player>
            return false;
        }

        // Get world environment
        World.Environment environment = player.getWorld().getEnvironment();

        if (environment == World.Environment.THE_END) {
            // Command was used in The End
            player.sendMessage(Component.text("You cannot use this command in The End.").style(Style.style(NamedTextColor.RED)));
            return true;
        }

        // Get chunk
        Chunk chunk = player.getChunk();

        // Get chunk data
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        // Make owner key
        NamespacedKey ownerKey = new NamespacedKey(PLUGIN, "owner");

        // Get owner UUID as a string
        String ownerUUID = chunkData.get(ownerKey, PersistentDataType.STRING);

        // Get player UUID as a string
        String playerUUID = player.getUniqueId().toString();

        if (ownerUUID == null || !ownerUUID.equals(playerUUID)) {
            // Player does not own this chunk
            player.sendMessage(Component.text("You do not own this chunk.").style(Style.style(NamedTextColor.RED)));
            return true;
        }

        // Get recipient UUID
        UUID recipientUUID = Bukkit.getPlayerUniqueId(args[0]);

        if (recipientUUID == null) {
            // Recipient doesn't exist
            player.sendMessage(Component.text("This player does not exist.").style(Style.style(NamedTextColor.RED)));
            return true;
        }

        // Make trusted key
        NamespacedKey trustedKey = new NamespacedKey(PLUGIN, "trusted");

        // Get trusted UUIDs as a string
        String trustedUUIDs = chunkData.getOrDefault(trustedKey, PersistentDataType.STRING, "");

        if (!trustedUUIDs.contains(recipientUUID.toString())) {
            // Recipient is not trusted
            player.sendMessage(Component.text("This player is not trusted.").style(Style.style(NamedTextColor.RED)));
            return true;
        }

        // Split trusted UUIDs into an ArrayList of strings
        ArrayList<String> trusted = new ArrayList<>(Arrays.asList(trustedUUIDs.split(",")));

        // Remove recipient from trusted ArrayList
        trusted.remove(recipientUUID.toString());

        // Untrust recipient
        if (trusted.isEmpty()) {
            chunkData.remove(trustedKey);
        } else {
            chunkData.set(trustedKey, PersistentDataType.STRING, String.join(",", trusted));
        }

        // Send success message
        player.sendMessage(Component.text(args[0] + " has been untrusted.").style(Style.style(NamedTextColor.GREEN)));

        return true;
    }

}
