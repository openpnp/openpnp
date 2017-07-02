/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.gui.support;

import org.openpnp.model.Configuration;

public class PartCellValue implements Comparable<PartCellValue> {
    private static Configuration configuration;

    private String partID;

    /**
     * When set, the toString() method will show the units contained within the Length instead of
     * converting to the system units.
     */

    public PartCellValue(String partID) {
        setPartID(partID);
    }

    public String getPartID() {
        return partID;
    }

    public void setPartID(String partID) {
        this.partID = partID;
    }

    @Override
    public String toString() {
        return getPartID();
    }

    int numericPos(String str)
    {
        int ret = -1;

        for (int i = 0; i < str.length(); i++)
        {
            if (Character.isDigit(str.charAt(i)))
            {
                ret = i;
                break;
            }
        }
        return ret;
    }

    int getNumeric(String str)
    {
        String numeric="";
        for (int i = 0; i < str.length(); i++)
        {
            if (Character.isDigit(str.charAt(i)))
            {
                numeric+=str.charAt(i);
            }
        }
        return Integer.parseInt(numeric);
    }

    public int compareRefName(String s1, String s2)
    {
        if (s1 == s2)
        {
            return 0;
        }

        if (numericPos(s1) != -1 && numericPos(s2) == -1)
        {
            return 1;
        }
        if (numericPos(s1) == -1 && numericPos(s2) != -1)
        {
            return -1;
        }

        if (numericPos(s1)!=-1 && numericPos(s2)!=-1)
        {
            String s1First = s1.substring(0,numericPos(s1));  // L123 this would contain L
            String s2First = s2.substring(0,numericPos(s2)); // L256 this would contain L

            if (s1First.compareTo(s2First)==0)
            {
                String s1Second = s1.substring(numericPos(s1), s1.length()); // L123 this would contain 123
                String s2Second = s2.substring(numericPos(s2), s2.length()); // L256 this would contain 256

                int num1;
                int num2;
                num1=getNumeric(s1Second);
                num2=getNumeric(s2Second);

                if (num1 < num2) {
                    return -1;
                }
                if (num1 > num2) {
                    return 1;
                }

                return 0;
            }
            else
            {
                return s1First.compareTo(s2First);
            }
        }

        return s1.compareTo(s2);
    }

    @Override
    public int compareTo(PartCellValue other)
    {
       return compareRefName(this.getPartID(),other.getPartID());
    }
}
