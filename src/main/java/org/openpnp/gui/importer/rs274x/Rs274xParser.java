package org.openpnp.gui.importer.rs274x;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.model.BoardPad;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Pad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple RS-274X parser. Not intended to be a general parser, but implements only OpenPnP
 * specific functionality.
 */
public class Rs274xParser {
    private final static Logger logger = LoggerFactory.getLogger(Rs274xParser.class);

    enum LevelPolarity {
        Dark, Clear
    }

    enum InterpolationMode {
        Linear, Clockwise, CounterClockwise
    }

    private BufferedReader reader;

    // Context
    private LengthUnit unit;
    private Aperture currentAperture;
    private Point2D.Double currentPoint;
    private LevelPolarity levelPolarity;
    private InterpolationMode interpolationMode;
    private boolean multiQuadrantMode;
    private boolean regionMode;
    private int coordinateFormatIntegerLength;
    private int coordinateFormatDecimalLength;
    private boolean coordinateFormatTrailingZeroOmission;
    private boolean coordinateFormatIncremental;
    private Map<Integer, Aperture> apertures = new HashMap<>();
    /**
     * Maps Aperture indexes to a count to aid in generation of pad names.
     */
    private Map<Integer, Integer> apertureUseCounts = new HashMap<>();

    private boolean stopped;
    private int lineNumber;
    private ParseStatistics parseStatistics;
    private boolean regionStarted;

    private List<BoardPad> pads;

    public Rs274xParser() {
        reset();
    }

    /**
     * Parse the given File for solder paste pads.
     * 
     * @see #parseSolderPastePads(Reader)
     * @param file
     * @return
     * @throws Exception
     */
    public List<BoardPad> parseSolderPastePads(File file) throws Exception {
        logger.info("Parsing " + file);
        return parseSolderPastePads(new FileReader(file));
    }

    /**
     * Parse the input from the Reader extracting individual pads to be used for solder paste
     * application. It is expected that the input is is an RS-274X Gerber solder paste layer.
     * 
     * Currently this code only parses out single flashes of rectangular, circular and oblong
     * apertures. Ideas for future versions include rendering the entire file and uses blob
     * detection and contour finding to create polygon pads.
     * 
     * Another option is to consider each operation it's own element/shape. This is how gerbv seems
     * to do it.
     * 
     * @param reader
     * @return
     * @throws Exception
     */
    public List<BoardPad> parseSolderPastePads(Reader reader) throws Exception {
        reset();

        this.reader = new BufferedReader(reader);

        try {
            while (!stopped) {
                readCommand();
            }
        }
        catch (Exception e) {
            parseStatistics.errored = true;
            error("Uncaught error: " + e.getMessage());
        }

        return pads;
    }

    private void readCommand() throws Exception {
        if (peek() == '%') {
            readExtendedCodeCommand();
        }
        else {
            readFunctionCodeCommand();
        }
    }

    private void readFunctionCodeCommand() throws Exception {
        // a command is either a D, G, M or coordinate data
        // followed by a D.
        // X, Y
        // TODO: Make sure this becomes the current point.
        Point2D.Double coordinate = new Point2D.Double(currentPoint.getX(), currentPoint.getY());
        // I, J
        Point2D.Double arcCoordinate = new Point2D.Double(0, 0);
        while (!stopped) {
            int ch = read();
            switch (ch) {
                case '*': {
                    // Empty block or end of block that was not terminated
                    // from a previous read.
                    return;
                }
                case 'D': {
                    readDcode(coordinate, arcCoordinate);
                    return;
                }
                case 'G': {
                    readGcode();
                    return;
                }
                case 'M': {
                    readMcode();
                    return;
                }
                    // TODO: See 7.2 Coordinate Data without Operation Code
                case 'X': {
                    coordinate.x = readCoordinateValue();
                    break;
                }
                case 'Y': {
                    coordinate.y = readCoordinateValue();
                    break;
                }
                case 'I': {
                    arcCoordinate.x = readCoordinateValue();
                    break;
                }
                case 'J': {
                    arcCoordinate.y = readCoordinateValue();
                    break;
                }
                default: {
                    error("Unknown function code " + ((char) ch));
                }
            }
        }
    }

