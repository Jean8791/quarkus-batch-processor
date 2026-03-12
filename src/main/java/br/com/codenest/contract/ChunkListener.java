package br.com.codenest.contract;

@FunctionalInterface
public interface ChunkListener {
    void onChunkComplete(long processedCount);
}