package org.openpnp.gui.importer.rs274x;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Pad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple RS-274X parser. Not intended to be a general parser, but implements
 * only OpenPnP specific functionality.
 */
public class Rs274xParser {
    private final static Logger logger = LoggerFactory.getLogger(Rs274xParser.class);
    
    enum LevelPolarity {
        Dark,
        Clear
    }
    
    enum InterpolationMode {
        Linear,
        Clockwise,
        CounterClockwise
    }
    
    private BufferedReader reader;
    
    // Context
    private LengthUnit unit;
    private Aperture currentAperture;
    private Point2D.Double currentPoint;
    private Point2D.Double endPoint;
    private LevelPolarity levelPolarity;
    private InterpolationMode interpolationMode;
    private boolean multiQuadrantMode;
    private Map<String, Aperture> apertures = new HashMap<>();
    private boolean regionMode;

    private boolean stopped;
    private int lineNumber;
    
    private List<Pad> pads;

    public Rs274xParser() {
        reset();
    }
    
    /**
     * Parse the given File for solder paste pads.
     * @see #parseSolderPastePads(Reader) 
     * @param file
     * @return
     * @throws Exception
     */
    public List<Pad> parseSolderPastePads(File file) throws Exception {
        logger.info("parsing " + file);
        return parseSolderPastePads(new FileReader(file));
    }
    
