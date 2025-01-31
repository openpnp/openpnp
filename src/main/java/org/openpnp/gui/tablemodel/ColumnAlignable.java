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

import javax.swing.SwingConstants;

/**
 * An interface for TableModels that defines how the corresponding columns of the Table should be 
 * aligned
 */
public interface ColumnAlignable {
    /**
     * Constant used to indicate the contents of the column should be aligned on the left edge
     */
    public static final int LEFT = SwingConstants.LEFT;
    
    /**
     * Constant used to indicate the contents of the column should be aligned on the center
     */
    public static final int CENTER = SwingConstants.CENTER;
    
    /**
     * Constant used to indicate the contents of the column should be aligned on the right edge
     */
    public static final int RIGHT = SwingConstants.RIGHT;
    
    /**
     * 
     * @return an array of column alignments, each element should be one of the constants
     * {@link #LEFT}, {@link #CENTER}, or {@link #RIGHT}
     */
    public int[] getColumnAlignments();
}
