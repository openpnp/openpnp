package org.openpnp.vision.gpu;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.pmw.tinylog.Logger;

/**
 * Loads the OpenCV T-API (OpenCL) shim and reports whether a GPU OpenCL device is usable.
 */
public class OclSupport {
    private static final String LIBRARY = "/native/linux-x86-64/libopenpnp_ocl.so";

    private static Boolean available;

    static native String deviceInfo();

    public static synchronized boolean isAvailable() {
        if (available == null) {
            available = load();
        }
        return available;
    }

    private static boolean load() {
        if (!System.getProperty("os.name").equals("Linux") || !System.getProperty("os.arch").equals("amd64")) {
            Logger.info("OpenCL image processing is only supported on Linux x86_64.");
            return false;
        }
        try (InputStream in = OclSupport.class.getResourceAsStream(LIBRARY)) {
            if (in == null) {
                Logger.info("OpenCL image processing unavailable: {} was not built.", LIBRARY);
                return false;
            }
            nu.pattern.OpenCV.loadLocally();
            File file = File.createTempFile("openpnp_ocl", ".so");
            file.deleteOnExit();
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(file.getAbsolutePath());
            String device = deviceInfo();
            if (device.isEmpty()) {
                Logger.info("OpenCL image processing unavailable: no OpenCL GPU device (is intel-opencl-icd installed?).");
                return false;
            }
            Logger.info("OpenCL image processing on {}", device);
            return true;
        }
        catch (Throwable e) {
            Logger.warn(e, "OpenCL image processing unavailable");
            return false;
        }
    }
}
