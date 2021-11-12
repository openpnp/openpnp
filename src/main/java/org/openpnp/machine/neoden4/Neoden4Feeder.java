package org.openpnp.machine.neoden4;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.neoden4.wizards.Neoden4FeederConfigurationWizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Rectangle;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider;
import org.pmw.tinylog.Logger;
import org.python.modules.thread.thread;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;


public class Neoden4Feeder extends ReferenceFeeder {

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    
    @Attribute(required = false)
    protected String actuatorName;

    @Attribute(required = false)
    private int feedCount = 0;

    @Element(required = false)
    private Length partPitchInTape = new Length(4, LengthUnit.Millimeters);

    @Element(required = false)
    private int partRotationInTape = 0;
    
    @Element(required = false)
    protected Vision vision = new Vision();

    protected Location pickLocation;

    /*
     * visionOffset contains the difference between where the part was expected to be and where it
     * is. Subtracting these offsets from the pickLocation produces the correct pick location.
     */
    protected Location visionOffset;

    public Length getPartPitchInTape() {
        return partPitchInTape;
    }

    public void setPartPitchInTape(Length newPartPitchInTape)
    {
        Object oldValue = this.partPitchInTape;
        this.partPitchInTape = newPartPitchInTape;
        firePropertyChange("partPitchInTape", oldValue, newPartPitchInTape);
    }

    public int getPartRotationInTape() {
        return partRotationInTape;
    }

    public void setPartRotationInTape(int newPartRotationInTape) {
        Object oldValue = this.partRotationInTape;
        this.partRotationInTape = newPartRotationInTape;
        firePropertyChange("partRotationInTape", oldValue, newPartRotationInTape);
    }

    @Override
    public Location getPickLocation() throws Exception {
        pickLocation = location.derive(null, null, null, location.getRotation() + partRotationInTape);

        if (vision.isEnabled() && visionOffset != null) {
			return pickLocation.subtract(visionOffset);
        }

        return pickLocation;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        Logger.debug("feed({})", nozzle);

        if (actuatorName == null) {
            throw new Exception("No actuator name set.");
        }

        Head head = nozzle.getHead();

		Actuator actuator = Configuration.get().getMachine().getActuatorByName(actuatorName);
		if (actuator == null) {
			throw new Exception(
					String.format("No Actuator found with name %s on feed Head %s", actuatorName, head.getName()));
		}

        // Actuate actuator 
        actuator.actuate(partPitchInTape.getValue());

    	// Calculate vision offset
    	
    	
        if (vision.isEnabled()) {
        	try {
        		visionOffset = getVisionOffsets(head, location);
                Logger.debug("final visionOffsets " + visionOffset);
                Logger.debug("Modified pickLocation {}", getPickLocation());	
        	} catch (Exception e) {
        		
			}
        }

        setFeedCount(getFeedCount() + 1);
    }

    private Location getVisionOffsets(Head head, Location pickLocation) throws Exception {
        Logger.debug("getVisionOffsets({}, {})", head.getName(), pickLocation);
        
        // Find the Camera to be used for vision
        Camera camera = null;
        for (Camera c : head.getCameras()) {
            if (c.getVisionProvider() != null) {
                camera = c;
            }
        }

        if (camera == null) {
            throw new Exception("No vision capable camera found on head.");
        }
        
        if (vision.getTemplateImage() == null) {
            throw new Exception("Template image is required when vision is enabled.");
        }
        
        if (vision.getAreaOfInterest().getWidth() == 0 || vision.getAreaOfInterest().getHeight() == 0) {
            throw new Exception("Area of Interest is required when vision is enabled.");
        }

        head.moveToSafeZ();

        // Position the camera over the pick location.
        Logger.debug("Move camera to pick location.");
        camera.moveTo(pickLocation);

        // Move the camera to be in focus over the pick location.
        // head.moveTo(head.getX(), head.getY(), z, head.getC());

        VisionProvider visionProvider = camera.getVisionProvider();

        // Convert AOI origin to top-left corner (Neoden4Camera changes resolution)
        Rectangle vision_aoi = getVision().getAreaOfInterest();        
		Rectangle aoi = new Rectangle(
				vision_aoi.getX() + (camera.getWidth() / 2), 
				vision_aoi.getY() + (camera.getHeight() / 2), 
				vision_aoi.getWidth(), 
				vision_aoi.getHeight());


		// Check if there is no bug like:
		// - user set camera tp 1024x1024
		// - user select AOI almost full screen and save
		// - when camera is 512x512 and feeder is checking
		//   vision offsets, vision_aoi can be < 0 or > 512 
		//   and cause openCV error
		// If there is, clamp AOI to 512x512
        if (aoi.getX() < 0) {
        	aoi.setX(0); 
        }
        if (aoi.getX() > 512) {
        	aoi.setX(512); 
        }
        if (aoi.getY() < 0) {
        	aoi.setY(0); 
        }
        if (aoi.getY() > 512) {
        	aoi.setY(512); 
        }
        if (aoi.getWidth() < 0) {
        	aoi.setWidth(0); 
        }
        if (aoi.getWidth() > 512) {
        	aoi.setWidth(512); 
        }
        if (aoi.getHeight() < 0) {
        	aoi.setHeight(0); 
        }
        if (aoi.getHeight() > 512) {
        	aoi.setHeight(512); 
        }
		
		// Perform the template match
		Logger.debug("Perform template match.");
		Logger.debug(String.format("AOI X:%d, Y:%d, W:%d, H:%d",
				aoi.getX(), aoi.getY(), aoi.getWidth(), aoi.getHeight()));
		
		try {
			Point[] matchingPoints = visionProvider.locateTemplateMatches(
					aoi.getX(), aoi.getY(), aoi.getWidth(), aoi.getHeight(), 
					0, 0, vision.getTemplateImage());
		
			// Get the best match from the array
	        Point match = matchingPoints[0];
	
	        // match now contains the position, in pixels, from the top left corner
	        // of the image to the top left corner of the match. We are interested in
	        // knowing how far from the center of the image the center of the match is.
	        double imageWidth = camera.getWidth();
	        double imageHeight = camera.getHeight();
	        double templateWidth = vision.getTemplateImage().getWidth();
	        double templateHeight = vision.getTemplateImage().getHeight();
	        double matchX = match.x;
	        double matchY = match.y;
	
	        Logger.debug("matchX {}, matchY {}", matchX, matchY);
	
	        // Adjust the match x and y to be at the center of the match instead of
	        // the top left corner.
	        matchX += (templateWidth / 2);
	        matchY += (templateHeight / 2);
	
	        Logger.debug("centered matchX {}, matchY {}", matchX, matchY);
	
	        // Calculate the difference between the center of the image to the
	        // center of the match.
	        double offsetX = (imageWidth / 2) - matchX;
	        double offsetY = (imageHeight / 2) - matchY;
	
	        Logger.debug("offsetX {}, offsetY {}", offsetX, offsetY);
	
	        // Invert the Y offset because images count top to bottom and the Y
	        // axis of the machine counts bottom to top.
	        offsetY *= -1;
	
	        Logger.debug("negated offsetX {}, offsetY {}", offsetX, offsetY);
	
	        // And convert pixels to units
	        Location unitsPerPixel = camera.getUnitsPerPixel();
	        offsetX *= unitsPerPixel.getX();
	        offsetY *= unitsPerPixel.getY();
	
	        Logger.debug("final, in camera units offsetX {}, offsetY {}", offsetX, offsetY);
	
	        return new Location(unitsPerPixel.getUnits(), offsetX, offsetY, 0, 0);
		}
        catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
    }

