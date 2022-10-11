package me.zort.gencore;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import me.zort.gencore.cache.PlayerDataCache;
import me.zort.gencore.cache.SpawnedEntityCache;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.config.ConfiguredMob;
import me.zort.gencore.configuration.messages.ConfigurationImplMessages;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.accessor.ChunkGeneratorReferenceAccessor;
import me.zort.gencore.data.companion.Generator;
import me.zort.gencore.data.entity.PlayerData;
import me.zort.gencore.data.entity.SpawnedEntity;
import me.zort.gencore.object.Pair;
import me.zort.gencore.object.SerializableLoc;
import me.zort.gencore.util.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GenCoreListener implements Listener {

    private static GenCoreListener SINGLETON = null;
    private static final String GENERATOR_ITEM_KEY = "gencore-generator-item";

    public static GenCoreListener getSingleton() {
        return SINGLETON;
    }

    private final GenCore plugin = GenCore.getSingleton();
    private final ConfigurationImplConfig config = ConfigurationRegistry.getConfiguration(ConfigurationImplConfig.class)
            .get();
    private final PlayerDataCache playerCache = plugin.getPlayerCache();
    private final SpawnedEntityCache entityCache = plugin.getEntityCache();
    private final ConfigurationImplMessages messages = ConfigurationRegistry.getConfiguration(ConfigurationImplMessages.class)
            .get();

    protected GenCoreListener() {
        SINGLETON = this;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        String nick = e.getName();
        PlayerData playerData = playerCache.getPlayerData(nick);
        if(playerData == null) {
            e.setKickMessage("Cannot load mob generators data for your profile!");
            e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String nick = p.getName();
        PlayerData playerData = playerCache.getPlayerData(nick);
        List<Location> locs = playerData.getGenerators().stream()
                .map(Generator::getBukkitLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        ItemStack genItem = generatorItem();
        if(genItem != null) {
            locs.forEach(loc -> {
                Block block = loc.getBlock();
                Material genItemType = genItem.getType();
                if(genItemType.isBlock() && !block.getType().equals(genItemType)) {
                    block.setType(genItemType, false);
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String nick = e.getPlayer().getName();
        playerCache.clean(nick, true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        String nick = p.getName();
        ItemStack item = e.getItemInHand();
        if(isGeneratorItem(item)) {
            Block blockPlaced = e.getBlockPlaced();
            if(!blockPlaced.getType().isSolid()) {
                e.setCancelled(true);
                return;
            }
            PlayerData playerData = playerCache.getPlayerData(p);
            if(playerData == null) {
                e.setCancelled(true);
                return;
            }
            Optional<Generator> genOptional = playerData.getGeneratorBy(blockPlaced.getLocation());
            if(genOptional.isPresent()) {
                e.setCancelled(true);
                return;
            }
            if(playerData.getGenerators().size() >= playerData.getGeneratorsLimit()) {
                e.setCancelled(true);
                messages.send(p, ConfigurationImplMessages.Message.LIMIT_REACHED);
                return;
            }
            playerCache.modifyPlayerDataAsync(nick, pd -> {
                List<Generator> generators = pd.getGenerators();
                Generator gen = new Generator();
                gen.setLocation(SerializableLoc.of(blockPlaced.getLocation()));
                gen.setTierIndex(0);
                generators.add(gen);
            }).whenComplete((data, ex) -> {
                if(ex != null || data == null) {
                    messages.send(p, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                    return;
                }
                messages.send(p, ConfigurationImplMessages.Message.PLACED_GENERATOR);
            });
            Location particleLoc = blockPlaced.getLocation().clone().add(0.5, 1.5, 0.5);
            blockPlaced.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        String nick = p.getName();
        if(e.isCancelled()) return;
        Block block = e.getBlock();
        Location loc = block.getLocation();
        PlayerData playerData = playerCache.getPlayerData(p);
        if(playerData == null) return;
        if(playerData.getGeneratorBy(block.getLocation()).isPresent()) {
            playerCache.modifyPlayerDataAsync(nick, pd -> {
                List<Generator> generators = pd.getGenerators();
                generators.removeIf(gen -> gen.isGenBlock(block));
            }).whenComplete((data, ex) -> {
                if(ex != null || data == null) {
                    messages.send(p, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                    return;
                }
                messages.send(p, ConfigurationImplMessages.Message.BROKEN_GENERATOR);
            });
            Location particleLoc = block.getLocation().clone().add(0.5, 1.5, 0.5);
            block.getWorld().spawnParticle(Particle.BARRIER, particleLoc, 1);
        } else {
            ChunkGeneratorReferenceAccessor refAccessor = plugin.getRefAccessor();
            Pair<String, Generator> gen = refAccessor.getGenBy(loc);
            if(gen != null) {
                if(!gen.getFirst().equals(nick)) {
                    e.setCancelled(true);
                    messages.send(p, ConfigurationImplMessages.Message.CANT_BREAK_OTHERS_GENS);
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if(e.getHand() != EquipmentSlot.HAND) return;
            Player p = e.getPlayer();
            String nick = p.getName();
            Location loc = e.getClickedBlock().getLocation();
            PlayerData playerData = playerCache.getPlayerData(nick);
            Optional<Generator> genOptional = playerData.getGeneratorBy(loc);
            if(genOptional.isPresent()) {
                e.setCancelled(true);
                Generator gen = genOptional.get();
                if(gen.canUpgrade()) {
                    playerCache.modifyPlayerDataAsync(nick, pd -> {
                        Generator generator = pd.getGeneratorBy(loc).get();
                        if(!generator.upgrade()) {
                            throw new RuntimeException("Generator can't be upgraded!");
                        }
                    }).whenComplete((pd, ex) -> {
                        Optional<Generator> gno;
                        if(ex != null || pd == null || !(gno = pd.getGeneratorBy(loc)).isPresent()) {
                            messages.send(p, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                            return;
                        }
                        Generator gn = gno.get();
                        messages.send(p, ConfigurationImplMessages.Message.GENERATOR_LEVEL_UP, new Pair<>("%level%", gn.getTierIndex() + 1));
                    });
                } else {
                    messages.send(p, ConfigurationImplMessages.Message.GENERATOR_MAX_LEVEL);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreKill(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        EntityType entityType = entity.getType();
        if(entity instanceof LivingEntity && ((LivingEntity) entity).getHealth() - e.getFinalDamage() <= 0.0 && config.hasMob(entityType)) {
            if(!remove((LivingEntity) entity)) {
                ((LivingEntity) entity).setHealth(((LivingEntity) entity).getMaxHealth());
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    SpawnedEntity se = entityCache.query(entity.getUniqueId().toString(), null);
                    if(se != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            String name = messages.getSingle(ConfigurationImplMessages.Message.ENTITY_STACK_NAME, new Pair<>("%amount%", se.getAmount()), new Pair<>("%name%", entityType.getName()));
                            entity.setCustomName(name);
                        });
                    }
                });
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk c = e.getChunk();
        ChunkGeneratorReferenceAccessor refAccessor = plugin.getRefAccessor();
        refAccessor.registerAsync(c);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk c = e.getChunk();
        ChunkGeneratorReferenceAccessor refAccessor = plugin.getRefAccessor();
        refAccessor.unregister(c);
    }

    private boolean remove(LivingEntity entity) {
        EntityType entityType = entity.getType();
        String entityId = entity.getUniqueId().toString();
        JdbcConnector<? extends ConnectionSource, ?> connector = GenCore.getSingleton().getConnector();
        SpawnedEntity spawnedEntity = entityCache.getEntity(entityId, null);
        Consumer<SpawnedEntity> seCons = se -> {
            ConfiguredMob mob = config.getMobBy(entityType).get();
            ConfigurationSection mobSection = mob.getSection();
            int amount = mobSection.getInt("loot-amount", 1);
            List<ConfiguredMob.LootItem> items = mob.randomLoot(amount);
            playerCache.modifyPlayerDataAsync(se.getOwner(), playerData -> {
                playerData.getVirtualStorage().addAll(
                        items.stream()
                                .map(ConfiguredMob.LootItem::getName)
                                .collect(Collectors.toList())
                );
            });
        };
        if(spawnedEntity != null) {
            if(spawnedEntity.getAmount() <= 1) {
                //seCons.accept(spawnedEntity);
                return true;
            } else {
                entityCache.modifyEntityAsync(spawnedEntity.getId(), spawnedEntity.getOwner(), se -> {
                    se.setAmount(se.getAmount() - 1);
                    //seCons.accept(se);
                });
                return false;
            }
        } else {
            /* 
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Dao<SpawnedEntity, String> dao = connector.getOrCreateDao(SpawnedEntity.class);
                if(dao == null) {
                    plugin.getLogger().info("Cannot create dao for entities!");
                    return;
                }
                SpawnedEntity se;
                try {
                    se = dao.queryForId(entityId);
                    if(se != null && se.getAmount() <= 1) {
                        seCons.accept(se);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }); 
            */
            return true;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobKill(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        EntityType entityType = entity.getType();
        if(config.hasMob(entityType)) {
            ConfiguredMob mob = config.getMobBy(entityType).get();
            ConfigurationSection mobSection = mob.getSection();
            int amount = mobSection.getInt("loot-amount", 1);
            List<ConfiguredMob.LootItem> items = mob.randomLoot(amount);
            List<ItemStack> drops = e.getDrops();
            drops.clear();
            drops.addAll(items.stream().map(ConfiguredMob.LootItem::toItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
    }

    @Nullable
    public ItemStack generatorItem() {
        Optional<@NotNull ConfigurationSection> sectionOptional = config.getConfigurationSection("spawn-item");
        if(!sectionOptional.isPresent()) {
            return null;
        }
        ConfigurationSection section = sectionOptional.get();
        ItemStack item = ItemUtil.fromConfigurationSection(section);
        if(item == null) {
            return null;
        }
        return ItemUtil.editNBT(item, nbtItem -> nbtItem.setBoolean(GENERATOR_ITEM_KEY, true));
    }

    public boolean isGeneratorItem(ItemStack item) {
        if(item == null) return false;
        return ItemUtil.getFromNBT(item, nbtItem -> nbtItem.hasKey(GENERATOR_ITEM_KEY) && nbtItem.getBoolean(GENERATOR_ITEM_KEY));
    }

}
