package ua.lokha.blockphysicslimiter;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ChunkUpdateData {
    private String worldName;
    private int updatesCount;
}
