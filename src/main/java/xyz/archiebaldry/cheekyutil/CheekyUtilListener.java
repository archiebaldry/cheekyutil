package xyz.archiebaldry.cheekyutil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CheekyUtilListener implements Listener {

    private final CheekyUtil PLUGIN = CheekyUtil.getPlugin(CheekyUtil.class);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Send death message
        PLUGIN.sendDeathMessage(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Get player and death location
        Player player = event.getPlayer();
        Location location = player.getLocation();

        // Spawn skeleton corpse
        Skeleton corpse = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);

        // Set corpse's name to player's name
        corpse.customName(Component.text(player.getName()).style(Style.style(NamedTextColor.GRAY)));

        // Disable corpse's AI, enable invulnerability and disable burning
        corpse.setAI(false);
        corpse.setInvulnerable(true);
        corpse.setShouldBurnInDay(false);

        // Get player's inventory and experience
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armour = inventory.getArmorContents();
        ItemStack[] storage = inventory.getStorageContents();
        int level = player.getLevel();
        float levelProgress = player.getExp();

        // Get corpse's inventory and data container
        EntityEquipment corpseInventory = corpse.getEquipment();
        PersistentDataContainer corpseData = corpse.getPersistentDataContainer();

        // Internally label the skeleton as a corpse
        corpseData.set(new NamespacedKey(PLUGIN, "is_corpse"), PersistentDataType.INTEGER, 1);

        // Set corpse's armour, main hand and off hand
        corpseInventory.setArmorContents(armour);
        corpseInventory.setItemInMainHand(inventory.getItemInMainHand());
        corpseInventory.setItemInOffHand(inventory.getItemInOffHand());

        // Store items (excluding armour and off hand) in corpse data
        for (int i = 0; i < storage.length; i++) {
            // Get item stack at slot i
            ItemStack itemStack = storage[i];

            if (itemStack == null) { // Slot i is empty
                continue;
            }

            // Store item stack as byte array
            corpseData.set(new NamespacedKey(PLUGIN, "slot" + i), PersistentDataType.BYTE_ARRAY, itemStack.serializeAsBytes());
        }

        // Store experience in corpse data
        corpseData.set(new NamespacedKey(PLUGIN, "level"), PersistentDataType.INTEGER, level);
        corpseData.set(new NamespacedKey(PLUGIN, "level_progress"), PersistentDataType.FLOAT, levelProgress);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Send death message
        PLUGIN.sendDeathMessage(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            // Ignore off hand packet
            // https://www.spigotmc.org/threads/how-would-i-stop-an-event-from-being-called-twice.135234/#post-1434104
            return;
        }

        if (!(event.getRightClicked() instanceof Skeleton corpse)) {
            // Entity is not a skeleton
            return;
        }

        // Get corpse data
        PersistentDataContainer corpseData = corpse.getPersistentDataContainer();

        if (!corpseData.has(new NamespacedKey(PLUGIN, "is_corpse"))) {
            // Skeleton is not internally labelled as corpse
            return;
        }

        // Get player, their inventory and their armour
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armour = inventory.getArmorContents();

        // Get corpse inventory and experience
        EntityEquipment corpseInventory = corpse.getEquipment();
        ItemStack[] corpseArmour = corpseInventory.getArmorContents();
        int corpseLevel = corpseData.getOrDefault(new NamespacedKey(PLUGIN, "level"), PersistentDataType.INTEGER, 0);
        // TODO: float corpseLevelProgress = corpseData.getOrDefault(new NamespacedKey(PLUGIN, "level_progress"), PersistentDataType.FLOAT, 0F);

        // Create empty list for items that didn't fit
        List<ItemStack> failed = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            // Get armour piece currently on player
            ItemStack onPlayer = armour[i];

            if (onPlayer == null || onPlayer.getType() == Material.AIR) { // Player has no armour in that slot
                // Set armour piece to whatever's on corpse (could be air)
                armour[i] = corpseArmour[i];
            } else { // Player already had armour in that slot
                // Add armour piece on corpse to list of failed items
                failed.add(corpseArmour[i]);
            }

            // Remove armour piece from corpse
            corpseArmour[i] = null;
        }

        // Update player armour
        inventory.setArmorContents(armour);

        if (inventory.getItemInOffHand().getType() == Material.AIR) { // Player has nothing in their off hand
            // Set player off hand to corpse off hand (could be air)
            inventory.setItemInOffHand(corpseInventory.getItemInOffHand());
        } else { // Player already had something in their off hand
            // Add corpse off hand to list of failed items
            failed.add(corpseInventory.getItemInOffHand());
        }

        for (int i = 0; i < 36; i++) {
            // Create key
            NamespacedKey key = new NamespacedKey(PLUGIN, "slot" + i);

            // Get item (serialised) in corpse inventory at that slot
            byte[] serialised = corpseData.get(key, PersistentDataType.BYTE_ARRAY);

            if (serialised == null) { // Corpse has nothing in that slot
                continue;
            }

            // Deserialise item stored in corpse inventory at that slot
            ItemStack item = ItemStack.deserializeBytes(serialised);

            // Add item to player inventory
            HashMap<Integer, ItemStack> result = inventory.addItem(item);

            // Add every item that didn't fit to list of failed items
            failed.addAll(result.values());
        }

        for (ItemStack item : failed) {
            // Drop every failed item on the floor at the player's location
            player.getWorld().dropItem(player.getLocation(), item);
        }

        // Add corpse's experience to player
        player.giveExpLevels(corpseLevel);
        // TODO: Give player level progress

        // Remove corpse from world
        corpse.remove();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Tameable pet) { // Damagee is a pet
            // Get owner UUID
            UUID ownerUUID = pet.getOwnerUniqueId();

            if (ownerUUID == null) { // Pet has no owner
                return;
            }

            // Get owner and damager
            Player owner = Bukkit.getPlayer(ownerUUID);
            Entity damager = event.getDamager();

            if (owner == null || ownerUUID.equals(damager.getUniqueId()) || (damager instanceof Player player && PLUGIN.areFriends(player, owner))) { // Owner is offline OR damager is pet owner OR damager is friend of pet owner
                // Cancel damage
                event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof Player damagee && event.getDamager() instanceof Player damager && PLUGIN.areFriends(damagee, damager)) { // Entities are friends
            // Cancel damage
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Tameable entity && entity.isTamed()) { // Entity is a pet
            // Cancel death
            event.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(PLUGIN, () -> { // Delay code by 1 tick
                // Sit pet if possible
                if (entity instanceof Sittable sittable) {
                    sittable.setSitting(true);
                }

                // Teleport pet to spawn
                entity.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }, 1L);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        String playerUUID = event.getPlayer().getUniqueId().toString(); // Get player UUID

        PersistentDataContainer chunkData = event.getBlock().getChunk().getPersistentDataContainer(); // Get chunk data

        NamespacedKey ownerKey = new NamespacedKey(PLUGIN, "owner"); // Make owner key

        String ownerUUID = chunkData.get(ownerKey, PersistentDataType.STRING); // Get owner UUID

        if (ownerUUID == null || ownerUUID.equals(playerUUID)) {
            // The chunk is unclaimed or the player owns the chunk
            return;
        }

        NamespacedKey trustedKey = new NamespacedKey(PLUGIN, "trusted"); // Make trusted key

        String trustedUUIDs = chunkData.get(trustedKey, PersistentDataType.STRING); // Get trusted UUIDs

        if (trustedUUIDs != null && trustedUUIDs.contains(playerUUID)) {
            // The player is trusted with this chunk
            return;
        }

        event.setCancelled(true); // Cancel the event
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        String playerUUID = event.getPlayer().getUniqueId().toString(); // Get player UUID

        PersistentDataContainer chunkData = event.getBlock().getChunk().getPersistentDataContainer(); // Get chunk data

        NamespacedKey ownerKey = new NamespacedKey(PLUGIN, "owner"); // Make owner key

        String ownerUUID = chunkData.get(ownerKey, PersistentDataType.STRING); // Get owner UUID

        if (ownerUUID == null || ownerUUID.equals(playerUUID)) {
            // The chunk is unclaimed or the player owns the chunk
            return;
        }

        NamespacedKey trustedKey = new NamespacedKey(PLUGIN, "trusted"); // Make trusted key

        String trustedUUIDs = chunkData.get(trustedKey, PersistentDataType.STRING); // Get trusted UUIDs

        if (trustedUUIDs != null && trustedUUIDs.contains(playerUUID)) {
            // The player is trusted with this chunk
            return;
        }

        event.setCancelled(true); // Cancel the event
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) { // For each block being removed
            if (block.getChunk().getPersistentDataContainer().has(new NamespacedKey(PLUGIN, "owner"))) { // This block's chunk has an owner
                // Cancel the event
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) { // For each block being removed
            if (block.getChunk().getPersistentDataContainer().has(new NamespacedKey(PLUGIN, "owner"))) { // This block's chunk has an owner
                // Cancel the event
                event.setCancelled(true);
                return;
            }
        }
    }

}
