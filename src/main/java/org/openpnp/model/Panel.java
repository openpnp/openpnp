package org.openpnp.model;


import org.openpnp.gui.MainFrame;
import org.openpnp.model.Length;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
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
	
	@Element
	private String partId;  
	
	@Element
	private boolean checkFids;
	
    @ElementList(required = false)
    protected IdentifiableList<Placement> fiducials;
    
    @SuppressWarnings("unused")
    public Panel(){
    	fiducials = new IdentifiableList<>();
    }

	public Panel(String id) {
		this();
		this.id = id;
	}

	// This constructor is used for creating a pcb Panel with two fiducials. In this first release, we only contemplate UI
	// that supports two fids on a panel
	
	public Panel(String id, int cols, int rows, Length xGap, Length yGap, String partId, boolean checkFids, Placement fid0, Placement fid1) {
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
}