    // G54D06
    private void readGcode() throws Exception {
        int code = readInteger();
        switch (code) {
            case 1: {
                interpolationMode = InterpolationMode.Linear;
                break;
            }
            case 2: {
                interpolationMode = InterpolationMode.Clockwise;
                break;
            }
            case 3: {
                interpolationMode = InterpolationMode.CounterClockwise;
                break;
            }
            case 4: {
                // comment, ignore
                readUntil('*');
                break;
            }
            case 36: {
                enableRegionMode();
                break;
            }
            case 37: {
                disableRegionMode();
                break;
            }
            case 54: {
                // deprecated, prepare to select aperture
                break;
            }
            case 55: {
                // deprecated, prepare for flash
                break;
            }
            case 70: {
                // deprecated, unit inch
                unit = LengthUnit.Inches;
                break;
            }
            case 71: {
                // deprecated, unit mm
                unit = LengthUnit.Millimeters;
                break;
            }
            case 74: {
                multiQuadrantMode = false;
                break;
            }
            case 75: {
                multiQuadrantMode = true;
                break;
            }
            case 90: {
                // deprecated, absolute coordinate mode
                coordinateFormatIncremental = false;
                break;
            }
            case 91: {
                // deprecated, incremental coordinate mode
                coordinateFormatIncremental = true;
                break;
            }
            default: {
                warn("Unknown G code " + code);
            }
        }
    }

    private void readDcode(Point2D.Double coordinate, Point2D.Double arcCoordinate)
            throws Exception {
        int code = readInteger();
        switch (code) {
            case 1: {
                performD01(coordinate, arcCoordinate);
                break;
            }
            case 2: {
                performD02(coordinate);
                break;
            }
            case 3: {
                performD03(coordinate);
                break;
            }
            default: {
                if (code < 10) {
                    error("Unknown reserved D code " + code);
                }
                // anything else is an aperture code, so look up the aperture
                // and set it as the current
                currentAperture = apertures.get(code);
                if (currentAperture == null) {
                    warn("Unknown aperture " + code);
                }
            }
        }
    }

    private void readMcode() throws Exception {
        int code = readInteger();
        switch (code) {
            case 0: {
                // deprecated version of stop command
                stopped = true;
                break;
            }
            case 2: {
                stopped = true;
                break;
            }
            case 1: {
                // deprecated, does nothing
                break;
            }
            default: {
                warn("Unknown M code " + code);
            }
        }
    }

    /**
     * Linear or circular interpolation. If in region mode, add a line or arc to the current
     * contour. Otherwise draw a line or arc.
     * 
     * @param coordinate
     * @param arcCoordinate
     * @throws Exception
     */
    private void performD01(Point2D.Double coordinate, Point2D.Double arcCoordinate)
            throws Exception {
        if (interpolationMode == null) {
            error("Interpolation most must be set before using D02");
        }

        if (regionMode) {
            if (interpolationMode == InterpolationMode.Linear) {
                addRegionLine(coordinate);
            }
            else {
                addRegionArc(coordinate, arcCoordinate);
            }
        }
        else {
            if (interpolationMode == InterpolationMode.Linear) {
                parseStatistics.lineCount++;
                warn("Linear interpolation not yet supported");
            }
            else {
                parseStatistics.arcCount++;
                warn("Circular interpolation not yet supported");
            }
        }
        currentPoint = coordinate;
    }

    /**
     * Move / set the current coordinate. Additionally, in region mode end the current contour.
     * 
     * @param coordinate
     * @throws Exception
     */
    private void performD02(Point2D.Double coordinate) throws Exception {
        if (interpolationMode == null) {
            error("Interpolation mode must be set before using D02");
        }

        if (regionMode) {
            closeRegion();
        }

        currentPoint = coordinate;
    }

    /**
     * Flash the current aperture at the given coordinate.
     * 
     * @param coordinate
     * @throws Exception
     */
    private void performD03(Point2D.Double coordinate) throws Exception {
        if (currentAperture == null) {
            error("Can't flash, no current aperture");
        }
        if (regionMode) {
            error("Can't flash in region mode");
        }

        parseStatistics.flashCount++;

        Integer counter = apertureUseCounts.get(currentAperture.getIndex());
        if (counter == null) {
            counter = 0;
        }
        else {
            counter++;
        }
        apertureUseCounts.put(currentAperture.getIndex(), counter);

        BoardPad pad = currentAperture.createPad(unit, coordinate);
        pad.setName(String.format("D%02d-%03d", currentAperture.getIndex(), counter++));
        pads.add(pad);
        parseStatistics.padCount++;

        currentPoint = coordinate;

        parseStatistics.flashPerformedCount++;
    }

