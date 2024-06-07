package org.openpnp.gui.importer.diptrace.csv;

import org.openpnp.gui.importer.genericcsv.csv.GenericCSVParser;

public class DipTrace4xCsvParser extends GenericCSVParser {

    // methods to read of strings for each purpose to be found in the heading line
    // data read from file is converted to upper case before compare -> only list upper case pattern here
    // this is the list that is used by the default Named CSV importer
    public String[] getReferencePattern() {
        return new String[]{
                "REFDES",        //$NON-NLS-1$
        };
    }

    public String[] getValuePattern() {
        return new String[]{
                "VALUE",        //$NON-NLS-1$
        };
    }

    public String[] getPackagePattern() {
        return new String[]{
                "NAME",    //$NON-NLS-1$
        };
    }

    public String[] getXPattern() {
        return new String[]{
                "CENTER X (MM)",        //$NON-NLS-1$
        };
    }

    public String[] getYPattern() {
        return new String[]{
                "CENTER Y (MM)",        //$NON-NLS-1$
        };
    }

    public String[] getRotationPattern() {
        return new String[]{
                "ROTATION",        //$NON-NLS-1$
        };
    }

    public String[] getSidePattern() {
        return new String[]{
                "SIDE",        //$NON-NLS-1$
        };
    }

    public String[] getHeightPattern() {
        return new String[]{
                "HEIGHT",        //$NON-NLS-1$
        };
    }

    public String[] getCommentPattern() {
        return new String[]{
                "COMMENT"    //$NON-NLS-1$
        };
    }
}
