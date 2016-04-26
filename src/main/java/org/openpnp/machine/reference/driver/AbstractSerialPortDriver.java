package org.openpnp.machine.reference.driver;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.machine.reference.driver.wizards.AbstractSerialPortDriverConfigurationWizard;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;

/**
 * A base class for basic SerialPort based Drivers. Includes functions for connecting,
 * disconnecting, reading and sending lines.
 */
public abstract class AbstractSerialPortDriver implements ReferenceDriver, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSerialPortDriver.class);

    @Attribute(required = false)
    protected String portName;
    @Attribute(required = false)
    protected int baud = 115200;

    protected SerialPort serialPort;
    protected SerialInputStream input;
    protected OutputStream output;

    protected synchronized void connect() throws Exception {
        disconnect();
        serialPort = new SerialPort(portName);
        serialPort.openPort();
        serialPort.setParams(baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE, false, false);
        input = new SerialInputStream(serialPort);
        input.setTimeout(500);
        output = new SerialOutputStream(serialPort);
    }

    protected synchronized void disconnect() throws Exception {
        if (serialPort != null && serialPort.isOpened()) {
            serialPort.closePort();
            input = null;
            output = null;
            serialPort = null;
        }
    }

    @Override
    public void dispense(ReferencePasteDispenser dispenser, Location startLocation,
            Location endLocation, long dispenseTimeMilliseconds) throws Exception {
        // Do nothing. This is just stubbed in so that it can be released
        // without breaking every driver in the wild.
    }

    public String[] getPortNames() {
        return SerialPortList.getPortNames();
    }

    /**
     * Read a line from the serial port. Blocks for the default timeout. If the read times out a
     * TimeoutException is thrown. Any other failure to read results in an IOExeption;
     * 
     * @return
     * @throws TimeoutException
     * @throws IOException
     */
    protected String readLine() throws TimeoutException, IOException {
        StringBuffer line = new StringBuffer();
        while (true) {
            try {
                int ch = input.read();
                if (ch == -1) {
                    return null;
                }
                else if (ch == '\n' || ch == '\r') {
                    if (line.length() > 0) {
                        return line.toString();
                    }
                }
                else {
                    line.append((char) ch);
                }
            }
            catch (IOException ex) {
                if (ex.getCause() instanceof SerialPortTimeoutException) {
                    throw new TimeoutException(ex.getMessage());
                }
                throw ex;
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            disconnect();
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public int getBaud() {
        return baud;
    }

    public void setBaud(int baud) {
        this.baud = baud;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new AbstractSerialPortDriverConfigurationWizard(this);
    }
    
    /**
     * SerialInputStream and SerialOutputStream are from the pull request referenced in:
     * https://github.com/scream3r/java-simple-serial-connector/issues/17
     * 
     * If that pull request is ever merged we can update and remove these.
     */

    /**
     * Class that wraps a {@link SerialPort} to provide {@link InputStream} functionality. This
     * stream also provides support for performing blocking reads with timeouts. <br>
     * It is instantiated by passing the constructor a {@link SerialPort} instance. Do not create
     * multiple streams for the same serial port unless you implement your own synchronization.
     * 
     * @author Charles Hache <chalz@member.fsf.org>
     *
     */
    public class SerialInputStream extends InputStream {

        private SerialPort serialPort;
        private int defaultTimeout = 0;

        /**
         * Instantiates a SerialInputStream for the given {@link SerialPort} Do not create multiple
         * streams for the same serial port unless you implement your own synchronization.
         * 
         * @param sp The serial port to stream.
         */
        public SerialInputStream(SerialPort sp) {
            serialPort = sp;
        }

        /**
         * Set the default timeout (ms) of this SerialInputStream. This affects subsequent calls to
         * {@link #read()}, {@link #blockingRead(int[])}, and {@link #blockingRead(int[], int, int)}
         * The default timeout can be 'unset' by setting it to 0.
         * 
         * @param time The timeout in milliseconds.
         */
        public void setTimeout(int time) {
            defaultTimeout = time;
        }

        /**
         * Reads the next byte from the port. If the timeout of this stream has been set, then this
         * method blocks until data is available or until the timeout has been hit. If the timeout
         * is not set or has been set to 0, then this method blocks indefinitely.
         */
        @Override
        public int read() throws IOException {
            return read(defaultTimeout);
        }

        /**
         * The same contract as {@link #read()}, except overrides this stream's default timeout with
         * the given timeout in milliseconds.
         * 
         * @param timeout The timeout in milliseconds.
         * @return The read byte.
         * @throws IOException On serial port error or timeout
         */
        public int read(int timeout) throws IOException {
            byte[] buf = new byte[1];
            try {
                if (timeout > 0) {
                    buf = serialPort.readBytes(1, timeout);
                }
                else {
                    buf = serialPort.readBytes(1);
                }
                return buf[0];
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }

        /**
         * Non-blocking read of up to buf.length bytes from the stream. This call behaves as
         * read(buf, 0, buf.length) would.
         * 
         * @param buf The buffer to fill.
         * @return The number of bytes read, which can be 0.
         * @throws IOException on error.
         */
        @Override
        public int read(byte[] buf) throws IOException {
            return read(buf, 0, buf.length);
        }

        /**
         * Non-blocking read of up to length bytes from the stream. This method returns what is
         * immediately available in the input buffer.
         * 
         * @param buf The buffer to fill.
         * @param offset The offset into the buffer to start copying data.
         * @param length The maximum number of bytes to read.
         * @return The actual number of bytes read, which can be 0.
         * @throws IOException on error.
         */
        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {

            if (buf.length < offset + length)
                length = buf.length - offset;

            int available = this.available();

            if (available > length)
                available = length;

            try {
                byte[] readBuf = serialPort.readBytes(available);
                System.arraycopy(readBuf, 0, buf, offset, length);
                return readBuf.length;
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }

        /**
         * Blocks until buf.length bytes are read, an error occurs, or the default timeout is hit
         * (if specified). This behaves as blockingRead(buf, 0, buf.length) would.
         * 
         * @param buf The buffer to fill with data.
         * @return The number of bytes read.
         * @throws IOException On error or timeout.
         */
        public int blockingRead(byte[] buf) throws IOException {
            return blockingRead(buf, 0, buf.length, defaultTimeout);
        }

        /**
         * The same contract as {@link #blockingRead(byte[])} except overrides this stream's default
         * timeout with the given one.
         * 
         * @param buf The buffer to fill.
         * @param timeout The timeout in milliseconds.
         * @return The number of bytes read.
         * @throws IOException On error or timeout.
         */
        public int blockingRead(byte[] buf, int timeout) throws IOException {
            return blockingRead(buf, 0, buf.length, timeout);
        }

        /**
         * Blocks until length bytes are read, an error occurs, or the default timeout is hit (if
         * specified). Saves the data into the given buffer at the specified offset. If the stream's
         * timeout is not set, behaves as {@link #read(byte[], int, int)} would.
         * 
         * @param buf The buffer to fill.
         * @param offset The offset in buffer to save the data.
         * @param length The number of bytes to read.
         * @return the number of bytes read.
         * @throws IOException on error or timeout.
         */
        public int blockingRead(byte[] buf, int offset, int length) throws IOException {
            return blockingRead(buf, offset, length, defaultTimeout);
        }

        /**
         * The same contract as {@link #blockingRead(byte[], int, int)} except overrides this
         * stream's default timeout with the given one.
         * 
         * @param buf The buffer to fill.
         * @param offset Offset in the buffer to start saving data.
         * @param length The number of bytes to read.
         * @param timeout The timeout in milliseconds.
         * @return The number of bytes read.
         * @throws IOException On error or timeout.
         */
        public int blockingRead(byte[] buf, int offset, int length, int timeout)
                throws IOException {
            if (buf.length < offset + length)
                throw new IOException("Not enough buffer space for serial data");

            if (timeout < 1)
                return read(buf, offset, length);

            try {
                byte[] readBuf = serialPort.readBytes(length, timeout);
                System.arraycopy(readBuf, 0, buf, offset, length);
                return readBuf.length;
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public int available() throws IOException {
            int ret;
            try {
                ret = serialPort.getInputBufferBytesCount();
                if (ret >= 0)
                    return ret;
                throw new IOException("Error checking available bytes from the serial port.");
            }
            catch (Exception e) {
                throw new IOException("Error checking available bytes from the serial port.");
            }
        }

    }

    /**
     * Class that wraps a {@link SerialPort} to provide {@link OutputStream} functionality. <br>
     * It is instantiated by passing the constructor a {@link SerialPort} instance. Do not create
     * multiple streams for the same serial port unless you implement your own synchronization.
     * 
     * @author Charles Hache <chalz@member.fsf.org>
     *
     */
    public class SerialOutputStream extends OutputStream {

        SerialPort serialPort;

        /**
         * Instantiates a SerialOutputStream for the given {@link SerialPort} Do not create multiple
         * streams for the same serial port unless you implement your own synchronization.
         * 
         * @param sp The serial port to stream.
         */
        public SerialOutputStream(SerialPort sp) {
            serialPort = sp;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                serialPort.writeInt(b);
            }
            catch (SerialPortException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);

        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            byte[] buffer = new byte[len];
            System.arraycopy(b, off, buffer, 0, len);
            try {
                serialPort.writeBytes(buffer);
            }
            catch (SerialPortException e) {
                throw new IOException(e);
            }
        }
    }
}