    /**
     * Parse the input from the Reader extracting individual pads to be used
     * for solder paste application. It is expected that the input is is
     * an RS-274X Gerber solder paste layer.
     * 
     * Currently this code only parses out single flashes of rectangular,
     * circular and oblong apertures. Ideas for future versions include
     * rendering the entire file and uses blob detection and contour
     * finding to create polygon pads.
     * 
     * @param reader
     * @return
     * @throws Exception
     */
    public List<Pad> parseSolderPastePads(Reader reader) throws Exception {
        reset();
        
        this.reader = new BufferedReader(reader);
        
        while (!stopped) {
            readCommand();
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
        endPoint = new Point2D.Double(currentPoint.getX(), currentPoint.getY());
        while (!stopped) {
            int ch = read();
            switch (ch) {
                case '*': {
                    return;
                }
                case 'D': {
                    readDcode();
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
                case 'X': 
                case 'Y':
                case 'I':
                case 'J': {
                    readUntil('*');
                    return;
                }
                default : {
                    error("Unknown function code " + ((char) ch));
                }
            }
        }
    }
    
    private void readGcode() throws Exception {
        int code = readInteger(false);
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
                break;
            }
            case 36: {
                regionMode = true;
                break;
            }
            case 37: {
                regionMode = false;
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
            default: {
                logger.warn("Unknown G code " + code);
            }
        }
        readUntil('*');
    }
    
    private void readDcode() throws Exception {
        int code = readInteger(false);
        switch (code) {
            default: {
                logger.warn("Unknown D code " + code);
            }
        }
        readUntil('*');
    }
    
    private void readMcode() throws Exception {
        int code = readInteger(false);
        switch (code) {
            case 2: {
                stopped = true;
                break;
            }
            default: {
                logger.warn("Unknown M code " + code);
            }
        }
        readUntil('*');
    }
    
    private void readExtendedCodeCommand() throws Exception {
        if (read() != '%') {
            error("Expected start of extended code command");
        }
        while (true) {
            readUntil('*');
            if (peek() == '%') {
                read();
                break;
            }
        }
    }
    
    private void readUntil(int ch) throws Exception {
        while (read() != ch);
    }
    
    private int readInteger(boolean allowNegative) throws Exception {
        boolean negative = false;
        if (allowNegative) {
            if (peek() == '-') {
                negative = true;
                read();
            }
        }
        StringBuffer sb = new StringBuffer();
        String allowed = "0123456789";
        int ch;
        while (allowed.indexOf(peek()) != -1) {
            sb.append((char) read());
        }
        return negative ? -1 : 1 * Integer.parseInt(sb.toString());
    }
    
    /**
     * Read the next character in the stream, skipping any \r or \n that
     * precede it.
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
    
    private void error(String s) throws Exception {
        throw new Exception(lineNumber + ": " + s);
    }
    
    /**
     * Peek at the next character in the stream, skipping any \r or \n that
     * precede it.
     * @return
     * @throws Exception
     */
    private int peek() throws Exception {
        skipCrLf();
        return _peek();
    }
    
    /**
     * Consume any number of \r or \n, stopping when another character is found. 
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
        interpolationMode = null;
        stopped = false;
        regionMode = false;
        apertures = new HashMap<>();        
        lineNumber = 1;
        pads = new ArrayList<>();
    }
    
    public static void main(String[] args) throws Exception {
        File[] files = new File("/Users/jason/Desktop/paste_tests").listFiles();
        for (File file : files) {
            try {
                new Rs274xParser().parseSolderPastePads(file);
            }
            catch (Exception e) {
                System.out.println("Error in " + file.getName() + " " + e.getMessage());
            }
        }
//        new Rs274xParser().parseSolderPastePads(new File("/Users/jason/Desktop/paste_tests/BTPD-v6.GBC"));
    }
    
    static class Aperture {
        
    }
    
    static class StandardAperture extends Aperture {
        
    }
    
    static class MacroAperture extends Aperture {
        
    }
}

//G75*
//%MOIN*%
//%OFA0B0*%
//%FSLAX25Y25*%
//%IPPOS*%
//%LPD*%
//%AMOC8*
//5,1,8,0,0,1.08239X$1,22.5*
//%
//%ADD10R,0.08000X0.02600*%
//%ADD11R,0.05118X0.05906*%
//%ADD12R,0.03000X0.06000*%
//%ADD13R,0.05000X0.02200*%
//%ADD14R,0.02200X0.05000*%
//%ADD15R,0.21260X0.24409*%
//%ADD16R,0.03937X0.06299*%
//%ADD17R,0.05512X0.03937*%
//%ADD18R,0.04331X0.02953*%
//%ADD19R,0.02953X0.04331*%
//%ADD20R,0.08661X0.02362*%
//%ADD21R,0.02600X0.08000*%
//D10*
//X0062861Y0143661D03*
//X0062861Y0148661D03*
//X0062861Y0153661D03*
//X0062861Y0158661D03*
//X0062861Y0163661D03*
//X0062861Y0168661D03*
//X0062861Y0173661D03*
//X0062861Y0178661D03*
//X0062861Y0183661D03*
//X0101461Y0183661D03*
//X0101461Y0178661D03*
//X0101461Y0173661D03*
//X0101461Y0168661D03*
//X0101461Y0163661D03*
//X0101461Y0158661D03*
//X0101461Y0153661D03*
//X0101461Y0148661D03*
//X0101461Y0143661D03*
//D11*
//X0023314Y0059161D03*
//X0030007Y0059161D03*
//X0040814Y0059161D03*
//X0047507Y0059161D03*
//X0047507Y0069161D03*
//X0040814Y0069161D03*
//X0030007Y0069161D03*
//X0023314Y0069161D03*
//X0170814Y0146661D03*
//X0177507Y0146661D03*
//X0177507Y0182161D03*
//X0170814Y0182161D03*
//X0210814Y0184161D03*
//X0217507Y0184161D03*
//X0240814Y0094161D03*
//X0247507Y0094161D03*
//D12*
//X0246661Y0075161D03*
//X0261661Y0075161D03*
//X0261661Y0055161D03*
//X0246661Y0055161D03*
//D13*
//X0156061Y0153137D03*
//X0156061Y0156287D03*
//X0156061Y0159436D03*
//X0156061Y0162586D03*
//X0156061Y0165735D03*
//X0156061Y0168885D03*
//X0156061Y0172035D03*
//X0156061Y0175184D03*
//X0122261Y0175184D03*
//X0122261Y0172035D03*
//X0122261Y0168885D03*
//X0122261Y0165735D03*
//X0122261Y0162586D03*
//X0122261Y0159436D03*
//X0122261Y0156287D03*
//X0122261Y0153137D03*
//D14*
//X0128137Y0147261D03*
//X0131287Y0147261D03*
//X0134436Y0147261D03*
//X0137586Y0147261D03*
//X0140735Y0147261D03*
//X0143885Y0147261D03*
//X0147035Y0147261D03*
//X0150184Y0147261D03*
//X0150184Y0181061D03*
//X0147035Y0181061D03*
//X0143885Y0181061D03*
//X0140735Y0181061D03*
//X0137586Y0181061D03*
//X0134436Y0181061D03*
//X0131287Y0181061D03*
//X0128137Y0181061D03*
//D15*
//X0039161Y0114003D03*
//D16*
//X0048137Y0085263D03*
//X0030184Y0085263D03*
//D17*
//X0089830Y0088901D03*
//X0089830Y0081420D03*
//X0098491Y0085161D03*
//X0109830Y0088901D03*
//X0109830Y0081420D03*
//X0118491Y0085161D03*
//X0129830Y0088901D03*
//X0129830Y0081420D03*
//X0138491Y0085161D03*
//X0149830Y0088901D03*
//X0149830Y0081420D03*
//X0158491Y0085161D03*
//X0169830Y0088901D03*
//X0169830Y0081420D03*
//X0178491Y0085161D03*
//X0169830Y0068901D03*
//X0178491Y0065161D03*
//X0169830Y0061420D03*
//X0158491Y0065161D03*
//X0149830Y0068901D03*
//X0149830Y0061420D03*
//X0138491Y0065161D03*
//X0129830Y0068901D03*
//X0129830Y0061420D03*
//X0118491Y0065161D03*
//X0109830Y0068901D03*
//X0109830Y0061420D03*
//X0098491Y0065161D03*
//X0089830Y0068901D03*
//X0089830Y0061420D03*
//D18*
//X0048373Y0148161D03*
//X0048373Y0153161D03*
//X0048373Y0158161D03*
//X0048373Y0163161D03*
//X0048373Y0169161D03*
//X0048373Y0174161D03*
//X0048373Y0179161D03*
//X0048373Y0184161D03*
//X0039948Y0184161D03*
//X0039948Y0179161D03*
//X0039948Y0174161D03*
//X0039948Y0169161D03*
//X0039948Y0163161D03*
//X0039948Y0158161D03*
//X0039948Y0153161D03*
//X0039948Y0148161D03*
//D19*
//X0066661Y0103373D03*
//X0071661Y0103373D03*
//X0076661Y0103373D03*
//X0081661Y0103373D03*
//X0081661Y0094948D03*
//X0076661Y0094948D03*
//X0071661Y0094948D03*
//X0066661Y0094948D03*
//X0186661Y0094948D03*
//X0191661Y0094948D03*
//X0196661Y0094948D03*
//X0201661Y0094948D03*
//X0201661Y0103373D03*
//X0196661Y0103373D03*
//X0191661Y0103373D03*
//X0186661Y0103373D03*
//X0216661Y0103373D03*
//X0221661Y0103373D03*
//X0226661Y0103373D03*
//X0231661Y0103373D03*
//X0231661Y0094948D03*
//X0226661Y0094948D03*
//X0221661Y0094948D03*
//X0216661Y0094948D03*
//D20*
//X0224397Y0156661D03*
//X0224397Y0161661D03*
//X0224397Y0166661D03*
//X0224397Y0171661D03*
//X0203924Y0171661D03*
//X0203924Y0166661D03*
//X0203924Y0161661D03*
//X0203924Y0156661D03*
//D21*
//X0204661Y0076261D03*
//X0199661Y0076261D03*
//X0194661Y0076261D03*
//X0209661Y0076261D03*
//X0214661Y0076261D03*
//X0219661Y0076261D03*
//X0224661Y0076261D03*
//X0229661Y0076261D03*
//X0229661Y0052061D03*
//X0224661Y0052061D03*
//X0219661Y0052061D03*
//X0214661Y0052061D03*
//X0209661Y0052061D03*
//X0204661Y0052061D03*
//X0199661Y0052061D03*
//X0194661Y0052061D03*
//M02*
