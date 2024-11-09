package org.openpnp.gui.support;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public class Icons {
    public static Icon add = getIcon("/icons/general-add.svg");
    public static Icon delete = getIcon("/icons/general-remove.svg");
    public static Icon copy = getIcon("/icons/copy.svg");
    public static Icon paste = getIcon("/icons/paste.svg");
    public static Icon export = getIcon("/icons/export.svg");
    public static Icon importt = getIcon("/icons/import.svg");
    
    public static Icon nozzleAdd = getIcon("/icons/nozzle-add.svg");
    public static Icon nozzleRemove = getIcon("/icons/nozzle-remove.svg");

    public static Icon nozzleTipAdd = getIcon("/icons/nozzletip-add.svg");
    public static Icon nozzleTipRemove = getIcon("/icons/nozzletip-remove.svg");
    public static Icon nozzleTipLoad = getIcon("/icons/nozzletip-load.svg");
    public static Icon nozzleTipUnload = getIcon("/icons/nozzletip-unload.svg");

    public static Icon captureCamera = getIcon("/icons/capture-camera.svg");
    public static Icon captureTool = getIcon("/icons/capture-nozzle.svg");
    public static Icon capturePin = getIcon("/icons/capture-actuator.svg");

    public static Icon centerCamera = getIcon("/icons/position-camera.svg");
    public static Icon centerCameraMoveNext = getIcon("/icons/position-camera-move-next.svg");
    public static Icon centerTool = getIcon("/icons/position-nozzle.svg");
    public static Icon centerToolNoSafeZ = getIcon("/icons/position-nozzle-no-safe-z.svg");
    public static Icon contactProbeNozzle = getIcon("/icons/contact-probe-nozzle.svg");
    public static Icon centerPin = getIcon("/icons/position-actuator.svg");
    public static Icon centerPinNoSafeZ = getIcon("/icons/position-actuator-no-safe-z.svg");
    public static Icon centerCameraOnFeeder = getIcon("/icons/position-camera-on-feeder.svg");
    public static Icon centerNozzleOnFeeder = getIcon("/icons/position-nozzle-on-feeder.svg");

    public static Icon colorFalse = getIcon("/icons/color-false.svg");
    public static Icon colorTrue = getIcon("/icons/color-true.svg");
    
    public static Icon start = getIcon("/icons/control-start.svg");
    public static Icon pause = getIcon("/icons/control-pause.svg");
    public static Icon step = getIcon("/icons/control-next.svg");
    public static Icon stop = getIcon("/icons/control-stop.svg");

    public static Icon twoPointLocate = getIcon("/icons/board-two-placement-locate.svg");
    public static Icon fiducialCheck = getIcon("/icons/board-fiducial-locate.svg");
    public static Icon autoPanelize = getIcon("/icons/panelize.svg");
    public static Icon autoPanelizeXOut = getIcon("/icons/panelize_xout.svg");
    public static Icon autoPanelizeFidCheck = getIcon("/icons/panelize_fiducialcheck.svg");
    public static Icon useChildFiducial = getIcon("/icons/panelize_use_board_fiducial.svg");
    public static Icon board = getIcon("/icons/board.svg");
    public static Icon panel = getIcon("/icons/panel.svg");
    public static Icon clean = getIcon("/icons/clean-sweep.svg");

    public static Icon feed = getIcon("/icons/feeder-feed.svg");
    public static Icon pick = getIcon("/icons/pick.svg");
    public static Icon place = getIcon("/icons/place.svg");
    public static Icon showPart = getIcon("/icons/feeder-show-part-outline.svg");
    public static Icon editFeeder = getIcon("/icons/feeder-edit.svg");
    public static Icon feeder = getIcon("/icons/feeder.svg");
    

    public static Icon partAlign = getIcon("/icons/part-align.svg");

    public static Icon arrowUp = getIcon("/icons/arrow-up.svg");
    public static Icon arrowDown = getIcon("/icons/arrow-down.svg");
    public static Icon arrowLeft = getIcon("/icons/arrow-left.svg");
    public static Icon arrowRight = getIcon("/icons/arrow-right.svg");
    public static Icon home = getIcon("/icons/home.svg");
    public static Icon homeWarning = getIcon("/icons/home_warning.svg");
    public static Icon refresh = getIcon("/icons/refresh.svg");
    public static Icon rotateClockwise = getIcon("/icons/rotate-clockwise.svg");
    public static Icon rotateCounterclockwise = getIcon("/icons/rotate-counterclockwise.svg");
    public static Icon zero = getIcon("/icons/zero.svg");
    
    public static Icon navigateFirst = getIcon("/icons/nav-first.svg");
    public static Icon navigateLast = getIcon("/icons/nav-last.svg");
    public static Icon navigatePrevious = getIcon("/icons/nav-previous.svg");
    public static Icon navigateNext = getIcon("/icons/nav-next.svg");

    public static Icon pinDisabled = getIcon("/icons/pin_disabled.svg");
    public static Icon pinEnabled = getIcon("/icons/pin_enabled.svg");

    public static Icon powerOn = getIcon("/icons/power_button_on.svg");
    public static Icon powerOff = getIcon("/icons/power_button_off.svg");

    public static Icon park = getIcon("/icons/park.svg");

    public static Icon scrollDown = getIcon("/icons/scroll-down.svg");

    public static Icon lockOutline = getIcon("/icons/lock-outline.svg");
    public static Icon lockOpenOutline = getIcon("/icons/lock-open-outline.svg");
    public static Icon lockQuestion = getIcon("/icons/lock-question.svg");

    public static Icon openSCadIcon = getIcon("/icons/openscad-icon.svg");
    public static Icon processActivity1Icon = getIcon("/icons/process-activity-1.svg");
    public static Icon processActivity2Icon = getIcon("/icons/process-activity-2.svg");

    public static Icon axisCartesian = getIcon("/icons/axis-cartesian.svg");
    public static Icon axisRotation = getIcon("/icons/axis-rotate.svg");
    public static Icon captureAxisLow = getIcon("/icons/capture-axis-low.svg");
    public static Icon captureAxisHigh = getIcon("/icons/capture-axis-high.svg");
    public static Icon positionAxisLow = getIcon("/icons/position-axis-low.svg");
    public static Icon positionAxisHigh = getIcon("/icons/position-axis-high.svg");
    public static Icon driver = getIcon("/icons/driver.svg");
    public static Icon solutions = getIcon("/icons/solutions.svg");
    public static Icon accept = getIcon("/icons/accept.svg");
    public static Icon dismiss = getIcon("/icons/dismiss.svg");
    public static Icon undo = getIcon("/icons/undo.svg");
    public static Icon info = getIcon("/icons/info.svg");

    public static Icon nozzleSingle = getIcon("/icons/nozzle-single.svg", 96, 96);
    public static Icon nozzleDualNeg = getIcon("/icons/nozzle-neg.svg", 96, 96);
    public static Icon nozzleDualCam = getIcon("/icons/nozzle-cam.svg", 96, 96);

    public static Icon milestone = getIcon("/icons/milestone.svg", 96, 96);
    public static Icon camAxisTransform = getIcon("/icons/cam-axis-transform.svg", 283, 283);
    public static Icon safeZFixed = getIcon("/icons/safe-z-fixed.svg", 96, 96);
    public static Icon safeZDynamic = getIcon("/icons/safe-z-dynamic.svg", 96, 96);
    public static Icon safeZCapture = getIcon("/icons/safe-z-capture.svg", 96, 96);

    public static Icon footprintQuad = getIcon("/icons/footprint-quad.svg");
    public static Icon footprintDual = getIcon("/icons/footprint-dual.svg");
    public static Icon footprintBga = getIcon("/icons/footprint-bga.svg");
    public static Icon footprintToggle = getIcon("/icons/footprint-mark.svg");
    public static Icon kicad = getIcon("/icons/kicad-logo.svg");

    public static Icon getIcon(String resourceName, int width, int height) {
        if (resourceName.endsWith(".svg")) {
            return new FlatSVGIcon(resourceName.substring(1), width, height);
        }
        else {
            return new ImageIcon(Icons.class.getResource(resourceName));
        }
    }

    public static Icon getIcon(String resourceName) {
        return getIcon(resourceName, 24, 24);
    }
}
