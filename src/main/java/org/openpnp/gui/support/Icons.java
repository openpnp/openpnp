package org.openpnp.gui.support;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Icons {
    public static Icon add = getIcon("/icons/file-add.svg");
    public static Icon delete = getIcon("/icons/file-remove.svg");
    public static Icon neww = getIcon("/icons/file-new.svg");
    
    public static Icon captureCamera = getIcon("/icons/capture-camera.svg");
    public static Icon captureTool = getIcon("/icons/capture-nozzle.svg");
    public static Icon capturePin = getIcon("/icons/capture-actuator.svg");
    
    public static Icon centerCamera = getIcon("/icons/position-camera.svg");
    public static Icon centerTool = getIcon("/icons/position-nozzle.svg");
    public static Icon centerPin = getIcon("/icons/position-actuator.svg");
    
    public static Icon start = getIcon("/icons/control-start.svg");
    public static Icon pause = getIcon("/icons/control-pause.svg");
    public static Icon step = getIcon("/icons/control-next.svg");
    public static Icon stop = getIcon("/icons/control-stop.svg");
    
    public static Icon load = getIcon("/icons/nozzletip-load.svg");
    public static Icon unload = getIcon("/icons/nozzletip-unload.svg");
    
    public static Icon twoPointLocate = getIcon("/icons/board-two-placement-locate.svg");
    public static Icon fiducialCheck = getIcon("/icons/board-fiducial-locate.svg");

    public static Icon feed = getIcon("/icons/feeder-feed.svg");
    public static Icon showPart = getIcon("/icons/feeder-show-part-outline.svg");
    public static Icon editFeeder = getIcon("/icons/feeder-edit.svg");

    public static Icon getIcon(String resourceName, int width, int height) {
        if (resourceName.endsWith(".svg")) {
            return new SvgIcon(Icons.class.getResource(resourceName), width, height);
        }
        else {
            return new ImageIcon(Icons.class.getResource(resourceName));
        }
    }
    
    public static Icon getIcon(String resourceName) {
        return getIcon(resourceName, 24, 24);
    }    
}
