package pl.Dowimixworsafe.reviveMe.managers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.Dowimixworsafe.reviveMe.ReviveMe;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TombstoneManager {

    private final ReviveMe plugin;
    private final DataManager dataManager;
    private final Map<UUID, UUID[]> graveEntities = new HashMap<>();

    public TombstoneManager(ReviveMe plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void createGrave(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();

        dataManager.getData().set("grave.items." + uuid, null);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                dataManager.getData().set("grave.items." + uuid + "." + i, contents[i]);
            }
        }
        dataManager.getData().set("grave.exp." + uuid, player.getTotalExperience());
        dataManager.getData().set("grave.level." + uuid, player.getLevel());
        dataManager.getData().set("grave.expFloat." + uuid, player.getExp());

        dataManager.getData().set("grave.location." + uuid, loc);
        dataManager.saveData();

        spawnGraveVisuals(player, loc);
    }

    public void spawnGraveVisuals(Player player, Location loc) {

        loc.setPitch(0);
        loc = loc.getBlock().getLocation().add(0.5, 0, 0.5);

        UUID uuid = player.getUniqueId();
        removeGraveVisuals(uuid);

        BlockDisplay base = (BlockDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.BLOCK_DISPLAY);
        base.setBlock(Bukkit.createBlockData(Material.STONE));
        base.setTransformation(new Transformation(
                new Vector3f(-0.4f, 0f, -0.1f),
                new AxisAngle4f(),
                new Vector3f(0.8f, 1.2f, 0.2f),
                new AxisAngle4f()));

        ItemDisplay head = (ItemDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.ITEM_DISPLAY);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
        }
        head.setItemStack(skull);

        head.setTransformation(new Transformation(
                new Vector3f(0f, 0.8f, 0.105f),
                new AxisAngle4f((float) Math.toRadians(180), 0f, 1f, 0f),
                new Vector3f(0.5f, 0.5f, 0.05f),
                new AxisAngle4f()));

        TextDisplay text = (TextDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.TEXT_DISPLAY);
        String dateStr = new SimpleDateFormat("dd.MM.yyyy").format(new Date());

        text.setText(player.getName() + "\n" + dateStr);
        text.setLineWidth(200);
        text.setAlignment(TextDisplay.TextAlignment.CENTER);

        text.setBillboard(Display.Billboard.FIXED);
        text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        text.setShadowed(false);

        text.setTransformation(new Transformation(
                new Vector3f(0f, 0.35f, 0.11f),
                new AxisAngle4f(),
                new Vector3f(0.25f, 0.25f, 0.25f),
                new AxisAngle4f()));

        Interaction interaction = (Interaction) loc.getWorld().spawnEntity(loc, EntityType.INTERACTION);
        interaction.setInteractionWidth(0.8f);
        interaction.setInteractionHeight(1.2f);

        graveEntities.put(uuid,
                new UUID[] { base.getUniqueId(), head.getUniqueId(), text.getUniqueId(), interaction.getUniqueId() });

        interaction.addScoreboardTag("grave_" + uuid.toString());
    }

    public void tryLootGrave(Player player, Interaction interaction) {
        if (plugin.getDataManager().isDead(player)) {
            player.sendMessage(plugin.getConfigManager().getMsg("grave-cannot-loot-dead"));
            return;
        }

        UUID uuid = player.getUniqueId();
        if (interaction.getScoreboardTags().contains("grave_" + uuid.toString())) {
            restoreItems(player);
            removeGrave(uuid);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            player.sendMessage(plugin.getConfigManager().getMsg("grave-looted"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMsg("grave-not-yours"));
        }
    }

    private void restoreItems(Player player) {
        UUID uuid = player.getUniqueId();

        if (dataManager.getData().contains("grave.items." + uuid)) {
            org.bukkit.configuration.ConfigurationSection itemsSection = dataManager.getData()
                    .getConfigurationSection("grave.items." + uuid);
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        ItemStack item = itemsSection.getItemStack(key);
                        if (item != null) {
                            ItemStack existing = player.getInventory().getItem(slot);
                            if (existing == null || existing.getType().isAir()) {
                                player.getInventory().setItem(slot, item);
                            } else {
                                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                                for (ItemStack left : leftover.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                                }
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        player.setTotalExperience(dataManager.getData().getInt("grave.exp." + uuid, 0));
        player.setLevel(dataManager.getData().getInt("grave.level." + uuid, 0));
        player.setExp((float) dataManager.getData().getDouble("grave.expFloat." + uuid, 0.0));
    }

    public void removeGrave(UUID uuid) {
        removeGraveVisuals(uuid);

        dataManager.getData().set("grave.items." + uuid, null);
        dataManager.getData().set("grave.exp." + uuid, null);
        dataManager.getData().set("grave.level." + uuid, null);
        dataManager.getData().set("grave.expFloat." + uuid, null);
        dataManager.getData().set("grave.location." + uuid, null);
        dataManager.saveData();
    }

    public void removeGraveVisuals(UUID uuid) {
        if (graveEntities.containsKey(uuid)) {
            for (UUID entityId : graveEntities.get(uuid)) {
                org.bukkit.entity.Entity e = Bukkit.getEntity(entityId);
                if (e != null) {
                    if (e instanceof Interaction) {
                        e.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, e.getLocation(), 20, 0.5, 0.5, 0.5,
                                0.01);
                    }
                    e.remove();
                }
            }
            graveEntities.remove(uuid);
        }
    }
}