    private void enableRegionMode() throws Exception {
        if (regionMode) {
            error("Can't start region mode when already in region mode");
        }
        regionMode = true;
        regionStarted = false;
    }

    private void addRegionLine(Point2D.Double coordinate) throws Exception {
        if (!regionMode) {
            error("Can't add region line outside of region mode");
        }
        if (!regionStarted) {
            regionStarted = true;
        }
        parseStatistics.regionLineCount++;
        warn("Linear interpolation in region mode not yet supported");
    }

    private void addRegionArc(Point2D.Double coordinate, Point2D.Double arcCoordinate)
            throws Exception {
        if (!regionMode) {
            error("Can't add region arc outside of region mode");
        }
        if (!regionStarted) {
            regionStarted = true;
        }
        parseStatistics.regionArcCount++;
        warn("Circular interpolation in region mode not yet supported");
    }

    private void closeRegion() throws Exception {
        if (!regionMode) {
            error("Can't end region when not in region mode");
        }
        if (regionStarted) {
            regionStarted = false;
            parseStatistics.regionCount++;
        }
    }

    private void disableRegionMode() throws Exception {
        if (!regionMode) {
            error("Can't exit region mode, not in region mode");
        }
        closeRegion();
        regionMode = false;
    }

    private void readExtendedCodeCommand() throws Exception {
        if (read() != '%') {
            error("Expected start of extended code command");
        }
        String code = "" + ((char) read()) + ((char) read());
        switch (code) {
            case "FS": {
                // Sets the ‘Coordinate format’ graphics state parameter. See 4.9.
                // These commands are mandatory and must be used only once, in the header of a file.
                // Example: FSLAX24Y24
                readCoordinateFormat();
                break;
            }
            case "MO": {
                // Sets the ‘Unit’ graphics state parameter. See 4.10.
                readUnit();
                break;
            }
            case "AD": {
                // Assigns a D code number to an aperture definition. See 4.11.
                // These commands can be used multiple times. It is recommended to put them in
                // header of a file.
                readApertureDefinition();
                break;
            }
            case "AM": {
                // Defines macro apertures which can be referenced from the AD command. See 4.12.
                // TODO: We just ignore them for now.
                while (peek() != '%') {
                    readUntil('*');
                    read();
                }
                break;
            }
            case "SR": {
                // Sets the ‘Step and Repeat’ graphics state parameter. See 4.13.
                // These commands can be used multiple times over the whole file.
                readUntil('*');
                read();
                break;
            }
            case "LP": {
                // Starts a new level and sets the ‘Level polarity’ graphics state parameter. See
                // 4.14.
                readUntil('*');
                read();
                break;
            }
            case "AS": {
                // Deprecated axis select, ignore
                readUntil('*');
                read();
                break;
            }
            case "IN": {
                // Deprecated image name, ignore
                readUntil('*');
                read();
                break;
            }
            case "IP": {
                // Deprecated image polarity, ignore
                readUntil('*');
                read();
                break;
            }
            case "IR": {
                // Deprecated image rotation, ignore
                readUntil('*');
                read();
                break;
            }
            case "LN": {
                // Deprecated level name, ignore
                readUntil('*');
                read();
                break;
            }
            case "MI": {
                // Deprecated mirror image, ignore
                readUntil('*');
                read();
                break;
            }
            case "OF": {
                // Deprecated offset, ignore
                readUntil('*');
                read();
                break;
            }
            case "SF": {
                // Deprecated scale factor, ignore
                readUntil('*');
                read();
                break;
            }
            default: {
                warn("Unknown extended command code " + code);
                while (peek() != '%') {
                    read();
                }
            }
        }
        if (read() != '%') {
            error("Expected end of extended code command");
        }
    }

