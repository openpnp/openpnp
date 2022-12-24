/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.tablemodel;

/**
 * An interface for TableModels that define how corresponding columns of the Table should be saved
 * and restored
 */
public interface ColumnWidthSaveable {
    /**
     * Defines that the column should be saved and restored with a fixed width
     */
    public static final int FIXED = 0;
    
    /**
     * Defines that the column should be saved and restored with a proportional width
     */
    public static final int PROPORTIONAL = 1;
    
    /**
     * 
     * @return an array of column width types, each element should be one of the constants 
     * {@link #FIXED} or {@link #PROPORTIONAL}
     */
    public int[] getColumnWidthTypes();
}
