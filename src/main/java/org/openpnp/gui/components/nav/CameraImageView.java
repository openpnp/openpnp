package org.openpnp.gui.components.nav;

import java.awt.image.BufferedImage;

import org.openpnp.CameraListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.UiUtils;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

class CameraImageView extends ImageView implements CameraListener {
    final Camera camera;
    final double width;
    final double height;
    
    public CameraImageView(Camera camera) {
        this.camera = camera;
        
        Location unitsPerPixel =
                camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
        width = unitsPerPixel.getX() * camera.getWidth();
        height = unitsPerPixel.getY() * camera.getHeight();
        setFitWidth(width);
        setFitHeight(height);
        // Images are flipped with respect to display coordinates, so
        // flip em back.
        setScaleY(-1);
//        setTranslateX(-width / 2);
//        setTranslateY(-height / 2);
        
        // TODO STOPSHIP bottom vision camera image doesn't seem to appear in the right
        // place.
        
        // bottom vision camera image seems offset
        // actually, need to check offsets of all items
        // especially nozzle and feeder and actuator
        // it would be cool if you could easily center the camera over
        // an object without having to go find it's position and click the
        // locate button
        // so like just click a feeder and the camera goes
        
        // also, the whole damn interface should just be nav view
        // right click to add new things, click things to configure
        // them, etc.
        
        // also, also, binding tooltips to things like names is bad since they
        // might change during runtime. they need to be bound to properties.
        
        
        setOnMouseClicked(event -> {
            setOpacity(getOpacity() == 1 ? 0.20 : 1);
        });
        
        camera.startContinuousCapture(this, 10);

        UiUtils.bindTooltip(this, new Tooltip(camera.getName()));
        
        updateLocation();
        
        Configuration.get().getMachine().addListener(machineListener);
    }
    
    private void updateLocation() {
        Platform.runLater(() -> {
            Location location = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
//            setX(location.getX());
//            setY(location.getY());
          setTranslateX(location.getX() - width / 2);
          setTranslateY(location.getY() - height / 2);
        });
    }
    

    public void frameReceived(BufferedImage img) {
        if (img == null) {
            return;
        }
        Platform.runLater(() -> setImage(SwingFXUtils.toFXImage(img, null)));
    }
    
    MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineHeadActivity(Machine machine, Head head) {
            updateLocation();
        }

        @Override
        public void machineEnabled(Machine machine) {
            updateLocation();
        }
    };    
}