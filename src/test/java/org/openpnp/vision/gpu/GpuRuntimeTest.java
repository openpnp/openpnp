package org.openpnp.vision.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.IntBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openpnp.vision.gpu.GpuPipeline.Binding;

public class GpuRuntimeTest {
    private static final int COUNT = 1000;

    @BeforeAll
    public static void setup() {
        Assumptions.assumeTrue(GpuRuntime.isAvailable(), "no Vulkan GPU");
    }

    private static GpuPipeline selftest(int scale) {
        return new GpuPipeline("selftest", new int[] { scale }, Binding.Uniform, Binding.Storage, Binding.Storage);
    }

    private static GpuBuffer params(int add) {
        GpuBuffer params = new GpuBuffer(16, true);
        params.map().putInt(0, COUNT).putInt(4, add);
        return params;
    }

    private static void fill(GpuBuffer buffer) {
        IntBuffer values = buffer.map().asIntBuffer();
        for (int i = 0; i < COUNT; i++) {
            values.put(i, i);
        }
    }

    @Test
    public void chainedDispatchesSeeEachOthersResults() {
        try (GpuPipeline triple = selftest(3); GpuPipeline twice = selftest(2);
                GpuBuffer add1 = params(1); GpuBuffer add5 = params(5);
                GpuBuffer src = new GpuBuffer(COUNT * 4, true);
                GpuBuffer middle = new GpuBuffer(COUNT * 4, false);
                GpuBuffer dst = new GpuBuffer(COUNT * 4, true);
                GpuProgram program = new GpuProgram.Builder()
                        .dispatch(triple, (COUNT + 63) / 64, 1, 1, add1, src, middle)
                        .dispatch(twice, (COUNT + 63) / 64, 1, 1, add5, middle, dst)
                        .build()) {
            fill(src);
            program.run();
            IntBuffer out = dst.map().asIntBuffer();
            for (int i = 0; i < COUNT; i++) {
                assertEquals((i * 3 + 1) * 2 + 5, out.get(i));
            }
        }
    }

    @Test
    public void rerunPicksUpNewParameters() {
        try (GpuPipeline pipeline = selftest(1); GpuBuffer params = params(0);
                GpuBuffer src = new GpuBuffer(COUNT * 4, true); GpuBuffer dst = new GpuBuffer(COUNT * 4, true);
                GpuProgram program = new GpuProgram.Builder()
                        .dispatch(pipeline, (COUNT + 63) / 64, 1, 1, params, src, dst).build()) {
            fill(src);
            for (int add = 0; add < 5; add++) {
                params.map().putInt(4, add);
                program.run();
                assertEquals(COUNT - 1 + add, dst.map().asIntBuffer().get(COUNT - 1));
            }
        }
    }

    @Test
    public void indirectDispatchAndCopy() {
        try (GpuPipeline pipeline = selftest(1); GpuBuffer params = params(10);
                GpuBuffer groups = new GpuBuffer(12, true);
                GpuBuffer src = new GpuBuffer(COUNT * 4, true);
                GpuBuffer mid = new GpuBuffer(COUNT * 4, false);
                GpuBuffer dst = new GpuBuffer(COUNT * 4, true);
                GpuProgram program = new GpuProgram.Builder()
                        .dispatchIndirect(pipeline, groups, 0, params, src, mid)
                        .copy(mid, 0, dst, 0, COUNT * 4)
                        .build()) {
            fill(src);
            IntBuffer dstValues = dst.map().asIntBuffer();
            for (int i = 0; i < COUNT; i++) {
                dstValues.put(i, -1);
            }
            groups.map().putInt(0, 1).putInt(4, 1).putInt(8, 1);
            program.run();
            assertEquals(10, dstValues.get(0));
            assertEquals(73, dstValues.get(63));
            groups.map().putInt(0, (COUNT + 63) / 64);
            program.run();
            assertEquals(COUNT - 1 + 10, dstValues.get(COUNT - 1));
        }
    }

    @Test
    public void programKeepsClosedBuffersAlive() {
        GpuPipeline pipeline = selftest(1);
        GpuBuffer params = params(4);
        GpuBuffer src = new GpuBuffer(COUNT * 4, true);
        fill(src);
        try (GpuBuffer dst = new GpuBuffer(COUNT * 4, true)) {
            GpuProgram program = new GpuProgram.Builder()
                    .dispatch(pipeline, (COUNT + 63) / 64, 1, 1, params, src, dst).build();
            pipeline.close();
            params.close();
            src.close();
            long value = program.submit();
            program.close();
            GpuRuntime.await(value, TimeUnit.SECONDS.toNanos(5));
            assertEquals(COUNT - 1 + 4, dst.map().asIntBuffer().get(COUNT - 1));
            assertTrue(GpuRuntime.completed() >= value);
        }
    }

    @Test
    public void closedObjectsRejectUse() {
        GpuBuffer buffer = new GpuBuffer(16, true);
        buffer.close();
        assertThrows(IllegalStateException.class, buffer::map);
        assertThrows(IllegalStateException.class, buffer::handle);
    }

    @Test
    public void cacheReusesAndEvicts() throws Exception {
        GpuCache<Integer, GpuBuffer> cache = new GpuCache<>(50, TimeUnit.MILLISECONDS);
        GpuBuffer first = cache.get(1, size -> new GpuBuffer(size * 16, true));
        assertSame(first, cache.get(1, size -> new GpuBuffer(size * 16, true)));
        Thread.sleep(100);
        cache.sweep(System.nanoTime());
        assertTrue(first.isClosed());
        assertEquals(0, cache.size());
        assertNotSame(first, cache.get(1, size -> new GpuBuffer(size * 16, true)));
        cache.clear();
    }
}
