package org.openpnp.machine.reference.driver;

import java.util.regex.Matcher;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.model.Configuration;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for basic SerialPort based Drivers. Includes functions
 * for connecting, disconnecting, reading and sending lines. 
 */
public abstract class AbstractSerialPortDriver implements ReferenceDriver, SerialPortEventListener {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSerialPortDriver.class);
    
    @Attribute
    protected String portName;
    @Attribute
    protected int baud;
    
    protected SerialPort serialPort;
    
    public AbstractSerialPortDriver() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration)
                    throws Exception {
                connect();
            }
        });
    }
    
    protected synchronized void connect() throws Exception {
        disconnect();
        serialPort = new SerialPort(portName);
        serialPort.openPort();
        serialPort.setParams(
                baud, 
                SerialPort.DATABITS_8, 
                SerialPort.STOPBITS_1, 
                SerialPort.PARITY_NONE, 
                false, 
                false);
        serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
        serialPort.addEventListener(this);    
    }
    
    protected synchronized void disconnect() throws Exception {
        if (serialPort != null) {
            serialPort.closePort();
        }
    }
    
    /**
     * Send the given String s to the serial port.
     * @param s
     * @throws Exception
     */
    protected synchronized void send(String s) throws Exception {
        
    }
    
    /**
     * Wait for a line from the serial port to match the specified regExp. If
     * no match is found within timeout milliseconds the method returns null.
     * @param regExp
     * @param timeout
     * @return A Matcher containing the matched string or null if no match is
     * found within the timeout period.
     * @throws Exception
     */
    protected synchronized Matcher expect(String regExp, long timeout) throws Exception {
        return null;
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        if (serialPortEvent.isRXCHAR()) {
            try {
                System.out.print(new String(serialPort.readBytes()));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
