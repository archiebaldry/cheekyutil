package xyz.archiebaldry.cheekyutil.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.archiebaldry.cheekyutil.CheekyUtil;

import java.util.Objects;
import java.util.UUID;

public class ChunkInfoCommand implements CommandExecutor {

    private final CheekyUtil PLUGIN = CheekyUtil.getPlugin(CheekyUtil.class);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            // Sender was not a player
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

        if (ownerUUID == null) {
            // Chunk is unclaimed
            player.sendMessage(Component.text("This chunk is unclaimed.").style(Style.style(NamedTextColor.GREEN)));
            return true;
        }

        // Send header message
        player.sendMessage(Component.text(String.format("Chunk %d %d", chunk.getX(), chunk.getZ())).style(Style.style(NamedTextColor.GOLD)));

        // Get owner name
        String owner = Objects.requireNonNullElse(Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName(), ownerUUID);

        // Send owner message
        player.sendMessage(Component.text("Owner: ").append(Component.text(owner).style(Style.style(NamedTextColor.YELLOW))));

        // Make trusted key
        NamespacedKey trustedKey = new NamespacedKey(PLUGIN, "trusted");

        // Get trusted UUIDs as a string
        String trustedUUIDs = chunkData.get(trustedKey, PersistentDataType.STRING);

        if (trustedUUIDs == null) {
            // Chunk has no trusted players
            // Send trusted message
            player.sendMessage(Component.text("Trusted: ").append(Component.text("No trusted players").style(Style.style(NamedTextColor.YELLOW))));
        } else {
            // Chunk has trusted players
            // Split trusted UUIDs into a string array
            String[] trusted = trustedUUIDs.split(",");

            for (int i = 0; i < trusted.length; i++) {
                // Replace every trusted UUID with username
                trusted[i] = Objects.requireNonNullElse(Bukkit.getOfflinePlayer(UUID.fromString(trusted[i])).getName(), ownerUUID);
            }

            // Send trusted message
            player.sendMessage(Component.text("Trusted: ").append(Component.text(String.join(", ", trusted)).style(Style.style(NamedTextColor.YELLOW))));
        }

        return true;
    }

}
