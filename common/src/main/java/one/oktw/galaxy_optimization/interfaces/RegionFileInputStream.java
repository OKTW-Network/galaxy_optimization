package one.oktw.galaxy_optimization.interfaces;

import net.minecraft.world.level.ChunkPos;

import java.io.DataInputStream;
import java.io.IOException;

public interface RegionFileInputStream {
    DataInputStream getChunkInputStreamNoSync(ChunkPos pos) throws IOException;
}