    private void readApertureDefinition() throws Exception {
        int ch;
        if (read() != 'D') {
            error("Expected aperture D code");
        }

        int code = readInteger();
        int type = read();
        Aperture aperture = null;
        switch (type) {
            case 'R': {
                aperture = readRectangleApertureDefinition(code);
                break;
            }
            case 'C': {
                aperture = readCircleApertureDefinition(code);
                break;
            }
            case 'O': {
                aperture = readObroundApertureDefinition(code);
                break;
            }
            case 'P': {
                aperture = readPolygonApertureDefinition(code);
                break;
            }
            default: {
                error(String.format("Unhandled aperture definition type %c, code %d", ((char) type),
                        code));
            }
        }
        apertures.put(code, aperture);
    }

    private Aperture readRectangleApertureDefinition(int index) throws Exception {
        if (read() != ',') {
            error("Expected , in rectangle aperture definition");
        }
        double width = readDecimal();
        if (read() != 'X') {
            error("Expected X in rectangle aperture definition");
        }
        double height = readDecimal();
        Double holeDiameter = null;
        if (peek() == 'X') {
            read();
            holeDiameter = readDecimal();
        }
        if (read() != '*') {
            error("Expected end of data block");
        }
        return new RectangleAperture(index, width, height, holeDiameter);
    }

    private Aperture readCircleApertureDefinition(int index) throws Exception {
        if (read() != ',') {
            error("Expected , in circle aperture definition");
        }
        double diameter = readDecimal();
        Double holeDiameter = null;
        if (peek() == 'X') {
            read();
            holeDiameter = readDecimal();
        }
        if (read() != '*') {
            error("Expected end of data block");
        }
        return new CircleAperture(index, diameter, holeDiameter);
    }

    private Aperture readObroundApertureDefinition(int index) throws Exception {
        if (read() != ',') {
            error("Expected , in obround aperture definition");
        }
        double width = readDecimal();
        if (read() != 'X') {
            error("Expected X in obround aperture definition");
        }
        double height = readDecimal();
        Double holeDiameter = null;
        if (peek() == 'X') {
            read();
            holeDiameter = readDecimal();
        }
        if (read() != '*') {
            error("Expected end of data block");
        }
        return new ObroundAperture(index, width, height, holeDiameter);
    }

    private Aperture readPolygonApertureDefinition(int index) throws Exception {
        if (read() != ',') {
            error("Expected , in circle aperture definition");
        }

        double diameter = readDecimal();

        if (read() != 'X') {
            error("Expected X in polygon aperture definition");
        }
        int numberOfVertices = readInteger();

        Double rotation = null;
        if (peek() == 'X') {
            read();
            rotation = readDecimal();
        }

        Double holeDiameter = null;
        if (peek() == 'X') {
            read();
            holeDiameter = readDecimal();
        }
        if (read() != '*') {
            error("Expected end of data block");
        }
        return new PolygonAperture(index, diameter, numberOfVertices, rotation, holeDiameter);
    }

    private void readUnit() throws Exception {
        String unitCode = readString(2);
        if (unitCode.equals("MM")) {
            unit = LengthUnit.Millimeters;
        }
        else if (unitCode.equals("IN")) {
            unit = LengthUnit.Inches;
        }
        else {
            error("Unknown unit code " + unitCode);
        }

        if (read() != '*') {
            error("Expected end of data block");
        }
    }

    private void readCoordinateFormat() throws Exception {
        int ch;

        while ("LTAI".indexOf((char) peek()) != -1) {
            ch = read();
            switch (ch) {
                case 'L': {
                    coordinateFormatTrailingZeroOmission = false;
                    break;
                }
                case 'T': {
                    coordinateFormatTrailingZeroOmission = true;
                    break;
                }
                case 'A': {
                    coordinateFormatIncremental = false;
                    break;
                }
                case 'I': {
                    coordinateFormatIncremental = true;
                    break;
                }
            }
        }

        int xI, xD, yI, yD;

        ch = read();
        if (ch != 'X') {
            error("Expected X coordinate format");
        }
        xI = read() - '0';
        if (xI < 0 || xI > 6) {
            warn("Invalid coordinate format, X integer part {}, should be >= 0 && <= 6", xI);
        }
        xD = read() - '0';
        if (xD < 4 || xD > 6) {
            warn("Invalid coordinate format, X decimal part {}, should be >= 4 && <= 6", xD);
        }

        ch = read();
        if (ch != 'Y') {
            error("Expected Y coordinate format");
        }
        yI = read() - '0';
        if (yI < 0 || yI > 6) {
            warn("Invalid coordinate format, Y integer part {}, should be >= 0 && <= 6", yI);
        }
        yD = read() - '0';
        if (yD < 4 || yD > 6) {
            warn("Invalid coordinate format, Y decimal part {}, should be >= 4 && <= 6", yD);
        }

        if (xI != yI || xD != yD) {
            error(String.format("Coordinate format X does not match Y: %d.%d != %d.%d", xI, xD, yI,
                    yD));
        }

        coordinateFormatIntegerLength = xI;
        coordinateFormatDecimalLength = xD;

        if (read() != '*') {
            error("Expected end of data block");
        }
    }

