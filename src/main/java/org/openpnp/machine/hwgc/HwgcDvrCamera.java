/*
 * Copyright (C) 2026 Contributed by Arnoud @ DeltaProto <arnoud@deltaproto.com>
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.hwgc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.hwgc.wizards.HwgcDvrCameraConfigurationWizard;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * Camera driver for HWGC DVR capture card channels (hwsys.dll).
 * Captures 1920x1080 AHD frames from the Cyclone IV FPGA + NVP6114 PCI-E card.
 *
 * Typical channel mapping on SMT550:
 *   - Channels 0-3: Down-looking cameras (fiducial/mark detection)
 *   - Channel 4: Up-looking camera (component alignment)
 *   - Channels 5-7: Unused (no camera connected)
 */
public class HwgcDvrCamera extends ReferenceCamera {

    @Attribute(required = false)
    private int channel = 0;

    // Shared singleton — one InitHwDSPs() call for all camera instances
    private static HwgcDvrSdk sharedSdk;
    private static int sharedChannelCount;
    private static int openCount;
    private static final Object LOCK = new Object();

    private boolean opened;
    private File tempDir;

    public HwgcDvrCamera() {
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        if (!ensureOpen()) {
            return null;
        }

        if (channel >= sharedChannelCount) {
            Logger.warn("HWGC DVR: channel {} not available (total: {})", channel, sharedChannelCount);
            return null;
        }

        String path = new File(tempDir, "dvr_ch" + channel + ".bmp").getAbsolutePath();

        int ret;
        synchronized (LOCK) {
            ret = sharedSdk.SaveCaptureImage(channel, path);
        }

        if (ret != 0) {
            Logger.warn("HWGC DVR: SaveCaptureImage ch{} returned {}", channel, ret);
            return null;
        }

        File f = new File(path);
        if (!f.exists() || f.length() < 100) {
            Logger.warn("HWGC DVR: ch{} file missing or too small: exists={}, size={}",
                    channel, f.exists(), f.exists() ? f.length() : 0);
            return null;
        }

        try {
            // Fix BMP header — hwsys.dll writes incorrect file size fields
            try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
                long realSize = raf.length();
                byte[] sizeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt((int) realSize).array();
                raf.seek(2);
                raf.write(sizeBytes);
                byte[] imgSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt((int) (realSize - 54)).array();
                raf.seek(34);
                raf.write(imgSize);
            }
            BufferedImage img = ImageIO.read(f);
            if (img == null) {
                Logger.warn("HWGC DVR: ch{} ImageIO.read returned null for {} ({} bytes)",
                        channel, path, f.length());
            } else {
                Logger.trace("HWGC DVR: ch{} captured {}x{}", channel, img.getWidth(), img.getHeight());
            }
            return img;
        }
        catch (Exception e) {
            Logger.warn("HWGC DVR: failed to read BMP for ch{}: {}", channel, e.getMessage());
            return null;
        }
    }

    @Override
    protected synchronized boolean ensureOpen() {
        if (opened) {
            return true;
        }
        synchronized (LOCK) {
            if (sharedSdk == null) {
                sharedSdk = HwgcDvrSdk.tryLoad();
                if (sharedSdk == null) {
                    Logger.error("HWGC DVR: hwsys.dll not found. "
                            + "Set -Djna.library.path to the DLL directory.");
                    return false;
                }

                // Init with retry — first attempt may fail if called too early
                boolean initialized = false;
                for (int attempt = 0; attempt < 3; attempt++) {
                    try {
                        sharedSdk.InitHwDSPs();
                        sharedChannelCount = sharedSdk.GetVideoTotalChannels();
                        Logger.info("HWGC DVR: initialized, {} channels available",
                                sharedChannelCount);

                        // Open ALL channels at once (DVR hardware requires batch init)
                        boolean anySuccess = false;
                        for (int i = 0; i < sharedChannelCount; i++) {
                            try {
                                sharedSdk.VideoChannelOpen(i);
                                sharedSdk.StartVideoPreview(i);
                                anySuccess = true;
                            }
                            catch (Error e) {
                                Logger.trace("HWGC DVR: channel {} failed on attempt {}: {}",
                                        i, attempt + 1, e.getMessage());
                            }
                        }
                        if (anySuccess) {
                            initialized = true;
                            break;
                        }
                    }
                    catch (Error e) {
                        Logger.warn("HWGC DVR: init attempt {} failed: {}",
                                attempt + 1, e.getMessage());
                    }
                    // Wait before retry
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        sharedSdk = null;
                        return false;
                    }
                    // Re-init on next attempt
                    try {
                        sharedSdk.DeInitHwDSPs();
                    }
                    catch (Error e) {
                        // ignore
                    }
                }
                if (!initialized) {
                    Logger.error("HWGC DVR: failed to initialize after 3 attempts");
                    sharedSdk = null;
                    return false;
                }
            }
            openCount++;
        }

        tempDir = new File(System.getProperty("java.io.tmpdir"), "hwgc_dvr_cap");
        tempDir.mkdirs();
        opened = true;
        Logger.info("HWGC DVR camera opened: channel {}", channel);
        // Let the parent class start the capture thread
        return super.ensureOpen();
    }

    @Override
    public synchronized void close() throws java.io.IOException {
        if (!opened) {
            return;
        }
        opened = false;
        synchronized (LOCK) {
            openCount--;
            if (openCount <= 0 && sharedSdk != null) {
                for (int i = 0; i < sharedChannelCount; i++) {
                    try {
                        sharedSdk.StopVideoCapture(i);
                    }
                    catch (Error e) {
                        // ignore
                    }
                }
                try {
                    sharedSdk.DeInitHwDSPs();
                }
                catch (Error e) {
                    // ignore
                }
                sharedSdk = null;
                openCount = 0;
                Logger.info("HWGC DVR: shutdown");
            }
        }
        super.close();
    }

    // ── Configuration ──

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new HwgcDvrCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }
}
