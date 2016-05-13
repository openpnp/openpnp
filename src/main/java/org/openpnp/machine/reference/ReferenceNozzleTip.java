package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceNozzleTip extends AbstractNozzleTip {
    private final static Logger logger = LoggerFactory.getLogger(ReferenceNozzleTip.class);

    @ElementList(required = false, entry = "id")
    private Set<String> compatiblePackageIds = new HashSet<>();

    @Attribute(required = false)
    private boolean allowIncompatiblePackages;

    @Element(required = false)
    private Location changerStartLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerMidLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerEndLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Calibration calibration = new Calibration();

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();

    public ReferenceNozzleTip() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                for (String id : compatiblePackageIds) {
                    org.openpnp.model.Package pkg = configuration.getPackage(id);
                    if (pkg == null) {
                        continue;
                    }
                    compatiblePackages.add(pkg);
                }
            }
        });
    }

    @Override
    public boolean canHandle(Part part) {
        boolean result =
                allowIncompatiblePackages || compatiblePackages.contains(part.getPackage());
        logger.debug("{}.canHandle({}) => {}", new Object[] {getName(), part.getId(), result});
        return result;
    }

    public Set<org.openpnp.model.Package> getCompatiblePackages() {
        return new HashSet<>(compatiblePackages);
    }

    public void setCompatiblePackages(Set<org.openpnp.model.Package> compatiblePackages) {
        this.compatiblePackages.clear();
        this.compatiblePackages.addAll(compatiblePackages);
        compatiblePackageIds.clear();
        for (org.openpnp.model.Package pkg : compatiblePackages) {
            compatiblePackageIds.add(pkg.getId());
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceNozzleTipConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {unloadAction, loadAction};
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    public boolean isAllowIncompatiblePackages() {
        return allowIncompatiblePackages;
    }

    public void setAllowIncompatiblePackages(boolean allowIncompatiblePackages) {
        this.allowIncompatiblePackages = allowIncompatiblePackages;
    }

    public Location getChangerStartLocation() {
        return changerStartLocation;
    }

    public void setChangerStartLocation(Location changerStartLocation) {
        this.changerStartLocation = changerStartLocation;
    }

    public Location getChangerMidLocation() {
        return changerMidLocation;
    }

    public void setChangerMidLocation(Location changerMidLocation) {
        this.changerMidLocation = changerMidLocation;
    }

    public Location getChangerEndLocation() {
        return changerEndLocation;
    }

    public void setChangerEndLocation(Location changerEndLocation) {
        this.changerEndLocation = changerEndLocation;
    }

    private Nozzle getParentNozzle() {
        for (Head head : Configuration.get().getMachine().getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
                    if (nozzleTip == this) {
                        return nozzle;
                    }
                }
            }
        }
        return null;
    }

    public Calibration getCalibration() {
        return calibration;
    }

    public Action loadAction = new AbstractAction("Load") {
        {
            putValue(SMALL_ICON, Icons.load);
            putValue(NAME, "Load");
            putValue(SHORT_DESCRIPTION, "Load the currently selected nozzle tip.");
        }

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                getParentNozzle().loadNozzleTip(ReferenceNozzleTip.this);
            });
        }
    };

    public Action unloadAction = new AbstractAction("Unoad") {
        {
            putValue(SMALL_ICON, Icons.unload);
            putValue(NAME, "Unload");
            putValue(SHORT_DESCRIPTION, "Unoad the currently loaded nozzle tip.");
        }

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                getParentNozzle().unloadNozzleTip();
            });
        }
    };

    @Root
    public static class Calibration {
        public static class CalibrationOffset {
            final Location offset;
            final double angle;

            public CalibrationOffset(Location offset, double angle) {
                this.offset = offset;
                this.angle = angle;
            }

            @Override
            public String toString() {
                return angle + " " + offset;
            }
        }

        @Element(required = false)
        private CvPipeline calibrationPipeline = new CvPipeline();

        @Attribute(required = false)
        double angleIncrement = 30;

        List<CalibrationOffset> offsets;

        public void calibrate(ReferenceNozzleTip nozzleTip) throws Exception {
            this.offsets = null;

            Nozzle nozzle = nozzleTip.getParentNozzle();
            Camera camera = VisionUtils.getBottomVisionCamera();

            // Move to the camera with an angle of 0.
            Location location = camera.getLocation();
            location = location.derive(null, null, null, 0d);
            MovableUtils.moveToLocationAtSafeZ(nozzle, location);
            for (int i = 0; i < 3; i++) {
                // Locate the nozzle offsets.
                Location offset = findCircle();

                // Subtract the offsets and move to that position to center the nozzle.
                location = location.subtract(offset);
                nozzle.moveTo(location);
            }
            // This is our baseline location and should have the nozzle well centered over the
            // camera.
            Location startLocation = location;

            // Now we rotate the nozzle 360 degrees at calibration.angleIncrement steps, find the
            // nozzle using the camera and record the offsets.
            List<CalibrationOffset> offsets = new ArrayList<>();
            for (double i = 0; i < 360; i += angleIncrement) {
                location = startLocation.derive(null, null, null, i);
                nozzle.moveTo(location);
                Location offset = findCircle();
                offsets.add(new CalibrationOffset(offset, i));
            }

            // The nozzle tip is now calibrated and calibration.getCalibratedLocation() can be
            // used.
            this.offsets = offsets;

            // TESTS
            // for (double i = 0; i <= 360; i += 15) {
            // nozzle.moveTo(
            // startLocation.add(new Location(LengthUnit.Millimeters, 10, 10, 0, 0)));
            // nozzle.moveTo(startLocation.derive(null, null, null, i));
            // Thread.sleep(1000);
            //
            // nozzle.moveTo(
            // startLocation.add(new Location(LengthUnit.Millimeters, 10, 10, 0, 0)));
            // nozzle.moveTo(getCalibratedLocation(startLocation.derive(null, null, null, i)));
            // Thread.sleep(1000);
            // }
        }

        public Location findCircle() throws Exception {
            Camera camera = VisionUtils.getBottomVisionCamera();
            calibrationPipeline.setCamera(camera);
            calibrationPipeline.process();
            List<Result.Circle> circles =
                    (List<Result.Circle>) calibrationPipeline.getResult("result").model;
            List<Location> locations = circles.stream().map(circle -> {
                return VisionUtils.getPixelCenterOffsets(camera, circle.x, circle.y);
            }).sorted((a, b) -> {
                double a1 = a.getLinearDistanceTo(new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
                double b1 = b.getLinearDistanceTo(new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
                return Double.compare(a1, b1);
            }).collect(Collectors.toList());
            Location location = locations.get(0);
            MainFrame.mainFrame.cameraPanel.getCameraView(camera).showFilteredImage(
                    OpenCvUtils.toBufferedImage(calibrationPipeline.getWorkingImage()), 250);
            return location;
        }

        /**
         * Find the two closest offsets to the angle being requested. The offsets start at angle 0
         * and go to angle 360 - angleIncrement in angleIncrement steps.
         */
        public List<CalibrationOffset> getOffsetPairForAngle(double angle) {
            CalibrationOffset a = null, b = null;
            if (angle >= offsets.get(offsets.size() - 1).angle) {
                return Arrays.asList(offsets.get(offsets.size() - 1), offsets.get(0));
            }
            for (int i = 0; i < offsets.size(); i++) {
                if (angle < offsets.get(i + 1).angle) {
                    a = offsets.get(i);
                    b = offsets.get(i + 1);
                    break;
                }
            }
            return Arrays.asList(a, b);
        }

        public Location getCalibratedLocation(Location location) {
            double angle = location.getRotation();
            // Make sure the angle is between 0 and 360.
            while (angle < 0) {
                angle += 360;
            }
            while (angle > 360) {
                angle -= 360;
            }
            List<CalibrationOffset> offsets = getOffsetPairForAngle(angle);
            CalibrationOffset a = offsets.get(0);
            CalibrationOffset b = offsets.get(1);
            Location offsetA = a.offset.convertToUnits(location.getUnits());
            Location offsetB = b.offset.convertToUnits(location.getUnits());

            double ratio = (angle - a.angle) / (b.angle - a.angle);
            double deltaX = offsetB.getX() - offsetA.getX();
            double deltaY = offsetB.getY() - offsetA.getY();
            double offsetX = offsetA.getX() + (deltaX * ratio);
            double offsetY = offsetA.getY() + (deltaY * ratio);

            location = location.subtract(new Location(location.getUnits(), offsetX, offsetY, 0, 0));
            return location;
        }

        public boolean isCalibrated() {
            return offsets != null && !offsets.isEmpty();
        }

        public CvPipeline getCalibrationPipeline() throws Exception {
            calibrationPipeline.setCamera(VisionUtils.getBottomVisionCamera());
            return calibrationPipeline;
        }

        public void setCalibrationPipeline(CvPipeline calibrationPipeline) {
            this.calibrationPipeline = calibrationPipeline;
        }
    }
}
