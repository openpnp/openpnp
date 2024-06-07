package org.openpnp.gui.importer.genericcsv.csv;

import com.Ostermiller.util.CSVParser;
import org.openpnp.model.*;
import org.openpnp.model.Package;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class GenericCSVParser {

    // the following methods are called to get the pattern used to decode the data
    // each array contains possible strings that might appear in the header line.
    // If any appears, the corresponding column is decoded as the respective type.
    // This class converts the data read from file to upper case before compare. So
    // provide upper case pattern only.
    public abstract String[] getReferencePattern();
    public abstract String[] getValuePattern();
    public abstract String[] getPackagePattern();
    public abstract String[] getXPattern();
    public abstract String[] getYPattern();
    public abstract String[] getRotationPattern();
    public abstract String[] getSidePattern();
    public abstract String[] getHeightPattern();
    public abstract String[] getCommentPattern();

    // this variables hold the pattern to decode the data
    private String referencePattern[];
    private String valuePattern[];
    private String packagePattern[];
    private String xPattern[];
    private String yPattern[];
    private String rotationPattern[];
    private String sidePattern[];
    private String heightPattern[];
    private String commentPattern[];


    // the following variable will contain the column indexes in which that data has
    // been detected using the string arrays define above.
    // This indexes are used to detect if all required properties have been found
    // and to extract the data by its function.
    private int referenceIndex;
    private int valueIndex;
    private int packageIndex;
    private int xIndex;
    private int yIndex;
    private int rotationIndex;
    private int sideIndex;
    private int heightIndex;
    private int commentIndex;

    // the length field contains the amount of columns required in data after the
    // header line has been detected and decoded
    private int len = 0;

    // automatic mil to mm conversion: if a property of the header ends in the specified
    // string, all values are converted from mil to mm. Again, the conversion is done with the
    // data read from file converted to upper case, so specify an upper case value here.
    static private final String MilToMM = "(MIL)";	//$NON-NLS-1$

    // this flags are used to remember if mil to mm conversion is required
    private boolean xUnitsMil;
    private boolean yUnitsMil;
    private boolean heightUnitsMil;

    // this character is used as separator when decoding the data
    // on a first pass, data is decoded using ','. On a second pass '\t' is used
    static private char separator = ',';

    // maximum number of lines at start of file to search for heading line describing the content
    private static final int maxHeaderLines = 50;

    // define that the data has to contain at least 6 columns. This is given by the fact
    // that reference, value, package, x, y and rotation are mandatory.
    private static final int minNumColumns = 6;

    //////////////////////////////////////////////////////////

    public void initialise() {
        // get strings to parse CSV context
        referencePattern = getReferencePattern();
        valuePattern     = getValuePattern();
        packagePattern   = getPackagePattern();
        xPattern         = getXPattern();
        yPattern         = getYPattern();
        rotationPattern  = getRotationPattern();
        sidePattern      = getSidePattern();
        heightPattern    = getHeightPattern();
        commentPattern   = getCommentPattern();
    }


    private static int checkCSV(String str[], String val[]) {
        for (int i = 0; i < str.length; i++) {
            for (int j = 0; j < val.length; j++) {
                if (str[i].equals(val[j])) {
                    Logger.trace("checkCSV: " + val[j] + " = " + j); //$NON-NLS-1$ //$NON-NLS-2$

                    return j;
                }
            }
        }
        // not found: return an invalid column index
        return -1;
    }

    private boolean checkCSV(String str[]) {

        // note that layer/side, height and comment are optional
        if (       (referenceIndex = checkCSV(referencePattern, str)) >= 0
                && (valueIndex     = checkCSV(valuePattern,     str)) >= 0
                && (packageIndex   = checkCSV(packagePattern,   str)) >= 0
                && (xIndex         = checkCSV(xPattern,         str)) >= 0
                && (yIndex         = checkCSV(yPattern,         str)) >= 0
                && (rotationIndex  = checkCSV(rotationPattern,  str)) >= 0) {

            // the following fields are optional
            heightIndex  = checkCSV(heightPattern , str); // optional height field
            sideIndex    = checkCSV(sidePattern,    str); // optional top/bottom layer field
            commentIndex = checkCSV(commentPattern, str); // optional comment field

            // test if any value requires mil to mm conversion
            xUnitsMil = str[xIndex].endsWith(MilToMM);
            if (xUnitsMil) {
                Logger.trace("X units are in mils"); //$NON-NLS-1$
            }
            yUnitsMil = str[yIndex].endsWith(MilToMM);
            if (yUnitsMil) {
                Logger.trace("Y units are in mils"); //$NON-NLS-1$
            }
            heightUnitsMil = heightIndex >= 0 && str[heightIndex].endsWith(MilToMM);
            if (heightUnitsMil) {
                Logger.trace("Height units are in mils"); //$NON-NLS-1$
            }

            // find the largest index, which defines the required line length
            len = 0;
            len = Math.max(len, referenceIndex);
            len = Math.max(len, valueIndex);
            len = Math.max(len, packageIndex);
            len = Math.max(len, xIndex);
            len = Math.max(len, yIndex);
            len = Math.max(len, rotationIndex);
            len = Math.max(len, sideIndex);
            len = Math.max(len, heightIndex);
            len = Math.max(len, commentIndex);
            Logger.trace("checkCSV: Len = " + len); //$NON-NLS-1$
            return true;
        }
        // output values found
        Logger.trace("checkCSV: referenceIndex = " + referenceIndex); //$NON-NLS-1$
        Logger.trace("checkCSV: valueIndex = "     + valueIndex);     //$NON-NLS-1$
        Logger.trace("checkCSV: packageIndex = "   + packageIndex);   //$NON-NLS-1$
        Logger.trace("checkCSV: xIndex = "         + xIndex);         //$NON-NLS-1$
        Logger.trace("checkCSV: yIndex = "         + yIndex);         //$NON-NLS-1$
        Logger.trace("checkCSV: rotationIndex = "  + rotationIndex);  //$NON-NLS-1$
        Logger.trace("checkCSV: sideIndex = "      + sideIndex);      //$NON-NLS-1$
        Logger.trace("checkCSV: heightIndex = "    + heightIndex);    //$NON-NLS-1$
        Logger.trace("checkCSV: commentIndex = "   + commentIndex);   //$NON-NLS-1$
        // force length to invalid for following stages
        len = 0;
        return false;
    }

    private boolean checkLine(String str) throws Exception {
        Logger.trace("checkLine: " + str); //$NON-NLS-1$
        String input_str = str.toUpperCase();
        if (input_str.charAt(0) == '#') {
            input_str = input_str.substring(1);
        }
        if (input_str == null) {
            return false;
        }
        // sting not empty, try to decode it as header line
        String as[];		// a line of the file as string
        String at[][];		// the line as split into fields
        CSVParser csvParser = new CSVParser(new StringReader(input_str));
        as = csvParser.getLine();

        // first pass: try with ',' separator
        separator = ',';
        if (as.length >= minNumColumns && checkCSV(as)) {
            return true;
        }

        // second pass: try with '\t' separator
        separator = '\t';
        at = CSVParser.parse(input_str, separator);
        if (at.length > 0 && at[0].length >= minNumColumns && checkCSV(at[0])) {
            return true;
        }
        /*
         * at=csvParser.parse(input_str,comma=' '); if(at.length>0&&at[0].length>=6&&checkCSV(at[0]))
         * return true;
         */
        return false;
    }

    /*
     * CSVParser csvParser = new CSVParser( new FileInputStream("datei.csv") ); for ( String as[];
     * (as = csvParser.getLine()) != null; ) if(as.length<=Len) continue; else System.out.println(
     * csvParser.lastLineNumber() + " " + t );
     */
    //////////////////////////////////////////////////////////

    // convert given string taken from a .csv files field into a double
    // unified method to handle all conversions identical
    private double convert(String s) {
        return Double.parseDouble(s
                .replace(",", ".")    //$NON-NLS-1$ //$NON-NLS-2$
                .replace(" ", ""));   //$NON-NLS-1$ //$NON-NLS-2$
    }
    // convert length string to double incl. units removal and mil to mm conversion
    private double convert(String s, boolean unitsInMil) {
        double v = Double.parseDouble(s
                .replace(",", ".")    //$NON-NLS-1$ //$NON-NLS-2$
                .replace(" ", "")     //$NON-NLS-1$ //$NON-NLS-2$
                .replace("mm", "")    //$NON-NLS-1$ //$NON-NLS-2$
                .replace("mil", "")); //$NON-NLS-1$ //$NON-NLS-2$
        // if units are in mil, convert them to the OpenPnP standard millimeter
        if (unitsInMil) {
            v = v * 0.0254;
        }
        return v;
    }

    // detect the character set required to read the file by searching for a byte order mark
    private String detectCharacterSet(File file) throws Exception {
        FileInputStream f = new FileInputStream(file);

        // read the first two characters and test if its a byte order mark
        // (https://en.wikipedia.org/wiki/Byte_order_mark)
        int b1 = f.read();
        int b2 = f.read();

        // if the file starts with 0xff 0xfe, its a byte order mark for UTF-16 encoding
        if (b1 == 0xff && b2 == 0xfe) {
            Logger.trace("Byte Order Mark detected, will read " + file + " using UTF-16 character set");
            return "UTF-16"; //$NON-NLS-1$
        }

        // return default character set
        Logger.trace("No Byte Order Mark detected, will read " + file + " using the default ISO-8859-1 character set");
        return "ISO-8859-1"; //$NON-NLS-1$
    }

    public List<Placement> parseFile(File file, boolean createMissingParts,
                                     boolean updateHeights) throws Exception {
        initialise();

        String characterset = detectCharacterSet(file);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file), characterset));
        ArrayList<Placement> placements = new ArrayList<>();
        String line;
        Configuration cfg = Configuration.get();

        // search for a maximum number of lines for headings describing the content
        for (int i = 0; i++ < maxHeaderLines && (line = reader.readLine()) != null;) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (checkLine(line)) {
                break;
            }
        }

        if (len <= 0) {
            reader.close();
            throw new Exception("Unable to find relevant headers' names.\n See https://github.com/openpnp/openpnp/wiki/Importing-Centroid-Data for more."); //$NON-NLS-1$
        }

        // CSVParser csvParser = new CSVParser(new FileInputStream(file));
        CSVParser csvParser = new CSVParser(reader, separator);
        for (String as[]; (as = csvParser.getLine()) != null;) {
            if (as.length <= len) {
                continue;
            }
            else {
                String placementId = as[referenceIndex];
                double placementX = convert(as[xIndex], xUnitsMil);
                double placementY = convert(as[yIndex], yUnitsMil);

                double heightZ = 0.0; // set default height to zero in case its not included in CSV
                if (heightIndex >= 0) {
                    heightZ = convert(as[heightIndex], heightUnitsMil);
                }

                double placementRotation = convert(as[rotationIndex]);
                // convert rotation to [-180 .. 180]
                placementRotation = Utils2D.angleNorm(placementRotation, 180);

                String partId = as[packageIndex] + "-" + as[valueIndex]; //$NON-NLS-1$
                Part part = cfg.getPart(partId);

                // if part does not exist, create it
                if (part == null && createMissingParts) {
                    part = new Part(partId);
                    Length l = new Length(heightZ, LengthUnit.Millimeters);
                    part.setHeight(l);
                    org.openpnp.model.Package pkg = cfg.getPackage(as[packageIndex]);
                    if (pkg == null) {
                        pkg = new Package(as[packageIndex]);
                        cfg.addPackage(pkg);
                    }
                    part.setPackage(pkg);

                    cfg.addPart(part);
                }

                // if we still don't have a part, skip this placement
                if (part == null) {
                    // no configuration -> skip placement
                    Logger.warn("no part for placement " + placementId + " (" + partId + ") found, skipped.");   //$NON-NLS-1$
                    continue;
                }

                // if part exists and height exist and user wants height updated do it.
                if (updateHeights && heightIndex >= 0) {
                    Length l = new Length(heightZ, LengthUnit.Millimeters);
                    part.setHeight(l);
                }

                // create new placement
                Placement placement = new Placement(placementId);

                // change placement type to Fiducial if the reference/id starts with "FID" or "REF" followed by a digit
                String id = placement.getId().toUpperCase();
                if (   (id.startsWith("FID") || id.startsWith("REF"))
                        && Character.isDigit(id.charAt(3))) {
                    placement.setType(Placement.Type.Fiducial);
                }

                // set placements location
                placement.setLocation(new Location(LengthUnit.Millimeters, placementX, placementY,
                        0, placementRotation));

                placement.setPart(part);

                // get optional comment
                if(commentIndex >= 0) {
                    placement.setComments(as[commentIndex]);
                }

                // get optional side
                char c = 0;
                if (sideIndex >= 0) {
                    c = as[sideIndex].toUpperCase().charAt(0);
                }
                placement.setSide(c == 'B' || c == 'Y' ? Abstract2DLocatable.Side.Bottom : Abstract2DLocatable.Side.Top);
                placements.add(placement);
            }
        }
        reader.close();
        return placements;
    }
}
