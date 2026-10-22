package org.openpnp.vision.gpu;

import java.io.File;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.openpnp.vision.gpu.GpuPipeline.Binding;
import org.pmw.tinylog.Logger;

/**
 * Loads the Vulkan compute runtime and reports whether a working GPU is available.
 */
public class GpuRuntime {
    private static final String LIBRARY = "/native/linux-x86-64/libopenpnp_vk.so";

    private static Boolean available;
    private static String deviceName = "";

    public static synchronized boolean isAvailable() {
        if (available == null) {
            available = load();
        }
        return available;
    }

    public static String getDeviceName() {
        return isAvailable() ? deviceName : "";
    }

    /**
     * Whether shaders may use 64-bit integers.
     */
    public static boolean hasInt64() {
        return isAvailable() && GpuNative.hasInt64();
    }

    /**
     * Whether shaders may read and write single bytes of storage buffers.
     */
    public static boolean hasByteStorage() {
        return isAvailable() && GpuNative.hasByteStorage();
    }

    /**
     * Whether shaders may pick one of an array of buffers with a parameter.
     */
    public static boolean hasArrayIndexing() {
        return isAvailable() && GpuNative.hasArrayIndexing();
    }

    public static long completed() {
        return GpuNative.completed();
    }

    /**
     * Sleeps until the GPU has finished all work up to the given submit value.
     */
    public static void await(long value, long timeoutNs) {
        if (!GpuNative.await(value, timeoutNs)) {
            throw new IllegalStateException("GPU work timed out");
        }
    }

    private static boolean load() {
        if (!System.getProperty("os.name").equals("Linux") || !System.getProperty("os.arch").equals("amd64")) {
            Logger.info("GPU image processing is only supported on Linux x86_64.");
            return false;
        }
        try (InputStream in = GpuRuntime.class.getResourceAsStream(LIBRARY)) {
            if (in == null) {
                Logger.info("GPU image processing unavailable: {} was not built.", LIBRARY);
                return false;
            }
            File file = File.createTempFile("openpnp_vk", ".so");
            file.deleteOnExit();
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(file.getAbsolutePath());
            deviceName = GpuNative.init();
            selfTest();
            Logger.info("GPU image processing on {} (Vulkan)", deviceName);
            return true;
        }
        catch (Throwable e) {
            Logger.warn(e, "GPU image processing unavailable");
            return false;
        }
    }

    private static void selfTest() {
        int count = 100;
        try (GpuPipeline pipeline = new GpuPipeline("selftest", new int[] { 3 }, Binding.Uniform, Binding.Storage,
                Binding.Storage);
                GpuBuffer params = new GpuBuffer(16, true);
                GpuBuffer src = new GpuBuffer(count * 4, true);
                GpuBuffer dst = new GpuBuffer(count * 4, true);
                GpuProgram program = new GpuProgram.Builder().dispatch(pipeline, (count + 63) / 64, 1, 1, params,
                        src, dst).build()) {
            params.map().putInt(0, count).putInt(4, 7);
            IntBuffer in = src.map().asIntBuffer();
            for (int i = 0; i < count; i++) {
                in.put(i, i);
            }
            program.run();
            IntBuffer out = dst.map().asIntBuffer();
            for (int i = 0; i < count; i++) {
                if (out.get(i) != i * 3 + 7) {
                    throw new IllegalStateException("GPU self test computed " + out.get(i) + " instead of "
                            + (i * 3 + 7));
                }
            }
        }
    }
}
