/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import org.simpleframework.xml.Attribute;


public class Rectangle {
	@Attribute
	private int left;
	@Attribute
	private int top;
	@Attribute
	private int right;
	@Attribute
	private int bottom;
	
	public Rectangle() {
		
	}
	
	public Rectangle(int left, int top, int right, int bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}
	
	public int getLeft() {
		return left;
	}
	
	public void setLeft(int left) {
		this.left = left;
	}
	
	public int getTop() {
		return top;
	}
	
	public void setTop(int top) {
		this.top = top;
	}
	
	public int getRight() {
		return right;
	}
	
	public void setRight(int right) {
		this.right = right;
	}
	
	public int getBottom() {
		return bottom;
	}
	
	public void setBottom(int bottom) {
		this.bottom = bottom;
	}
	
	public int getWidth() {
		return Math.abs(right - left);
	}
	
	public int getHeight() {
		return Math.abs(bottom - top);
	}
}
