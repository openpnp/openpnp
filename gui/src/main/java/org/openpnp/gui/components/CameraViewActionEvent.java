package org.openpnp.gui.components;

public class CameraViewActionEvent {
	public int componentX, componentY;
	public double physicalX, physicalY;
	
	public CameraViewActionEvent(CameraView source, int componentX, int componentY, double physicalX, double physicalY) {
		this.componentX = componentX;
		this.componentY = componentY;
		this.physicalX = physicalX;
		this.physicalY = physicalY;
	}

	public int getComponentX() {
		return componentX;
	}

	public void setComponentX(int componentX) {
		this.componentX = componentX;
	}

	public int getComponentY() {
		return componentY;
	}

	public void setComponentY(int componentY) {
		this.componentY = componentY;
	}

	public double getPhysicalX() {
		return physicalX;
	}

	public void setPhysicalX(double physicalX) {
		this.physicalX = physicalX;
	}

	public double getPhysicalY() {
		return physicalY;
	}

	public void setPhysicalY(double physicalY) {
		this.physicalY = physicalY;
	}
}
