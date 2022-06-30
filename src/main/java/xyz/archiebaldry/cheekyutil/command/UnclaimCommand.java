package xyz.archiebaldry.cheekyutil.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.archiebaldry.cheekyutil.CheekyUtil;

import java.util.UUID;

public class UnclaimCommand implements CommandExecutor {

    private final CheekyUtil PLUGIN = CheekyUtil.getPlugin(CheekyUtil.class);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            // Sender was not a player
            sender.sendMessage(Component.text("Only players can perform this command.").style(Style.style(NamedTextColor.RED)));

            return true;
        }

        World.Environment environment = player.getWorld().getEnvironment(); // Get environment

        if (environment == World.Environment.THE_END) {
            // Command was used in The End
            player.sendMessage(Component.text("You cannot use this command in The End.").style(Style.style(NamedTextColor.RED)));

            return true;
        }

        String playerUUID = player.getUniqueId().toString(); // Get player UUID

        PersistentDataContainer playerData = player.getPersistentDataContainer(); // Get player data

        NamespacedKey chunksKey = new NamespacedKey(PLUGIN, "chunks"); // Make chunks key

        int chunks = playerData.getOrDefault(chunksKey, PersistentDataType.INTEGER, 0); // Get chunks

        PersistentDataContainer chunkData = player.getChunk().getPersistentDataContainer(); // Get chunk data

        NamespacedKey ownerKey = new NamespacedKey(PLUGIN, "owner"); // Make owner key

        String ownerUUID = chunkData.get(ownerKey, PersistentDataType.STRING); // Get owner UUID

        if (ownerUUID == null) {
            // The chunk is unclaimed
            player.sendMessage(Component.text("Nobody owns this chunk.").style(Style.style(NamedTextColor.YELLOW)));
        } else if (ownerUUID.equals(playerUUID)) {
            // The player owns this chunk
            chunkData.remove(ownerKey); // Remove owner

            chunkData.remove(new NamespacedKey(PLUGIN, "trusted")); // Remove any trusted players

            if (chunks > 1) { // Player is unclaiming 2nd or onwards chunk
                // Give 8 emeralds
                player.getInventory().addItem(new ItemStack(Material.EMERALD, 8));
            }

            playerData.set(chunksKey, PersistentDataType.INTEGER, chunks - 1);

            player.sendMessage(Component.text("You unclaimed this chunk. You will receive 8 emeralds provided this was not your only chunk.").style(Style.style(NamedTextColor.GREEN)));
        } else {
            // Somebody else owns this chunk
            String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName(); // Get owner name

            player.sendMessage(Component.text(ownerName + " owns this chunk.").style(Style.style(NamedTextColor.RED)));
        }

        return true;
    }

}
