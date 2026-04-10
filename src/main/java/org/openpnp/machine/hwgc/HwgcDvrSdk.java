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

import com.sun.jna.Library;
import com.sun.jna.Native;

import org.pmw.tinylog.Logger;

/**
 * JNA interface for hwsys.dll — the HWGC DVR capture card SDK.
 * Controls the Altera Cyclone IV FPGA + Nextchip NVP6114 PCI-E capture card
 * that interfaces with up to 8 AHD analog cameras (1920x1080).
 *
 * Method names match the DLL exports exactly (PascalCase).
 * Checkstyle MethodName rule is suppressed for this file via pom.xml.
 */
public interface HwgcDvrSdk extends Library {

    int InitHwDSPs();
    int DeInitHwDSPs();
    int GetVideoTotalChannels();
    int VideoChannelOpen(int ch);
    int StartVideoPreview(int ch);
    int StopVideoCapture(int ch);
    int SaveCaptureImage(int ch, String filename);

    /**
     * Load the SDK. Returns null if the DLL is not available.
     */
    static HwgcDvrSdk tryLoad() {
        try {
            return Native.load("hwsys", HwgcDvrSdk.class);
        }
        catch (UnsatisfiedLinkError e) {
            Logger.warn("HWGC DVR: hwsys.dll not found: {}", e.getMessage());
            return null;
        }
    }
}
