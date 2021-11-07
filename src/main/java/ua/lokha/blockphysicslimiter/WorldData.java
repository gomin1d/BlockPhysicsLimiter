package ua.lokha.blockphysicslimiter;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import lombok.Data;

@Data
public class WorldData {
    private String worldName;
    public Long2IntOpenHashMap counter = new Long2IntOpenHashMap();
    public Long2IntOpenHashMap toLog = new Long2IntOpenHashMap();

    public WorldData(String worldName) {
        this.worldName = worldName;

        this.counter.defaultReturnValue(0);
        this.toLog.defaultReturnValue(0);
    }
}
