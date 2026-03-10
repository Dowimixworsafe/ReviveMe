package pl.Dowimixworsafe.reviveRitual.managers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TombstoneManager {

    private final ReviveRitual plugin;
    private final DataManager dataManager;

    public TombstoneManager(ReviveRitual plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public Location createGrave(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        loc = findFreeSpot(loc);
        String graveId = UUID.randomUUID().toString().substring(0, 8);

        String basePath = "grave." + uuid + "." + graveId;
        dataManager.getData().set(basePath + ".items", null);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                dataManager.getData().set(basePath + ".items." + i, contents[i]);
            }
        }
        dataManager.getData().set(basePath + ".exp", player.getTotalExperience());
        dataManager.getData().set(basePath + ".level", player.getLevel());
        dataManager.getData().set(basePath + ".expFloat", player.getExp());

        dataManager.getData().set(basePath + ".location", loc);
        dataManager.saveData();

        spawnGraveVisuals(player, loc, graveId);
        return loc;
    }

    private Location findFreeSpot(Location origin) {
        Location center = origin.getBlock().getLocation().add(0.5, 0, 0.5);
        if (!hasGraveAt(center))
            return center;

        for (int radius = 1; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius)
                        continue;
                    Location candidate = center.clone().add(x, 0, z);
                    if (!hasGraveAt(candidate))
                        return candidate;
                }
            }
        }
        return center;
    }

    private boolean hasGraveAt(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.5, 1.0, 0.5)) {
            for (String tag : e.getScoreboardTags()) {
                if (tag.startsWith("grave_"))
                    return true;
            }
        }
        return false;
    }

    public void spawnGraveVisuals(Player player, Location loc, String graveId) {

        loc.setPitch(0);
        loc = loc.getBlock().getLocation().add(0.5, 0, 0.5);

        UUID uuid = player.getUniqueId();
        String tag = "grave_" + uuid.toString() + "_" + graveId;

        BlockDisplay basePlatform = (BlockDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.BLOCK_DISPLAY);
        basePlatform.setBlock(Bukkit.createBlockData(Material.SMOOTH_STONE));
        basePlatform.setTransformation(new Transformation(
                new Vector3f(-0.45f, 0f, -0.15f),
                new AxisAngle4f(),
                new Vector3f(0.9f, 0.08f, 0.3f),
                new AxisAngle4f()));
        basePlatform.addScoreboardTag(tag);

        BlockDisplay mainBody = (BlockDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.BLOCK_DISPLAY);
        mainBody.setBlock(Bukkit.createBlockData(Material.STONE));
        mainBody.setTransformation(new Transformation(
                new Vector3f(-0.35f, 0.08f, -0.08f),
                new AxisAngle4f(),
                new Vector3f(0.7f, 0.85f, 0.16f),
                new AxisAngle4f()));
        mainBody.addScoreboardTag(tag);

        float archBaseY = 0.85f;
        float[][] archLayers = {
                { -0.40f, 0.80f, 0.08f },
                { -0.36f, 0.72f, 0.07f },
                { -0.30f, 0.60f, 0.06f },
                { -0.22f, 0.44f, 0.06f },
        };
        float currentY = archBaseY;
        for (float[] layer : archLayers) {
            BlockDisplay archPart = (BlockDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.BLOCK_DISPLAY);
            archPart.setBlock(Bukkit.createBlockData(Material.STONE));
            archPart.setTransformation(new Transformation(
                    new Vector3f(layer[0], currentY, -0.12f),
                    new AxisAngle4f(),
                    new Vector3f(layer[1], layer[2], 0.24f),
                    new AxisAngle4f()));
            archPart.addScoreboardTag(tag);
            currentY += layer[2];
        }

        BlockDisplay mossLeft = (BlockDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.BLOCK_DISPLAY);
        mossLeft.setBlock(Bukkit.createBlockData(Material.MOSSY_COBBLESTONE));
        mossLeft.setTransformation(new Transformation(
                new Vector3f(-0.38f, 0.08f, -0.09f),
                new AxisAngle4f(),
                new Vector3f(0.08f, 0.5f, 0.18f),
                new AxisAngle4f()));
        mossLeft.addScoreboardTag(tag);

        BlockDisplay mossRight = (BlockDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.BLOCK_DISPLAY);
        mossRight.setBlock(Bukkit.createBlockData(Material.MOSSY_COBBLESTONE));
        mossRight.setTransformation(new Transformation(
                new Vector3f(0.3f, 0.08f, -0.09f),
                new AxisAngle4f(),
                new Vector3f(0.08f, 0.5f, 0.18f),
                new AxisAngle4f()));
        mossRight.addScoreboardTag(tag);

        ItemDisplay head = (ItemDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.ITEM_DISPLAY);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
        }
        head.setItemStack(skull);
        head.setTransformation(new Transformation(
                new Vector3f(0f, 1.25f, 0f),
                new AxisAngle4f((float) Math.toRadians(180), 0f, 1f, 0f),
                new Vector3f(0.5f, 0.5f, 0.5f),
                new AxisAngle4f()));
        head.addScoreboardTag(tag);

        TextDisplay ripText = (TextDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.TEXT_DISPLAY);
        ripText.setText("§4§lR.I.P");
        ripText.setLineWidth(200);
        ripText.setAlignment(TextDisplay.TextAlignment.CENTER);
        ripText.setBillboard(Display.Billboard.FIXED);
        ripText.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        ripText.setShadowed(true);
        ripText.setTransformation(new Transformation(
                new Vector3f(0f, 0.6f, 0.09f),
                new AxisAngle4f(),
                new Vector3f(0.3f, 0.3f, 0.3f),
                new AxisAngle4f()));
        ripText.addScoreboardTag(tag);

        TextDisplay text = (TextDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.TEXT_DISPLAY);
        String dateStr = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        text.setText(player.getName() + "\n" + dateStr);
        text.setLineWidth(200);
        text.setAlignment(TextDisplay.TextAlignment.CENTER);
        text.setBillboard(Display.Billboard.FIXED);
        text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        text.setShadowed(true);
        text.setTransformation(new Transformation(
                new Vector3f(0f, 0.3f, 0.09f),
                new AxisAngle4f(),
                new Vector3f(0.25f, 0.25f, 0.25f),
                new AxisAngle4f()));
        text.addScoreboardTag(tag);

        Interaction interaction = (Interaction) loc.getWorld().spawnEntity(loc, EntityType.INTERACTION);
        interaction.setInteractionWidth(0.9f);
        interaction.setInteractionHeight(1.5f);
        interaction.addScoreboardTag(tag);
    }

    public void tryLootGrave(Player player, Interaction interaction) {
        if (plugin.getDataManager().isDead(player)) {
            player.sendMessage(plugin.getConfigManager().getMsg("grave-cannot-loot-dead"));
            return;
        }

        UUID uuid = player.getUniqueId();
        String myPrefix = "grave_" + uuid.toString();
        String foundGraveId = null;
        String otherGraveTag = null;

        for (String tag : interaction.getScoreboardTags()) {
            if (tag.startsWith("grave_")) {
                if (tag.equals(myPrefix)) {
                    foundGraveId = "legacy";
                } else if (tag.startsWith(myPrefix + "_")) {
                    foundGraveId = tag.substring((myPrefix + "_").length());
                } else {
                    otherGraveTag = tag;
                }
            }
        }

        if (foundGraveId != null) {
            restoreItems(player, foundGraveId);
            removeGrave(uuid, foundGraveId);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            player.sendMessage(plugin.getConfigManager().getMsg("grave-looted"));
        } else if (otherGraveTag != null) {
            if (plugin.getConfig().getBoolean("grave-cross-loot", false)) {
                String withoutPrefix = otherGraveTag.substring("grave_".length());
                int lastUnderscore = withoutPrefix.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String ownerUuidStr = withoutPrefix.substring(0, lastUnderscore);
                    String graveId = withoutPrefix.substring(lastUnderscore + 1);
                    try {
                        UUID ownerUuid = UUID.fromString(ownerUuidStr);
                        restoreItemsFrom(player, ownerUuid, graveId);
                        removeGrave(ownerUuid, graveId);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                        player.sendMessage(plugin.getConfigManager().getMsg("grave-looted"));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(plugin.getConfigManager().getMsg("grave-not-yours"));
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMsg("grave-not-yours"));
                }
            } else {
                player.sendMessage(plugin.getConfigManager().getMsg("grave-not-yours"));
            }
        }
    }

    private void restoreItemsFrom(Player player, UUID ownerUuid, String graveId) {
        String basePath = "grave." + ownerUuid + "." + graveId;
        String itemsPath = basePath + ".items";

        if (dataManager.getData().contains(itemsPath)) {
            org.bukkit.configuration.ConfigurationSection itemsSection = dataManager.getData()
                    .getConfigurationSection(itemsPath);
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

        String expPath = basePath + ".exp";
        String levelPath = basePath + ".level";
        String expFloatPath = basePath + ".expFloat";

        player.setTotalExperience(dataManager.getData().getInt(expPath, 0));
        player.setLevel(dataManager.getData().getInt(levelPath, 0));
        player.setExp((float) dataManager.getData().getDouble(expFloatPath, 0));
    }

    private void restoreItems(Player player, String graveId) {
        UUID uuid = player.getUniqueId();
        String basePath = graveId.equals("legacy") ? "grave" : ("grave." + uuid + "." + graveId);
        String itemsPath = basePath + ".items" + (graveId.equals("legacy") ? "." + uuid : "");

        if (dataManager.getData().contains(itemsPath)) {
            org.bukkit.configuration.ConfigurationSection itemsSection = dataManager.getData()
                    .getConfigurationSection(itemsPath);
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

        String expPath = basePath + ".exp" + (graveId.equals("legacy") ? "." + uuid : "");
        String levelPath = basePath + ".level" + (graveId.equals("legacy") ? "." + uuid : "");
        String expFloatPath = basePath + ".expFloat" + (graveId.equals("legacy") ? "." + uuid : "");

        player.setTotalExperience(dataManager.getData().getInt(expPath, 0));
        player.setLevel(dataManager.getData().getInt(levelPath, 0));
        player.setExp((float) dataManager.getData().getDouble(expFloatPath, 0.0));
    }

    public void removeGrave(UUID uuid, String graveId) {
        removeGraveVisuals(uuid, graveId);

        if (graveId.equals("legacy")) {
            dataManager.getData().set("grave.items." + uuid, null);
            dataManager.getData().set("grave.exp." + uuid, null);
            dataManager.getData().set("grave.level." + uuid, null);
            dataManager.getData().set("grave.expFloat." + uuid, null);
            dataManager.getData().set("grave.location." + uuid, null);
        } else {
            dataManager.getData().set("grave." + uuid + "." + graveId, null);
        }
        dataManager.saveData();
    }

    public void removeGraveVisuals(UUID uuid, String graveId) {
        String locPath = graveId.equals("legacy") ? "grave.location." + uuid
                : "grave." + uuid + "." + graveId + ".location";
        Location loc = dataManager.getData().getLocation(locPath);
        if (loc != null && loc.getWorld() != null) {
            String targetTag = graveId.equals("legacy") ? "grave_" + uuid.toString()
                    : "grave_" + uuid.toString() + "_" + graveId;
            for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                if (e.getScoreboardTags().contains(targetTag)) {
                    if (e instanceof Interaction) {
                        e.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, e.getLocation(), 20, 0.5, 0.5, 0.5,
                                0.01);
                    }
                    e.remove();
                }
            }
        }
    }
}
