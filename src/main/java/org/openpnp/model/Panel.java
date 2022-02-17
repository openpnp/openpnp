package org.openpnp.model;


import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.model.Board.Side;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

@Root(name = "openpnp-panel")
public class Panel extends FiducialLocatable implements PropertyChangeListener, Identifiable {
    @Element
    private String id;

    @Deprecated
    @Element(required = false)
    private Integer columns = 1;
    @Deprecated
    @Element(required = false)
    private Integer rows = 1;
    @Deprecated
    @Element(required = false)
    private Length xGap;
    @Deprecated
    @Element(required = false)
    private Length yGap;

    @Element(required=false)
    private String partId;
    
    @Element
    private boolean checkFids;

    /**
     * @deprecated Use FiducialLocatable.placements instead
     */
    @Deprecated
    @ElementList(required = false)
    protected IdentifiableList<Placement> fiducials;
    
    @ElementList(required = false)
    protected List<FiducialLocatableLocation> children = new ArrayList<>();
    
    private transient File file;
    private transient boolean dirty;

    @Commit
    private void commit() {
        //Converted deprecated elements
        if (fiducials != null) {
            placements.addAll(fiducials);
            fiducials = null;
        }
        columns = null;
        rows = null;
        xGap = null;
        yGap = null;
    }
    
    public Panel(File file) {
        setFile(file);
        addPropertyChangeListener(this);
    }

    @SuppressWarnings("unused")
    public Panel() {
//        fiducials = new IdentifiableList<>();
    }

    /**
     * Constructs a deep copy of the specified panel
     * @param panel - the Panel to copy
     */
    public Panel(Panel panel) {
        super(panel);
        this.id = panel.id;
        this.partId = panel.partId;
        this.checkFids = panel.checkFids;
    }
    
    public Panel(String id) {
        this();
        this.id = id;
    }

    /**
     * Constructs a pcb panel with no defined fiducials
     * @param id - the id of the panel
     * @param cols - the number of boards in the panel's X direction
     * @param rows - the number of boards in the panel's Y direction
     * @param xGap - the gap between boards in the panel's X direction
     * @param yGap - the gap between boards in the panel's Y direction
     */
    public Panel(String id, int cols, int rows, Length xGap, Length yGap, Location boardDimensions) {
        this(id);
        this.checkFids = false;
        this.partId = "";
        LengthUnit units = boardDimensions.getUnits();
        this.dimensions = new Location(units).deriveLengths(
                boardDimensions.getLengthX().add(xGap).multiply(cols).subtract(xGap),
                boardDimensions.getLengthY().add(yGap).multiply(rows).subtract(yGap),
                null, null);
    }

    public List<Placement> getFiducials() {
        return getPlacements();
    }
    
    public List<FiducialLocatableLocation> getChildren() {
        return children;
    }

    public void setChildren(List<FiducialLocatableLocation> children) {
        this.children = children;
    }

    public String getPartId() {
        return this.partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }
    
    public Part getFiducialPart() {
        if (getPartId() == null) {
            return null;
        }
        return Configuration.get().getPart(getPartId());
    }
    
    public void setFiducialPart(Part fiducialPart) {
        if (fiducialPart == null) {
            setPartId(null);
        }
        setPartId(fiducialPart.getId());
    }

    public boolean isCheckFiducials() {
        return this.checkFids;
    }

    public void setCheckFiducials(boolean checkFiducials) {
        this.checkFids = checkFiducials;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("Panel: id %s, fiducial Count: %d", id, fiducials.size());
    }

    public File getFile() {
        return file;
    }

    void setFile(File file) {
        Object oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, file);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() != Panel.this || !evt.getPropertyName().equals("dirty")) {
            setDirty(true);
        }
    }

    public void setLocation(Job job) {
//        LengthUnit mm = LengthUnit.Millimeters;
//        
//        BoardLocation rootPcb = job.getBoardLocations().get(0);
//        Logger.trace("rootPcb = " + rootPcb);
//
//        Location rootDims = rootPcb.getBoard().getDimensions().convertToUnits(mm);
//        
//        if (rootPcbOffset == null) {
//            Logger.trace("Updating an old panelization to new format");
//            originalRootPcbLocation = rootPcb;
//            rootPcbOffset = new Location(mm);
//            location = rootPcb.getLocation();
//            dimensions = new Location(mm);
//            dimensions = dimensions.deriveLengths(
//                    rootDims.getLengthX().add(xGap).multiply(getColumns()).subtract(xGap),
//                    rootDims.getLengthY().add(yGap).multiply(getRows()).subtract(yGap),
//                    null, null);
//        }
//        
//        job.removeAllBoards();
//        
//        AffineTransform transformRootPcbToMachine = rootPcb.getPlacementTransform();
//        if (transformRootPcbToMachine == null) {
//            transformRootPcbToMachine = Utils2D.getDefaultBoardPlacementLocationTransform(rootPcb);
//        }
//
//        double pcbStepX = rootDims.getLengthX().add(xGap).getValue();
//        double pcbStepY = rootDims.getLengthY().add(yGap).getValue();
//        
//        for (int j = 0; j < getRows(); j++) {
//            for (int i = 0; i < getColumns(); i++) {
//                // deep copy the existing rootPcb
//                BoardLocation newPcb = new BoardLocation(rootPcb);
//
//                AffineTransform transformNewPcbToMachine = new AffineTransform();
//                transformNewPcbToMachine.setToTranslation(pcbStepX*i, pcbStepY*j); //really transformNewPcbToRootPcb
//                transformNewPcbToMachine.preConcatenate(transformRootPcbToMachine); //now its transformNewPcbToMachine
//                newPcb.setPlacementTransform(transformNewPcbToMachine);
//                
//                // Return the compensated board location
//                Location origin = new Location(LengthUnit.Millimeters);
//                if (rootPcb.getSide() == Side.Bottom) {
//                    origin = origin.add(rootPcb.getBoard().getDimensions().derive(null, 0., 0., 0.));
//                }
//                Location newBoardLocation = Utils2D.calculateBoardPlacementLocation(newPcb, origin);
//                newBoardLocation = newBoardLocation.convertToUnits(newPcb.getLocation().getUnits());
//                newBoardLocation = newBoardLocation.derive(null, null, newPcb.getLocation().getZ(), null);
//
//                newPcb.setLocation(newBoardLocation);
//                newPcb.setPlacementTransform(transformNewPcbToMachine);
//                
//                job.addBoardLocation(newPcb);
//            }
//        }
    }
}
