package org.openpnp.model;


import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public class Panel extends AbstractModelObject implements Identifiable {
    @Element
    private String id;

    @Element
    private int columns = 1;
    @Element
    private int rows = 1;

    @Element
    private Length xGap;
    @Element
    private Length yGap;

    @Element(required=false)
    private String partId;
    
    @Element
    private boolean checkFids;

    @ElementList(required = false)
    protected IdentifiableList<Placement> fiducials = new IdentifiableList<>();

    @SuppressWarnings("unused")
    public Panel() {
        fiducials = new IdentifiableList<>();
    }

    public Panel(String id) {
        this();
        this.id = id;
    }

    // This constructor is used for creating a pcb Panel with two fiducials. In this first release,
    // we only contemplate UI
    // that supports two fids on a panel

    public Panel(String id, int cols, int rows, Length xGap, Length yGap, String partId,
            boolean checkFids, Placement fid0, Placement fid1) {
        this(id);
        this.columns = cols;
        this.rows = rows;
        this.xGap = xGap;
        this.yGap = yGap;
        this.partId = partId;
        this.checkFids = checkFids;
        fiducials = new IdentifiableList<>();
        fiducials.add(fid0);
        fiducials.add(fid1);
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int cols) {
        this.columns = cols;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public Length getXGap() {
        return xGap;
    }

    public void setXGap(Length length) {
        this.xGap = length;
    }

    public Length getYGap() {
        return yGap;
    }

    public void setYGap(Length length) {
        this.yGap = length;
    }

    public IdentifiableList<Placement> getFiducials() {
        return fiducials;
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


    public void setLocation(Job job) {
        BoardLocation rootPCB = job.getBoardLocations().get(0);

        job.removeAllBoards();
        job.addBoardLocation(rootPCB);

        double pcbWidthX = rootPCB.getBoard().getDimensions().getX();
        double pcbHeightY = rootPCB.getBoard().getDimensions().getY();

        for (int j = 0; j < getRows(); j++) {
            for (int i = 0; i < getColumns(); i++) {
                // We already have board 0,0 in the list as this is the root
                // PCB. No need to create it.
                if (i == 0 && j == 0) {
                    continue;
                }

                // deep copy the existing rootpcb
                BoardLocation newPCB = new BoardLocation(rootPCB);

                // OFfset the sub PCB
                newPCB.setLocation(newPCB.getLocation()
                        .add(new Location(Configuration.get().getSystemUnits(),
                                (pcbWidthX + getXGap().getValue()) * i,
                                (pcbHeightY + getYGap().getValue()) * j, 0, 0)));

                // Rotate the sub PCB
                newPCB.setLocation(newPCB.getLocation().rotateXyCenterPoint(rootPCB.getLocation(),
                        rootPCB.getLocation().getRotation()));

                job.addBoardLocation(newPCB);
            }
        }
    }
}
