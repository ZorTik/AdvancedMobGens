package me.zort.gencore.data.accessor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import me.zort.gencore.GenCore;
import me.zort.gencore.data.Accessor;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.companion.Generator;
import me.zort.gencore.data.entity.ChunkGeneratorReference;
import me.zort.gencore.data.entity.PlayerData;
import me.zort.gencore.object.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ChunkGeneratorReferenceAccessor implements Accessor<PlayerData> {

    private final JdbcConnector<? extends ConnectionSource, ?> connector;
    private final Map<Chunk, List<ChunkGeneratorReference>> refCache = Maps.newConcurrentMap();
    private final Map<String, Optional<PlayerData>> playerCache = Maps.newConcurrentMap();
    private final Logger logger;

    public ChunkGeneratorReferenceAccessor(JdbcConnector<? extends ConnectionSource, ?> connector, Logger logger) {
        this.connector = connector;
        this.logger = logger;
    }

    public CompletableFuture<Void> registerAsync(Chunk c) {
        return CompletableFuture.runAsync(() -> register(c));
    }

    public void register(Chunk c) {
        register(c, false);
    }

    private void register(Chunk c, boolean internal) {
        unregister(c, !internal);
        Dao<ChunkGeneratorReference, Integer> dao = connector.getOrCreateDao(ChunkGeneratorReference.class);
        if(dao == null) {
            String message = "Dao for generator reference cannot be created!";
            logger.info(message);
            throw new RuntimeException(message);
        }
        List<ChunkGeneratorReference> refs;
        try {
            refs = dao.query(
                    dao.queryBuilder().where()
                            .eq("worldName", c.getWorld().getName()).and()
                            .eq("xChunkIndex", c.getX()).and()
                            .eq("zChunkIndex", c.getZ())
                            .prepare()
            );
            refCache.put(c, Collections.synchronizedList(refs));
            Set<String> nicks = refs.stream()
                    .map(ChunkGeneratorReference::getNickname)
                    .collect(Collectors.toSet());
            Set<String> nicksToAdd = nicks.stream()
                    .filter(nick -> !playerCache.containsKey(nick))
                    .collect(Collectors.toSet());
            Set<Pair<String, Optional<PlayerData>>> nickPairs = nicksToAdd.stream()
                    .map(nick -> new Pair<>(nick, Optional.ofNullable(GenCore.getSingleton().getPlayerCache().getPlayerData(nick))))
                    .collect(Collectors.toSet());
            nickPairs.forEach(pair -> {
                playerCache.put(pair.getFirst(), pair.getSecond());
            });
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
    }

    public boolean unregister(Chunk c) {
        return unregister(c, true);
    }

    private boolean unregister(Chunk c, boolean removePlayers) {
        List<ChunkGeneratorReference> list = refCache.remove(c);
        if(list != null && removePlayers) {
            playerCache.entrySet().removeIf(e -> list.stream().anyMatch(ref -> ref.getNickname().equals(e.getKey())));
        }
        return list != null;
    }

    public boolean isGenBlock(Location loc) {
        return getGenBy(loc) != null;
    }

    @Nullable
    public Pair<String, Generator> getGenBy(Location loc) {
        if(loc == null) return null;
        Chunk chunk = loc.getChunk();
        Map<String, List<Generator>> gensMap = getGensBy(chunk);
        for(String nick : gensMap.keySet()) {
            List<Generator> gens = gensMap.get(nick);
            for(Generator gen : gens) {
                if(gen.isGenBlock(loc)) {
                    return new Pair<>(nick, gen);
                }
            }
        }
        return null;
    }

    public Map<String, List<Generator>> getGensBy(Chunk c) {
        List<ChunkGeneratorReference> refsBy = getRefsBy(c);
        Map<String, List<Generator>> genMap = Maps.newHashMap();
        refsBy.forEach(ref -> {
            String nick = ref.getNickname();
            PlayerData playerData = playerCache.getOrDefault(nick, Optional.empty())
                    .orElse(null);
            if(playerData != null && !genMap.containsKey(nick)) {
                genMap.put(nick, playerData.getGenerators().stream()
                        .filter(gen -> {
                            Location loc = gen.getBukkitLocation();
                            if(loc != null) {
                                Chunk chunk = loc.getChunk();
                                return chunk.getX() == c.getX()
                                        && chunk.getZ() == c.getZ();
                            }
                            return false;
                        })
                        .collect(Collectors.toList()));
            }
        });
        return genMap;
    }

    public List<ChunkGeneratorReference> getRefsBy(Chunk c) {
        return refCache.getOrDefault(c, Lists.newArrayList());
    }

    @Override
    public void access(PlayerData pd) {
        Dao<ChunkGeneratorReference, Integer> dao = connector.getOrCreateDao(ChunkGeneratorReference.class);
        if(dao == null) {
            String message = "Dao for generator reference cannot be created!";
            logger.info(message);
            throw new RuntimeException(message);
        }
        try {
            List<ChunkGeneratorReference> refList = dao.query(dao.queryBuilder()
                    .where()
                    .eq("nickname", pd.getNickname())
                    .prepare());
            if(refList.size() != pd.getGenerators().size()) {
                dao.delete(refList.stream()
                        .filter(ref -> Bukkit.getWorld(ref.getWorldName()) != null)
                        .collect(Collectors.toList()));
                Map<ChunkGeneratorReference, Long> changeMap = Maps.newConcurrentMap();
                List<ChunkGeneratorReference> newRefList = pd.getGenerators().stream()
                        .map(gen -> {
                            String world = gen.getLocation().getWorld();
                            if(Bukkit.getWorld(world) == null) {
                                return null;
                            }
                            Location loc = gen.getBukkitLocation();
                            Chunk chunk = loc.getChunk();
                            ChunkGeneratorReference ref = new ChunkGeneratorReference();
                            ref.setNickname(pd.getNickname());
                            ref.setWorldName(world);
                            ref.setXChunkIndex(chunk.getX());
                            ref.setZChunkIndex(chunk.getZ());
                            return ref;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                Set<ChunkGeneratorReference> temp = new HashSet<>();
                List<ChunkGeneratorReference> flList = new ArrayList<>(refList);
                flList.addAll(newRefList);
                for(ChunkGeneratorReference cgr : flList) {
                    if(temp.stream().noneMatch(cgr1 -> cgr1.equals(cgr))) {
                        temp.add(cgr);
                    }
                }
                for(ChunkGeneratorReference cgr : temp) {
                    long nlc = newRefList.stream()
                            .filter(cgr1 -> cgr1.equals(cgr))
                            .count();
                    long olc = refList.stream()
                            .filter(cgr1 -> cgr1.equals(cgr))
                            .count();
                    if(nlc != olc) {
                        long change = nlc - olc;
                        changeMap.put(cgr, change);
                    }
                }
                List<ChunkGeneratorReference> remList = changeMap.entrySet().stream()
                        .filter(e -> e.getValue() < 0)
                        .flatMap(cgre -> {
                            ChunkGeneratorReference cgr = cgre.getKey();
                            return refList.stream()
                                    .filter(cgr1 -> cgr1.equals(cgr))
                                    .limit(Math.abs(cgre.getValue()));
                        })
                        .collect(Collectors.toList());
                List<ChunkGeneratorReference> addList = changeMap.entrySet().stream()
                        .filter(e -> e.getValue() > 0)
                        .flatMap(cgre -> {
                            ChunkGeneratorReference cgr = cgre.getKey();
                            return newRefList.stream()
                                    .filter(cgr1 -> cgr1.equals(cgr))
                                    .limit(Math.abs(cgre.getValue()));
                        })
                        .collect(Collectors.toList());
                if(!remList.isEmpty()) {
                    dao.delete(remList);
                }
                if(!addList.isEmpty()) {
                    dao.create(addList);
                }
                List<Chunk> changedChunksList = refCache.keySet().stream()
                        .filter(chunk -> {
                            Predicate<ChunkGeneratorReference> pred = cgr -> cgr.getXChunkIndex() == chunk.getX() && cgr.getZChunkIndex() == chunk.getZ() && cgr.getWorldName().equalsIgnoreCase(chunk.getWorld().getName());
                            return remList.stream().anyMatch(pred) || addList.stream().anyMatch(pred);
                        })
                        .collect(Collectors.toList());
                changedChunksList.forEach(c -> {
                    if(refCache.containsKey(c)) {
                        playerCache.put(pd.getNickname(), Optional.ofNullable(pd));
                    }
                    register(c, true);
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
