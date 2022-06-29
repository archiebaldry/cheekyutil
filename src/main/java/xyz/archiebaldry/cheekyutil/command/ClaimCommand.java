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

public class ClaimCommand implements CommandExecutor {

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
            if (chunks == 0) { // First chunk is free
                player.sendMessage(Component.text("You claimed your first chunk.").style(Style.style(NamedTextColor.GREEN)));

                playerData.set(chunksKey, PersistentDataType.INTEGER, 1);

                chunkData.set(ownerKey, PersistentDataType.STRING, playerUUID); // Set owner to player
            } else if (player.getInventory().containsAtLeast(new ItemStack(Material.EMERALD), 5)) { // 2nd chunk and onwards costs 5 emeralds
                player.sendMessage(Component.text("You claimed this chunk for 5 emeralds.").style(Style.style(NamedTextColor.GREEN)));

                ItemStack[] contents = player.getInventory().getStorageContents();

                int leftToRemove = 5;

                for (int i = 0; i < contents.length; i++) {
                    ItemStack stack = contents[i];

                    if (stack == null || stack.getType() != Material.EMERALD) {
                        continue;
                    }

                    if (stack.getAmount() < leftToRemove) {
                        leftToRemove -= stack.getAmount();

                        player.getInventory().setItem(i, null);
                    } else if (stack.getAmount() == leftToRemove) {
                        player.getInventory().setItem(i, null);

                        break;
                    } else {
                        player.getInventory().setItem(i, stack.asQuantity(stack.getAmount() - leftToRemove));

                        break;
                    }
                }

                playerData.set(chunksKey, PersistentDataType.INTEGER, chunks + 1);

                chunkData.set(ownerKey, PersistentDataType.STRING, playerUUID); // Set owner to player
            } else {
                player.sendMessage(Component.text("You need 5 emeralds to claim this chunk.").style(Style.style(NamedTextColor.RED)));
            }
        } else if (ownerUUID.equals(playerUUID)) {
            // The player already owns this chunk
            player.sendMessage(Component.text("You already own this chunk.").style(Style.style(NamedTextColor.YELLOW)));
        } else {
            // Somebody else owns this chunk
            String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName(); // Get owner name

            player.sendMessage(Component.text(ownerName + " owns this chunk.").style(Style.style(NamedTextColor.RED)));
        }

        return true;
    }

}
