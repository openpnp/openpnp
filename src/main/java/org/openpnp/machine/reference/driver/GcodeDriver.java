package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcodeDriver extends AbstractSerialPortDriver implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GcodeDriver.class);

    @Attribute(required = false)
    protected LengthUnit units = LengthUnit.Millimeters;

    @Attribute(required = false)
    protected int maxFeedRate = 1000;

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 0;

    @Element(required = false)
    protected Location homeLocation = null;

    @Element(required = false)
    protected String commandConfirmRegex = null;

    @Element(required = false)
    protected String connectCommand = null;

    @Element(required = false)
    protected String enableCommand = null;

    @Element(required = false)
    protected String disableCommand = null;

    @Element(required = false)
    protected String homeCommand = null;

    /**
     * This command has special handling for the X, Y, Z and Rotation variables. If the move does
     * not change one of these variables that variable is replaced with the empty string, removing
     * it from the command. This allows Gcode to be sent containing only the components that are
     * being used which is important for some controllers when moving an "extruder" for the C axis.
     * The end result is that if a move contains only a change in the C axis only the C axis value
     * will be sent.
     */
    @Element(required = false)
    protected String moveToCommand = null;

    @Element(required = false)
    protected String moveToCompleteRegex = null;

    @Element(required = false)
    protected String pickCommand = null;

    @Element(required = false)
    protected String placeCommand = null;

    @Element(required = false)
    protected String actuateBooleanCommand = null;

    @Element(required = false)
    protected String actuateDoubleCommand = null;

    @ElementList(required = false)
    protected List<ReferenceDriver> subDrivers = new ArrayList<>();

    @ElementList(required = false)
    protected List<Axis> axes = new ArrayList<>();

    private Thread readerThread;
    private boolean disconnectRequested;
    private boolean connected;
    private LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    @Commit
    public void commit() {
        if (axes.isEmpty()) {
            double x = 0, y = 0, z = 0, rotation = 0;
            if (this.homeLocation != null) {
                x = homeLocation.getX();
                y = homeLocation.getY();
                z = homeLocation.getZ();
                rotation = homeLocation.getRotation();
                this.homeLocation = null;
            }
            axes.add(new Axis("x", Axis.Type.X, x, "*"));
            axes.add(new Axis("y", Axis.Type.Y, y, "*"));
            axes.add(new Axis("z", Axis.Type.Z, z, "*"));
            axes.add(new Axis("rotation", Axis.Type.Rotation, rotation, "*"));
        }
    }

    public synchronized void connect() throws Exception {
        super.connect();

        connected = false;
        readerThread = new Thread(this);
        readerThread.start();

        // Wait a bit while the controller starts up
        Thread.sleep(connectWaitTimeMilliseconds);

        // Consume any startup messages
        try {
            while (!sendCommand(null, 250).isEmpty());
        }
        catch (Exception e) {

        }

        // Disable the machine
        setEnabled(false);

        // Send startup Gcode
        sendGcode(connectCommand);

        connected = true;
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
        if (connected) {
            if (enabled) {
                sendGcode(enableCommand);
            }
            else {
                sendGcode(disableCommand);
            }
        }

        for (ReferenceDriver driver : subDrivers) {
            driver.setEnabled(enabled);
        }
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        // Home is sent with an infinite timeout since it's tough to tell how long it will
        // take.
        String command = homeCommand;
        command = substituteVariable(command, "Id", head.getId());
        command = substituteVariable(command, "Name", head.getName());
        sendGcode(command, -1);

        for (Axis axis : axes) {
            axis.setCoordinate(axis.getHomeCoordinate());
        }

        for (ReferenceDriver driver : subDrivers) {
            driver.home(head);
        }
    }

    public Axis getAxis(HeadMountable hm, Axis.Type type) {
        for (Axis axis : axes) {
            if (axis.getType() == type && (axis.getHeadMountableIds().contains("*")
                    || axis.getHeadMountableIds().contains(hm.getId()))) {
                return axis;
            }
        }
        return null;
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        Axis xAxis = getAxis(hm, Axis.Type.X);
        Axis yAxis = getAxis(hm, Axis.Type.Y);
        Axis zAxis = getAxis(hm, Axis.Type.Z);
        Axis rotationAxis = getAxis(hm, Axis.Type.Rotation);

        Location location =
                new Location(units, xAxis == null ? 0 : xAxis.getTransformedCoordinate(hm),
                        yAxis == null ? 0 : yAxis.getTransformedCoordinate(hm),
                        zAxis == null ? 0 : zAxis.getTransformedCoordinate(hm),
                        rotationAxis == null ? 0 : rotationAxis.getTransformedCoordinate(hm))
                                .add(hm.getHeadOffsets());
        return location;
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        location = location.convertToUnits(units);

        location = location.subtract(hm.getHeadOffsets());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double rotation = location.getRotation();

        Axis xAxis = getAxis(hm, Axis.Type.X);
        Axis yAxis = getAxis(hm, Axis.Type.Y);
        Axis zAxis = getAxis(hm, Axis.Type.Z);
        Axis rotationAxis = getAxis(hm, Axis.Type.Rotation);

        // Handle NaNs, which means don't move this axis for this move. We set the appropriate
        // axis reference to null, which we'll check for later.
        if (Double.isNaN(x)) {
            xAxis = null;
        }
        if (Double.isNaN(y)) {
            yAxis = null;
        }
        if (Double.isNaN(z)) {
            zAxis = null;
        }
        if (Double.isNaN(rotation)) {
            rotationAxis = null;
        }

        // If no axes are included in the move, there's nothing to do, so just return.
        if (xAxis == null && yAxis == null && zAxis == null && rotationAxis == null) {
            return;
        }

        // For each included axis, if the axis has a transform, transform the target coordinate to
        // it's raw value.
        if (xAxis != null && xAxis.getTransform() != null) {
            x = xAxis.getTransform().toRaw(xAxis, hm, x);
        }
        if (yAxis != null && yAxis.getTransform() != null) {
            y = yAxis.getTransform().toRaw(yAxis, hm, y);
        }
        if (zAxis != null && zAxis.getTransform() != null) {
            z = zAxis.getTransform().toRaw(zAxis, hm, z);
        }
        if (rotationAxis != null && rotationAxis.getTransform() != null) {
            rotation = rotationAxis.getTransform().toRaw(rotationAxis, hm, rotation);
        }

        boolean emptyMove = true;

        String command = moveToCommand;
        command = substituteVariable(command, "Id", hm.getId());
        command = substituteVariable(command, "Name", hm.getName());
        command = substituteVariable(command, "FeedRate", maxFeedRate * speed);

        if (xAxis == null || xAxis.getCoordinate() == x) {
            command = substituteVariable(command, "X", null);
        }
        else {
            command = substituteVariable(command, "X", x);
            emptyMove = false;
        }

        if (yAxis == null || yAxis.getCoordinate() == y) {
            command = substituteVariable(command, "Y", null);
        }
        else {
            command = substituteVariable(command, "Y", y);
            emptyMove = false;
        }

        if (zAxis == null || zAxis.getCoordinate() == z) {
            command = substituteVariable(command, "Z", null);
        }
        else {
            command = substituteVariable(command, "Z", z);
            emptyMove = false;
        }

        if (rotationAxis == null || rotationAxis.getCoordinate() == rotation) {
            command = substituteVariable(command, "Rotation", null);
        }
        else {
            command = substituteVariable(command, "Rotation", rotation);
            emptyMove = false;
        }

        // No axes were included in the move, so there is nothing to do.
        if (emptyMove) {
            return;
        }

        List<String> responses = sendGcode(command);

        /*
         * If moveToCompleteRegex is specified we need to wait until we match the regex in a
         * response before continuing. We first search the initial responses from the command for
         * the regex. If it's not found we then collect responses for up to timeoutMillis while
         * searching the responses for the regex. As soon as it is matched we continue. If it's not
         * matched within the timeout we throw an Exception.
         */
        if (moveToCompleteRegex != null) {
            if (!containsMatch(responses, moveToCompleteRegex)) {
                long t = System.currentTimeMillis();
                boolean done = false;
                while (!done && System.currentTimeMillis() - t < timeoutMilliseconds) {
                    done = containsMatch(sendCommand(null, 250), moveToCompleteRegex);
                }
                if (!done) {
                    throw new Exception("Timed out waiting for move to complete.");
                }
            }
        }

        // And save the final values on the axes.
        if (xAxis != null) {
            xAxis.setCoordinate(x);
        }
        if (yAxis != null) {
            yAxis.setCoordinate(y);
        }
        if (zAxis != null) {
            zAxis.setCoordinate(z);
        }
        if (rotationAxis != null) {
            rotationAxis.setCoordinate(rotation);
        }

        for (ReferenceDriver driver : subDrivers) {
            driver.moveTo(hm, location, speed);
        }
    }

    private boolean containsMatch(List<String> responses, String regex) {
        for (String response : responses) {
            if (response.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        String command = pickCommand;
        command = substituteVariable(command, "Id", nozzle.getId());
        command = substituteVariable(command, "Name", nozzle.getName());
        sendGcode(command);

        for (ReferenceDriver driver : subDrivers) {
            driver.pick(nozzle);
        }
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        String command = placeCommand;
        command = substituteVariable(command, "Id", nozzle.getId());
        command = substituteVariable(command, "Name", nozzle.getName());
        sendGcode(command);

        for (ReferenceDriver driver : subDrivers) {
            driver.place(nozzle);
        }
    }


    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        String command = actuateBooleanCommand;
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        command = substituteVariable(command, "Index", actuator.getIndex());
        command = substituteVariable(command, "BooleanValue", on);
        command = substituteVariable(command, "True", on ? on : null);
        command = substituteVariable(command, "False", on ? null : on);
        sendGcode(command);

        for (ReferenceDriver driver : subDrivers) {
            driver.actuate(actuator, on);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        String command = actuateDoubleCommand;
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        command = substituteVariable(command, "Index", actuator.getIndex());
        command = substituteVariable(command, "DoubleValue", value);
        command = substituteVariable(command, "IntegerValue", (int) value);
        sendGcode(command);

        for (ReferenceDriver driver : subDrivers) {
            driver.actuate(actuator, value);
        }
    }

    public synchronized void disconnect() {
        disconnectRequested = true;
        connected = false;

        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.join();
            }
        }
        catch (Exception e) {
            logger.error("disconnect()", e);
        }

        try {
            super.disconnect();
        }
        catch (Exception e) {
            logger.error("disconnect()", e);
        }
        disconnectRequested = false;
    }

    @Override
    public void close() throws IOException {
        super.close();

        for (ReferenceDriver driver : subDrivers) {
            driver.close();
        }
    }

    protected List<String> sendGcode(String gCode) throws Exception {
        return sendGcode(gCode, timeoutMilliseconds);
    }

    protected List<String> sendGcode(String gCode, long timeout) throws Exception {
        if (gCode == null) {
            return new ArrayList<>();
        }
        List<String> responses = new ArrayList<>();
        for (String command : gCode.split("\n")) {
            command = command.trim();
            if (command.length() == 0) {
                continue;
            }
            responses.addAll(sendCommand(command, timeout));
        }
        return responses;
    }

    protected List<String> sendCommand(String command) throws Exception {
        return sendCommand(command, timeoutMilliseconds);
    }

    protected List<String> sendCommand(String command, long timeout) throws Exception {
        List<String> responses = new ArrayList<>();

        // Read any responses that might be queued up so that when we wait
        // for a response to a command we actually wait for the one we expect.
        responseQueue.drainTo(responses);

        logger.debug("sendCommand({}, {})...", command, timeout);

        // Send the command, if one was specified
        if (command != null) {
            logger.trace(">> " + command);
            output.write(command.getBytes());
            output.write("\n".getBytes());
        }

        // Collect responses till we find one with the confirmation or we timeout. Return
        // the collected responses.
        if (timeout == -1) {
            timeout = Long.MAX_VALUE;
        }
        long t = System.currentTimeMillis();
        boolean found = false;
        // Loop until we've timed out
        while (System.currentTimeMillis() - t < timeout) {
            // Wait to see if a response came in. We wait up until the number of millis remaining
            // in the timeout.
            String response = responseQueue.poll(timeout - (System.currentTimeMillis() - t),
                    TimeUnit.MILLISECONDS);
            // If no response yet, try again.
            if (response == null) {
                continue;
            }
            // Store the response that was received
            responses.add(response);
            // If the response is an ok or error we're done
            if (response.matches(commandConfirmRegex)) {
                found = true;
                break;
            }
        }
        // If a command was specified and no confirmation was found it's a timeout error.
        if (command != null && !found) {
            throw new Exception("Timeout waiting for response to " + command);
        }

        // Read any additional responses that came in after the initial one.
        responseQueue.drainTo(responses);

        logger.debug("sendCommand({}, {}) => {}",
                new Object[] {command, timeout == Long.MAX_VALUE ? -1 : timeout, responses});
        return responses;
    }

    public void run() {
        while (!disconnectRequested) {
            String line;
            try {
                line = readLine().trim();
            }
            catch (TimeoutException ex) {
                continue;
            }
            catch (IOException e) {
                logger.error("Read error", e);
                return;
            }
            line = line.trim();
            logger.trace("<< " + line);
            responseQueue.offer(line);
        }
    }

    /**
     * Find matches of variables in the format {Name:Format} and replace them with the specified
     * value formatted using String.format with the specified Format. Format is optional and
     * defaults to %s. A null value replaces the variable with "".
     */
    static protected String substituteVariable(String command, String name, Object value) {
        if (command == null) {
            return command;
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = Pattern.compile("\\{(\\w+)(?::(.+?))?\\}").matcher(command);
        while (matcher.find()) {
            String n = matcher.group(1);
            if (!n.equals(name)) {
                continue;
            }
            String format = matcher.group(2);
            if (format == null) {
                format = "%s";
            }
            String v = "";
            if (value != null) {
                v = String.format((Locale) null, format, value);
            }
            matcher.appendReplacement(sb, v);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<>();
        if (!subDrivers.isEmpty()) {
            children.add(new SimplePropertySheetHolder("Sub-Drivers", subDrivers));
        }
        return children.toArray(new PropertySheetHolder[] {});
    }

    public static class Axis {
        public enum Type {
            X,
            Y,
            Z,
            Rotation
        };

        @Attribute
        private String name;

        @Attribute
        private Type type;

        @Attribute(required = false)
        private double homeCoordinate = 0;

        @ElementList(required = false)
        private Set<String> headMountableIds = new HashSet<String>();

        @Element(required = false)
        private AxisTransform transform;

        /**
         * Stores the current value for this axis.
         */
        private double coordinate = 0;

        public Axis() {

        }

        public Axis(String name, Type type, double homeCoordinate, String... headMountableIds) {
            this.name = name;
            this.type = type;
            this.homeCoordinate = homeCoordinate;
            this.headMountableIds.addAll(Arrays.asList(headMountableIds));
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public double getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(double coordinate) {
            this.coordinate = coordinate;
        }
        
        public double getHomeCoordinate() {
            return homeCoordinate;
        }

        public void setHomeCoordinate(double homeCoordinate) {
            this.homeCoordinate = homeCoordinate;
        }

        public double getTransformedCoordinate(HeadMountable hm) {
            if (this.transform != null) {
                return transform.toTransformed(this, hm, this.coordinate);
            }
            return this.coordinate;
        }

        public Set<String> getHeadMountableIds() {
            return headMountableIds;
        }

        public void setHeadMountableIds(Set<String> headMountableIds) {
            this.headMountableIds = headMountableIds;
        }

        public AxisTransform getTransform() {
            return transform;
        }

        public void setTransform(AxisTransform transform) {
            this.transform = transform;
        }
    }

    public interface AxisTransform {
        /**
         * Transform the specified raw coordinate into it's corresponding transformed coordinate.
         * The transformed coordinate is what the user sees, while the raw coordinate is what the
         * motion controller sees.
         * 
         * @param hm
         * @param rawCoordinate
         * @return
         */
        public double toTransformed(Axis axis, HeadMountable hm, double rawCoordinate);

        /**
         * Transform the specified transformed coordinate into it's corresponding raw coordinate.
         * The transformed coordinate is what the user sees, while the raw coordinate is what the
         * motion controller sees.
         * 
         * @param hm
         * @param transformedCoordinate
         * @return
         */
        public double toRaw(Axis axis, HeadMountable hm, double transformedCoordinate);
    }

    /**
     * An AxisTransform for heads with dual linear Z axes powered by one motor. The two Z axes are
     * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
     * value negated. So, as normal moves up, negated moves down.
     */
    public static class NegatingTransform implements AxisTransform {
        @Element
        private String negatedHeadMountableId;

        @Override
        public double toTransformed(Axis axis, HeadMountable hm, double rawCoordinate) {
            if (hm.getId().equals(negatedHeadMountableId)) {
                return -rawCoordinate;
            }
            return rawCoordinate;
        }

        @Override
        public double toRaw(Axis axis, HeadMountable hm, double transformedCoordinate) {
            // Since we're just negating the value of the coordinate we can just
            // use the same function.
            return toTransformed(axis, hm, transformedCoordinate);
        }
    }
}
