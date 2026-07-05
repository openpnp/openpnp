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
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import org.pmw.tinylog.Logger;

/**
 * JNA interface for MVCAMSDK_X64.dll — the ChinaVision (MindVision) camera SDK.
 * Controls the CatchBest UB500MR-M USB3 industrial camera (5MP, 2592x1944, monochrome).
 *
 * Method names match the DLL exports exactly (PascalCase).
 * Checkstyle MethodName rule is suppressed for this file via pom.xml.
 */
public interface MvCamSdk extends Library {

    int CameraSdkInit(int lang);
    int CameraEnumerateDevice(Pointer devList, IntByReference devNum);
    int CameraInit(Pointer devInfo, int paramMode, int team, IntByReference handle);
    int CameraUnInit(int hCamera);
    int CameraPlay(int hCamera);
    int CameraStop(int hCamera);
    int CameraSetIspOutFormat(int hCamera, int format);
    int CameraSetTriggerMode(int hCamera, int mode);
    int CameraSetFrameSpeed(int hCamera, int speed);
    int CameraGetImageBuffer(int hCamera, Pointer frameHead,
            PointerByReference ppBuf, int timeout);
    int CameraImageProcess(int hCamera, Pointer pRaw, Pointer pOut, Pointer frameHead);
    int CameraReleaseImageBuffer(int hCamera, Pointer pBuf);
    int CameraSoftTrigger(int hCamera);

    /** ISP output format: BGR24. */
    int ISP_FORMAT_BGR24 = 3;
    /** Trigger mode: continuous. */
    int TRIGGER_MODE_CONTINUOUS = 0;
    /** Frame speed: high. */
    int FRAME_SPEED_HIGH = 2;
    /** Size of one tSdkCameraDevInfo struct. */
    int DEV_INFO_SIZE = 576;

    /**
     * Load the SDK from a specific path. Returns null if the DLL is not available.
     */
    static MvCamSdk tryLoad(String dllPath) {
        try {
            return Native.load(dllPath, MvCamSdk.class);
        }
        catch (UnsatisfiedLinkError e) {
            Logger.warn("HWGC USB3: MVCAMSDK not found at {}: {}", dllPath, e.getMessage());
            return null;
        }
    }
}
