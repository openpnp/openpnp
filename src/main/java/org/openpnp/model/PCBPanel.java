package org.openpnp.model;

import org.openpnp.model.Length;
import org.simpleframework.xml.Element;

public class PCBPanel extends AbstractModelObject {
	@Element
	private int columns = 1;
	@Element
	private int rows = 1;

	@Element
	private Length xGap;
	@Element
	private Length yGap;

	@Element
	private Location fid1;
	@Element
	private Location fid2;
	@Element
	private Length fidDiameter;

	public PCBPanel() {
		xGap = new Length(0, LengthUnit.Millimeters);
		yGap = new Length(0, LengthUnit.Millimeters);
		fid1 = new Location(LengthUnit.Millimeters);
		fid2 = new Location(LengthUnit.Millimeters);
		fidDiameter = new Length(2, LengthUnit.Millimeters);
	}

	public PCBPanel(int cols, int rows, Length xGap, Length yGap, Location fid1, Location fid2, Length fidDiameter) {
		this.columns = cols;
		this.rows = rows;
		this.xGap = xGap;
		this.yGap = yGap;
		this.fid1 = fid1;
		this.fid2 = fid2;
		this.fidDiameter = fidDiameter;
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

	public Location getFid1() {
		return fid1;
	}

	public void setFid1(Location fid) {
		this.fid1 = fid;
	}

	public Location getFid2() {
		return fid2;
	}

	public void setFid2(Location fid) {
		this.fid2 = fid;
	}

	public Length getFidSize() {
		return fidDiameter;
	}

	public void setFidSize(Length diameter) {
		this.fidDiameter = diameter;
	}

}
