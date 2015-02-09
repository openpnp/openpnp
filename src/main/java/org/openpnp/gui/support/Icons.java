package org.openpnp.gui.support;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Icons {
    public static Icon add = getIcon("/icons/add.png");
    public static Icon arrowDown = getIcon("/icons/arrow-down.png");
    public static Icon arrowUp = getIcon("/icons/arrow-up.png");
    public static Icon captureCamera = getIcon("/icons/capture-camera.png");
    public static Icon capturePin = getIcon("/icons/capture-pin.png");
    public static Icon captureToolZ = getIcon("/icons/capture-tool-z.png");
    public static Icon captureTool = getIcon("/icons/capture-tool.png");
    public static Icon centerCamera = getIcon("/icons/center-camera.png");
    public static Icon centerPin = getIcon("/icons/center-pin.png");
    public static Icon centerTool = getIcon("/icons/center-tool.png");
    public static Icon delete = getIcon("/icons/delete.png");
    public static Icon feed = getIcon("/icons/feed.png");
    public static Icon load = getIcon("/icons/load.png");
    public static Icon neww = getIcon("/icons/new.png");
    public static Icon pause = getIcon("/icons/pause.png");
    public static Icon showPart = getIcon("/icons/show-part.png");
    public static Icon start = getIcon("/icons/start.png");
    public static Icon step = getIcon("/icons/step.png");
    public static Icon stop = getIcon("/icons/stop.png");
    public static Icon twoPointLocate = getIcon("/icons/two-point-locate.png");
    public static Icon unload = getIcon("/icons/unload.png");
    
    public static Icon getIcon(String resourceName) {
        if (resourceName.endsWith(".svg")) {
            return new SvgIcon(Icons.class.getResource(resourceName), 24, 24);
        }
        else {
            return new ImageIcon(Icons.class.getResource(resourceName));
        }
    }
}
