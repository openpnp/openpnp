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
import java.awt.image.DataBufferByte;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.hwgc.wizards.HwgcUsb3CameraConfigurationWizard;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * Camera driver for the CatchBest UB500MR-M USB3 industrial camera via ChinaVision MVCAMSDK.
 * Captures 2592x1944 monochrome frames with ISP processing to BGR24.
 */
public class HwgcUsb3Camera extends ReferenceCamera {

    @Attribute(required = false)
    private String sdkPath = "C:/Program Files (x86)/ChinaVision/SDK/X64/MVCAMSDK_X64.dll";

    @Attribute(required = false)
    private int captureTimeoutMs = 2000;

    private MvCamSdk sdk;
    private int hCamera = -1;
    private boolean opened;

    // Reusable buffers
    private Memory ispBuffer;
    private final Memory frameHead = new Memory(256);
    private int lastWidth;
    private int lastHeight;

    public HwgcUsb3Camera() {
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        if (!ensureOpen()) {
            return null;
        }

        frameHead.clear(256);
        PointerByReference ppBuf = new PointerByReference();

        int ret = sdk.CameraGetImageBuffer(hCamera, frameHead, ppBuf, captureTimeoutMs);
        if (ret != 0 || ppBuf.getValue() == null) {
            // Try software trigger fallback
            try {
                sdk.CameraSoftTrigger(hCamera);
                Thread.sleep(50);
                frameHead.clear(256);
                ret = sdk.CameraGetImageBuffer(hCamera, frameHead, ppBuf, captureTimeoutMs);
            }
            catch (Exception e) {
                // ignore
            }
            if (ret != 0 || ppBuf.getValue() == null) {
                Logger.warn("HWGC USB3: CameraGetImageBuffer failed (ret={})", ret);
                return null;
            }
        }

        try {
            // Parse frame header to find width/height
            // MindVision tSFrameHead: offsets vary — find plausible width/height pair
            int width = 0;
            int height = 0;
            int[] vals = new int[5];
            for (int i = 0; i < 5; i++) {
                vals[i] = frameHead.getInt(i * 4);
            }
            for (int i = 0; i < vals.length - 1; i++) {
                if (vals[i] >= 100 && vals[i] <= 10000
                        && vals[i + 1] >= 100 && vals[i + 1] <= 10000) {
                    width = vals[i];
                    height = vals[i + 1];
                    break;
                }
            }
            if (width <= 0 || height <= 0) {
                // Fallback: UB500MR default 5MP resolution
                width = 2592;
                height = 1944;
            }

            // Allocate/reuse ISP output buffer
            int outSize = width * height * 3;
            if (ispBuffer == null || width != lastWidth || height != lastHeight) {
                ispBuffer = new Memory(outSize);
                lastWidth = width;
                lastHeight = height;
            }

            ret = sdk.CameraImageProcess(hCamera, ppBuf.getValue(), ispBuffer, frameHead);
            if (ret != 0) {
                Logger.warn("HWGC USB3: CameraImageProcess failed (ret={})", ret);
                return null;
            }

            // Create BufferedImage from BGR data
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] pixels = ispBuffer.getByteArray(0, outSize);
            byte[] imgData = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            System.arraycopy(pixels, 0, imgData, 0, Math.min(pixels.length, imgData.length));

            return img;
        }
        finally {
            sdk.CameraReleaseImageBuffer(hCamera, ppBuf.getValue());
        }
    }

    @Override
    protected synchronized boolean ensureOpen() {
        if (opened) {
            return true;
        }

        sdk = MvCamSdk.tryLoad(sdkPath);
        if (sdk == null) {
            Logger.error("HWGC USB3: MVCAMSDK_X64.dll not found at {}", sdkPath);
            return false;
        }

        sdk.CameraSdkInit(1); // 1 = English

        int maxDevs = 16;
        Memory devList = new Memory((long) MvCamSdk.DEV_INFO_SIZE * maxDevs);
        devList.clear((long) MvCamSdk.DEV_INFO_SIZE * maxDevs);
        IntByReference devNum = new IntByReference(maxDevs);

        int ret = sdk.CameraEnumerateDevice(devList, devNum);
        if (ret != 0 || devNum.getValue() <= 0) {
            Logger.error("HWGC USB3: no camera found (ret={}, count={})", ret, devNum.getValue());
            return false;
        }

        // Read device name for logging
        try {
            String name = devList.getString(64);
            if (name != null && !name.isEmpty()) {
                Logger.info("HWGC USB3: found camera '{}'", name);
            }
        }
        catch (Exception e) {
            // ignore
        }

        IntByReference handle = new IntByReference();
        ret = sdk.CameraInit(devList, -1, -1, handle);
        if (ret != 0) {
            Logger.error("HWGC USB3: CameraInit failed (ret={})", ret);
            return false;
        }
        hCamera = handle.getValue();

        sdk.CameraSetTriggerMode(hCamera, MvCamSdk.TRIGGER_MODE_CONTINUOUS);
        sdk.CameraSetIspOutFormat(hCamera, MvCamSdk.ISP_FORMAT_BGR24);
        try {
            sdk.CameraSetFrameSpeed(hCamera, MvCamSdk.FRAME_SPEED_HIGH);
        }
        catch (Error e) {
            // ignore
        }
        sdk.CameraPlay(hCamera);

        opened = true;
        Logger.info("HWGC USB3 camera opened (handle={})", hCamera);
        // Let the parent class start the capture thread
        return super.ensureOpen();
    }

    @Override
    public synchronized void close() throws java.io.IOException {
        if (!opened) {
            return;
        }
        opened = false;
        if (hCamera >= 0 && sdk != null) {
            try {
                sdk.CameraStop(hCamera);
            }
            catch (Error e) {
                // ignore
            }
            try {
                sdk.CameraUnInit(hCamera);
            }
            catch (Error e) {
                // ignore
            }
            hCamera = -1;
            Logger.info("HWGC USB3 camera closed");
        }
        super.close();
    }

    // ── Configuration ──

    public String getSdkPath() {
        return sdkPath;
    }

    public void setSdkPath(String sdkPath) {
        this.sdkPath = sdkPath;
    }

    public int getCaptureTimeoutMs() {
        return captureTimeoutMs;
    }

    public void setCaptureTimeoutMs(int captureTimeoutMs) {
        this.captureTimeoutMs = captureTimeoutMs;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new HwgcUsb3CameraConfigurationWizard(this);
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