    @Override
    public String toString() {
        return String.format("ReferenceTapeFeeder id %s", id);
    }

	public void resetVisionOffsets() {
		if (visionOffset != null) {
			visionOffset = null;
			Logger.debug("resetVisionOffsets " + visionOffset);
		}
	}

	public String getActuatorName() {
        return actuatorName;
    }

    public void setActuatorName(String actuatorName) {
        String oldValue = this.actuatorName;
        this.actuatorName = actuatorName;
        propertyChangeSupport.firePropertyChange("actuatorName", oldValue, actuatorName);
    }

    public int getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(int feedCount) {
    	// TODO: Consider reseting value when new part is set
        int oldValue = this.feedCount;
        this.feedCount = feedCount;
        firePropertyChange("feedCount", oldValue, feedCount);
    }
    
    public Vision getVision() {
        return vision;
    }

    public void setVision(Vision vision) {
        this.vision = vision;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new Neoden4FeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    public static class Vision {
        @Attribute(required = false)
        private boolean enabled;
        @Attribute(required = false)
        private String templateImageName;
        @Element(required = false)
        private Rectangle areaOfInterest = new Rectangle();
        @Element(required = false)
        private Location templateImageTopLeft = new Location(LengthUnit.Millimeters);
        @Element(required = false)
        private Location templateImageBottomRight = new Location(LengthUnit.Millimeters);

        private BufferedImage templateImage;
        private boolean templateImageDirty;

        public Vision() {
            Configuration.get().addListener(new ConfigurationListener.Adapter() {
                @Override
                public void configurationComplete(Configuration configuration) throws Exception {
                    if (templateImageName != null) {
                        File file = configuration.getResourceFile(Vision.this.getClass(),
                                templateImageName);
                        try {
                        	templateImage = ImageIO.read(file);
                        }
                    	catch(IOException exception) {
                    		enabled = false;
                    		Logger.warn("Cannot load template image: {} ", templateImageName);
                    	}
                    }
                }
            });
        }

        @SuppressWarnings("unused")
        @Persist
        private void persist() throws IOException {
            if (templateImageDirty) {
                File file = null;
                if (templateImageName != null) {
                    file = Configuration.get().getResourceFile(this.getClass(), templateImageName);
                }
                else {
                    file = Configuration.get().createResourceFile(this.getClass(), "tmpl_", ".png");
                    templateImageName = file.getName();
                }
                ImageIO.write(templateImage, "png", file);
                templateImageDirty = false;
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public BufferedImage getTemplateImage() {
            return templateImage;
        }

        public void setTemplateImage(BufferedImage templateImage) {
            if (templateImage != this.templateImage) {
                this.templateImage = templateImage;
                templateImageDirty = true;
            }
        }

        public Rectangle getAreaOfInterest() {
            return areaOfInterest;
        }

        public void setAreaOfInterest(Rectangle areaOfInterest) {
            this.areaOfInterest = areaOfInterest;
        }

        public Location getTemplateImageTopLeft() {
            return templateImageTopLeft;
        }

        public void setTemplateImageTopLeft(Location templateImageTopLeft) {
            this.templateImageTopLeft = templateImageTopLeft;
        }

        public Location getTemplateImageBottomRight() {
            return templateImageBottomRight;
        }

        public void setTemplateImageBottomRight(Location templateImageBottomRight) {
            this.templateImageBottomRight = templateImageBottomRight;
        }
    }
}