    private String readUntil(int ch) throws Exception {
        StringBuffer sb = new StringBuffer();
        while (peek() != ch) {
            sb.append((char) read());
        }
        return sb.toString();
    }

    private String readString(int length) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append((char) read());
        }
        return sb.toString();
    }

    private double readDecimal() throws Exception {
        boolean negative = false;
        int ch = peek();
        if (ch == '-') {
            negative = true;
            read();
        }
        else if (ch == '+') {
            read();
        }
        StringBuffer sb = new StringBuffer();
        String allowed = "0123456789.";
        while (allowed.indexOf(peek()) != -1) {
            sb.append((char) read());
        }
        return (negative ? -1 : 1) * Double.parseDouble(sb.toString());
    }

    private int readInteger() throws Exception {
        boolean negative = false;
        int ch = peek();
        if (ch == '-') {
            negative = true;
            read();
        }
        else if (ch == '+') {
            read();
        }
        StringBuffer sb = new StringBuffer();
        String allowed = "0123456789";
        while (allowed.indexOf(peek()) != -1) {
            sb.append((char) read());
        }
        return (negative ? -1 : 1) * Integer.parseInt(sb.toString());
    }

    private double readCoordinateValue() throws Exception {
        if (coordinateFormatIncremental) {
            error("Incremental coordinate format not supported");
        }
        if (coordinateFormatTrailingZeroOmission) {
            error("Trailing zero omission not supported");
        }
        if (coordinateFormatIntegerLength == -1 || coordinateFormatDecimalLength == -1) {
            error("Coordinate format not specified.");
        }
        // Read the value as an integer first, since this will read until it hits
        // something that isn't an integer character, then pad it out and then
        // break up the components.
        int value = readInteger();
        String sValue = Integer.toString(Math.abs(value));
        while (sValue.length() < coordinateFormatIntegerLength + coordinateFormatDecimalLength) {
            sValue = "0" + sValue;
        }
        String integerPart = sValue.substring(0, coordinateFormatIntegerLength);
        String decimalPart = sValue.substring(coordinateFormatIntegerLength,
                coordinateFormatIntegerLength + coordinateFormatDecimalLength - 1);
        return (value < 0 ? -1 : 1) * Double.parseDouble(integerPart + "." + decimalPart);
    }

    /**
     * Read the next character in the stream, skipping any \r or \n that precede it.
     * 
     * @return
     * @throws Exception
     */
    private int read() throws Exception {
        skipCrLf();
        int ch = reader.read();
        if (ch == -1) {
            error("Unexpected end of stream");
        }
        return ch;
    }

    /**
     * Peek at the next character in the stream, skipping any \r or \n that precede it.
     * 
     * @return
     * @throws Exception
     */
    private int peek() throws Exception {
        skipCrLf();
        return _peek();
    }

    /**
     * Consume any number of \r or \n, stopping when another character is found.
     * 
     * @throws Exception
     */
    private void skipCrLf() throws Exception {
        while (true) {
            int ch = _peek();
            if (ch == '\n') {
                lineNumber++;
                reader.read();
            }
            else if (ch == '\r') {
                reader.read();
            }
            else {
                return;
            }
        }
    }

    /**
     * Return the next character in the reader without consuming it.
     * 
     * @return
     * @throws Exception
     */
    private int _peek() throws Exception {
        reader.mark(1);
        int ch = reader.read();
        if (ch == -1) {
            error("Unexpected end of stream");
        }
        reader.reset();
        return ch;
    }

    private void reset() {
        unit = null;
        currentAperture = null;
        currentPoint = new Point2D.Double(0, 0);
        levelPolarity = LevelPolarity.Dark;
        /*
         * This is non-standard, but expected by Eagle, at least. The standard says that
         * interpolation mode is undefined at the start of the file but Eagle does not appear to
         * send a G01 at any point before it starts sending D01s.
         */
        interpolationMode = InterpolationMode.Linear;
        stopped = false;
        regionMode = false;
        coordinateFormatIntegerLength = -1;
        coordinateFormatDecimalLength = -1;
        coordinateFormatTrailingZeroOmission = false;
        coordinateFormatIncremental = false;
        apertures = new HashMap<>();
        lineNumber = 1;
        pads = new ArrayList<>();
        regionStarted = false;
        apertureUseCounts = new HashMap<>();

        parseStatistics = new ParseStatistics();
    }

    private void warn(String s) {
        logger.warn("WARNING: " + lineNumber + ": " + s);
    }

    private void warn(String fmt, Object o1) {
        logger.warn("WARNING: " + lineNumber + ": " + fmt, o1);
    }

    private void warn(String fmt, Object o1, Object o2) {
        logger.warn("WARNING: " + lineNumber + ": " + fmt, o1, o2);
    }

    private void warn(String fmt, Object[] o) {
        logger.warn("WARNING: " + lineNumber + ": " + fmt, o);
    }

    private void error(String s) throws Exception {
        throw new Exception("ERROR: " + lineNumber + ": " + s);
    }

    public static void main(String[] args) throws Exception {
        HashMap<File, ParseStatistics> results = new HashMap<>();
        File[] files = new File("/Users/jason/Desktop/paste_tests").listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            if (file.getName().equals(".DS_Store")) {
                continue;
            }
            Rs274xParser parser = new Rs274xParser();
            try {
                parser.parseSolderPastePads(file);
            }
            catch (Exception e) {
                System.out.println(file.getName() + " " + e.getMessage());
            }
            results.put(file, parser.parseStatistics);
        }

        ParseStatistics total = new ParseStatistics();
        logger.info("");
        logger.info("");
        ArrayList<File> sortedFiles = new ArrayList<>(results.keySet());
        Collections.sort(sortedFiles, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });
        for (File file : sortedFiles) {
            ParseStatistics stats = results.get(file);
            total.add(stats);;
            logger.info(String.format("%-32s: %s", file.getName(), stats.toString()));
        }
        String totalLine = String.format("%-32s: %s", "TOTALS", total.toString());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < totalLine.length(); i++) {
            sb.append("-");
        }
        logger.info(sb.toString());
        logger.info(totalLine);
    }

    static abstract class Aperture {
        final protected int index;

        public Aperture(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public abstract BoardPad createPad(LengthUnit unit, Point2D.Double coordinate);
    }

    static abstract class StandardAperture extends Aperture {
        public StandardAperture(int index) {
            super(index);
        }
    }

    static class RectangleAperture extends StandardAperture {
        public double width;
        public double height;
        public Double holeDiameter;

        public RectangleAperture(int index, double width, double height, Double holeDiameter) {
            super(index);
            this.width = width;
            this.height = height;
            this.holeDiameter = holeDiameter;
        }

        public BoardPad createPad(LengthUnit unit, Point2D.Double coordinate) {
            Pad.RoundRectangle pad = new Pad.RoundRectangle();
            pad.setUnits(unit);
            pad.setWidth(width);
            pad.setHeight(height);
            pad.setRoundness(0);
            BoardPad boardPad =
                    new BoardPad(pad, new Location(unit, coordinate.x, coordinate.y, 0, 0));
            return boardPad;
        }

        @Override
        public String toString() {
            return "RectangleAperture [width=" + width + ", height=" + height + ", holeDiameter="
                    + holeDiameter + "]";
        }
    }

    static class CircleAperture extends StandardAperture {
        public double diameter;
        public Double holeDiameter;

        public CircleAperture(int index, double diameter, Double holeDiameter) {
            super(index);
            this.diameter = diameter;
            this.holeDiameter = holeDiameter;
        }

        public BoardPad createPad(LengthUnit unit, Point2D.Double coordinate) {
            Pad.Circle pad = new Pad.Circle();
            pad.setRadius(diameter / 2);
            pad.setUnits(unit);
            BoardPad boardPad =
                    new BoardPad(pad, new Location(unit, coordinate.x, coordinate.y, 0, 0));
            return boardPad;
        }

        @Override
        public String toString() {
            return "CircleAperture [diameter=" + diameter + ", holeDiameter=" + holeDiameter + "]";
        }
    }

    static class ObroundAperture extends RectangleAperture {
        public ObroundAperture(int index, double width, double height, Double holeDiameter) {
            super(index, width, height, holeDiameter);
        }

        @Override
        public String toString() {
            return "ObroundAperture [width=" + width + ", height=" + height + ", holeDiameter="
                    + holeDiameter + "]";
        }
    }

    static class PolygonAperture extends CircleAperture {
        public int numberOfVertices;
        public Double rotation;

        public PolygonAperture(int index, double diameter, int numberOfVertices, Double rotation,
                Double holeDiameter) {
            super(index, diameter, holeDiameter);
            this.numberOfVertices = numberOfVertices;
            this.rotation = rotation;
        }

        @Override
        public String toString() {
            return "PolygonAperture [numberOfVertices=" + numberOfVertices + ", rotation="
                    + rotation + ", diameter=" + diameter + ", holeDiameter=" + holeDiameter + "]";
        }
    }

    static class MacroAperture extends Aperture {
        public MacroAperture(int index) {
            super(index);
        }

        @Override
        public BoardPad createPad(LengthUnit unit, java.awt.geom.Point2D.Double coordinate) {
            return null;
        }
    }

    static class ParseStatistics {
        public int lineCount;
        public int linePerformedCount;

        public int arcCount;
        public int arcPerformedCount;

        public int regionLineCount;
        public int regionLinePerformedCount;

        public int regionArcCount;
        public int regionArcPerformedCount;

        public int regionCount;
        public int regionPerformedCount;

        public int flashCount;
        public int flashPerformedCount;

        public int padCount;

        public boolean errored;

        public double percent(double count, double total) {
            if (total == 0) {
                return 100;
            }
            return (count / total) * 100;
        }

        public void add(ParseStatistics p) {
            lineCount += p.lineCount;
            linePerformedCount += p.linePerformedCount;

            arcCount += p.arcCount;
            arcPerformedCount += p.arcPerformedCount;

            regionLineCount += p.regionLineCount;
            regionLinePerformedCount += p.regionLinePerformedCount;

            regionArcCount += p.regionArcCount;
            regionArcPerformedCount += p.regionArcPerformedCount;

            regionCount += p.regionCount;
            regionPerformedCount += p.regionPerformedCount;

            flashCount += p.flashCount;
            flashPerformedCount += p.flashPerformedCount;

            padCount += p.padCount;
        }

        @Override
        public String toString() {
            int total = flashCount + regionCount + lineCount + arcCount;
            int totalPerformed = flashPerformedCount + regionPerformedCount + linePerformedCount
                    + arcPerformedCount;
            return String.format(
                    "%s Total %3.0f%% (%4d/%4d), Flash %3.0f%% (%4d/%4d), Line %3.0f%% (%4d/%4d), Arc %3.0f%% (%4d/%4d), Region %3.0f%% (%4d/%4d), Region line %3.0f%% (%4d/%4d), Region Arc %3.0f%% (%4d/%4d), Pads %4d",
                    errored ? "FAIL" : "PASS", percent(totalPerformed, total), totalPerformed,
                    total, percent(flashPerformedCount, flashCount), flashPerformedCount,
                    flashCount, percent(linePerformedCount, lineCount), linePerformedCount,
                    lineCount, percent(arcPerformedCount, arcCount), arcPerformedCount, arcCount,
                    percent(regionPerformedCount, regionCount), regionPerformedCount, regionCount,
                    percent(regionLinePerformedCount, regionLineCount), regionLinePerformedCount,
                    regionLineCount, percent(regionArcPerformedCount, regionArcCount),
                    regionArcPerformedCount, regionArcCount, padCount);
        }
    }
}
