package org.openpnp.gui.support;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Icons {
    public static Icon add = getIcon("/icons/material-design-icons/ic_add_24px.svg");
    public static Icon delete = getIcon("/icons/material-design-icons/ic_remove_24px.svg");
    public static Icon neww = getIcon("/icons/material-design-icons/ic_create_24px.svg");
    
    public static Icon captureCamera = getIcon("/icons/material-design-icons/ic_center_focus_weak_24px_blue.svg");
    public static Icon capturePin = getIcon("/icons/capture-pin.svg");
    public static Icon captureTool = getIcon("/icons/nozzle_blue.svg");
    
    public static Icon centerCamera = getIcon("/icons/material-design-icons/ic_center_focus_weak_24px_red.svg");
    public static Icon centerPin = getIcon("/icons/center-pin.svg");
    public static Icon centerTool = getIcon("/icons/nozzle_red.svg");
    
    public static Icon start = getIcon("/icons/material-design-icons/ic_play_arrow_24px.svg");
    public static Icon pause = getIcon("/icons/material-design-icons/ic_pause_24px.svg");
    public static Icon step = getIcon("/icons/material-design-icons/ic_skip_next_24px.svg");
    public static Icon stop = getIcon("/icons/material-design-icons/ic_stop_24px.svg");
    
    public static Icon load = getIcon("/icons/material-design-icons/ic_file_upload_24px.svg");
    public static Icon unload = getIcon("/icons/material-design-icons/ic_file_download_24px.svg");
    
    public static Icon twoPointLocate = getIcon("/icons/two-point-locate.svg");

    public static Icon feed = getIcon("/icons/material-design-icons/ic_fast_forward_24px.svg");
    public static Icon showPart = getIcon("/icons/material-design-icons/ic_search_24px.svg");

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
