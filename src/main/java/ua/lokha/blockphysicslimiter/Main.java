//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ua.lokha.blockphysicslimiter;

import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.ChunkSection;
import net.minecraft.server.v1_12_R1.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {
    private Map<String, WorldData> worldDataMap = new HashMap<>();
    private int startTick;
    private int radiusChunks;
    private int limit;
    private int limitBreakBlock;
    private int ticks;
    private IBlockData air;

    public Main() {
        this.startTick = this.getTick();
        this.air = Block.getById(0).fromLegacyData(0);
    }

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        this.loadConfigParams();
        this.getCommand("blockphysicslimiter").setExecutor(this);
    }

    public void loadConfigParams() {
        this.limit = Math.max(0, this.getConfig().getInt("limit"));
        this.limitBreakBlock = Math.max(0, this.getConfig().getInt("limit-break-block"));
        this.radiusChunks = Math.max(0, this.getConfig().getInt("radius-chunks"));
        this.ticks = Math.max(1, this.getConfig().getInt("ticks"));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.reloadConfig();
        this.loadConfigParams();
        sender.sendMessage("§aКонфиг перезагружен.");
        return true;
    }

    @EventHandler(
            ignoreCancelled = true,
            priority = EventPriority.LOW
    )
    public void on(BlockPhysicsEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        WorldData worldData = worldDataMap.computeIfAbsent(worldName, WorldData::new);
        int tick = this.getTick();
        if (tick - this.startTick >= this.ticks) {
            this.startTick = tick;

            try {
                this.printTopChunks();
            } catch (Exception var13) {
                this.getLogger().info("Ошибка при выводе списка загруженных чанков.");
                var13.printStackTrace();
            }

            for (WorldData it : worldDataMap.values()) {
                it.counter.clear();
            }
        }

        int chunkX = event.getBlock().getX() >> 4;
        int chunkZ = event.getBlock().getZ() >> 4;
        long key = key(chunkX, chunkZ);
        int count = worldData.counter.get(key);
        worldData.counter.put(key, count + 1);
        int toX = chunkX + this.radiusChunks;
        int toZ = chunkZ + this.radiusChunks;
        int sumCount = count;

        for(int x = chunkX - this.radiusChunks; x <= toX; ++x) {
            for(int z = chunkZ - this.radiusChunks; z <= toZ; ++z) {
                if (x != chunkX || z != chunkZ) {
                    sumCount += worldData.counter.get(key(x, z));
                }
            }
        }

        if (sumCount >= this.limit) {
            event.setCancelled(true);
            worldData.toLog.put(key, sumCount);
            if (sumCount >= this.limitBreakBlock) {
                this.removeBlock(event.getBlock(), sumCount, this.limit);
            }
        }

    }

    @EventHandler
    public void on(WorldUnloadEvent event){
        worldDataMap.remove(event.getWorld().getName());
    }

    private void printTopChunks() {
        for (WorldData worldData : worldDataMap.values()) {
            try {
                if (!worldData.toLog.isEmpty()) {
                    StringBuilder builder = new StringBuilder("[Мир " + worldData.getWorldName() + "] Список чанков, которые превысили лимит обновлений блоков за последние " + this.ticks + " тиков:");
                    ObjectIterator iterator = worldData.toLog.long2IntEntrySet().fastIterator();

                    while(iterator.hasNext()) {
                        Entry next = (Entry)iterator.next();
                        long key = next.getLongKey();
                        long x = getX(key);
                        long z = getZ(key);
                        builder.append("\nChunk X: ").append(x).append(", Chunk Z: ").append(z).append(", Count: ").append(next.getIntValue());
                    }

                    this.getLogger().info(builder.toString());
                }
            } catch (Exception e) {
                this.getLogger().severe("[printTopChunks] Ошибка при обработке мира " + worldData.getWorldName());
                e.printStackTrace();
            } finally {
                worldData.toLog.clear();
            }
        }
    }

    private void removeBlock(org.bukkit.block.Block block, int count, int limit) {
        int i = block.getX() & 15;
        int j = block.getZ() & 15;
        int sectionY = block.getY() >> 4;
        if (sectionY < 0 || sectionY >= 16) {
            return; // WorldEdit может вызвать событие для блока, которые выходят за пределы чанка
        }
        ChunkSection chunkSection = ((CraftChunk)block.getChunk()).getHandle().getSections()[sectionY];
        if (chunkSection != null) {
            chunkSection.setType(i, block.getY() & 15, j, this.air);
        }
    }

    public static long key(int x, int z) {
        return (long)x << 32 | (long)z & 4294967295L;
    }

    public static long getX(long key) {
        return (long)((int)(key >> 32));
    }

    public static long getZ(long key) {
        return (long)((int)key);
    }

    public int getTick() {
        return ((CraftServer)Bukkit.getServer()).getServer().aq();
    }
}
