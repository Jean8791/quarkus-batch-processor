package io.github.jean8791.benchmark;

import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks two heap peaks while a scenario runs:
 * <ul>
 *   <li>{@code retained}: highest heap occupancy right after a GC (what stayed alive);</li>
 *   <li>{@code used}: highest heap in use seen by a 10 ms sampler (includes garbage not yet collected).</li>
 * </ul>
 */
final class HeapMonitor implements AutoCloseable {

    private final AtomicLong peakRetained = new AtomicLong();
    private final AtomicLong peakUsed = new AtomicLong();
    private final Set<String> heapPools = new java.util.HashSet<>();
    private final Thread sampler;
    private volatile boolean running = true;

    HeapMonitor() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                heapPools.add(pool.getName());
            }
        }
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            ((NotificationEmitter) gc).addNotificationListener((notification, handback) -> {
                if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
                    return;
                }
                GarbageCollectionNotificationInfo info =
                        GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
                long after = info.getGcInfo().getMemoryUsageAfterGc().entrySet().stream()
                        .filter(entry -> heapPools.contains(entry.getKey()))
                        .mapToLong(entry -> entry.getValue().getUsed())
                        .sum();
                peakRetained.accumulateAndGet(after, Math::max);
            }, null, null);
        }
        sampler = new Thread(() -> {
            while (running) {
                MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                peakUsed.accumulateAndGet(heap.getUsed(), Math::max);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "heap-sampler");
        sampler.setDaemon(true);
        sampler.start();
    }

    void reset() {
        peakRetained.set(0);
        peakUsed.set(0);
    }

    /** Heap in use right now, in MB. */
    long usedMb() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    long peakRetainedMb() {
        return peakRetained.get() / (1024 * 1024);
    }

    long peakUsedMb() {
        return peakUsed.get() / (1024 * 1024);
    }

    @Override
    public void close() {
        running = false;
    }
}
