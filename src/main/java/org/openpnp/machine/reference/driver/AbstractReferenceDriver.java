package org.openpnp.machine.reference.driver;

import java.io.IOException;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.driver.SerialPortCommunications.DataBits;
import org.openpnp.machine.reference.driver.SerialPortCommunications.FlowControl;
import org.openpnp.machine.reference.driver.SerialPortCommunications.Parity;
import org.openpnp.machine.reference.driver.SerialPortCommunications.StopBits;
import org.openpnp.machine.reference.driver.wizards.AbstractReferenceDriverConfigurationWizard;
import org.openpnp.spi.base.AbstractDriver;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public abstract class AbstractReferenceDriver extends AbstractDriver implements ReferenceDriver {
    @Element(required = false)
    protected SerialPortCommunications serial = new SerialPortCommunications();

    @Element(required = false)
    protected TcpCommunications tcp = new TcpCommunications();

    @Attribute(required = false, name = "communications")
    protected String communicationsType = "serial";
    
    @Attribute(required = false)
    protected boolean connectionKeepAlive = true;

    /**
     * TODO The following properties are for backwards compatibility and can be removed after 2019-07-15. 
     */
    @Attribute(required = false)
    protected String portName;

    @Attribute(required = false)
    protected Integer baud = 115200;

    @Attribute(required = false)
    protected FlowControl flowControl = FlowControl.Off;

    @Attribute(required = false)
    protected DataBits dataBits = DataBits.Eight;

    @Attribute(required = false)
    protected StopBits stopBits = StopBits.One;

    @Attribute(required = false)
    protected Parity parity = Parity.None;

    @Attribute(required = false)
    protected Boolean setDtr = false;

    @Attribute(required = false)
    protected Boolean setRts = false;
    
    public AbstractReferenceDriver() {
    }
    
    @Commit
    public void commit() {
        if (portName != null && !portName.isEmpty()) {
            setPortName(this.portName);
            setBaud(this.baud);
            setFlowControl(this.flowControl);
            setDataBits(this.dataBits);
            setStopBits(this.stopBits);
            setParity(this.parity);
            setSetDtr(this.setDtr);
            setSetRts(this.setRts);            
        }
        this.portName = null;
        this.baud = null;
        this.flowControl = null;
        this.dataBits = null;
        this.stopBits = null;
        this.parity = null;
        this.setDtr = null;
        this.setRts = null;

        setCommunicationsType(communicationsType);
        setConnectionKeepAlive(connectionKeepAlive);
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
    
    public abstract void disconnect() throws Exception;
    
    public String getCommunicationsType() {
        return communicationsType;
    }

    public void setCommunicationsType(String communicationsType) {
        // If the communications type is changing we need to disconnect the old one first.
        if (communicationsType == null || !communicationsType.equals(this.communicationsType)) {
            try {
                disconnect();
            }
            catch (Exception e) {
                Logger.error(e);
            }
        }
        this.communicationsType = communicationsType;
    }
    
    public boolean getConnectionKeepAlive() {
    	return connectionKeepAlive;
    }
    
    public void setConnectionKeepAlive(boolean connectionKeepAlive) {
    	this.connectionKeepAlive = connectionKeepAlive;
    }
    
    public ReferenceDriverCommunications getCommunications() {
        switch (communicationsType) {
            case "serial": {
                return serial;
            }
            case "tcp": {
                return tcp;
            }
            default: {
                Logger.error("Invalid communications method attempted to be set. Defaulting to serial.");
                return serial;
            }
        }
    }

    public String getPortName() {
        return serial.getPortName();
    }

    public void setPortName(String portName) {
        serial.setPortName(portName);
    }

    public int getBaud() {
        return serial.getBaud();
    }

    public void setBaud(int baud) {
        serial.setBaud(baud);
    }

    public FlowControl getFlowControl() {
        return serial.getFlowControl();
    }

    public void setFlowControl(FlowControl flowControl) {
        serial.setFlowControl(flowControl);
    }

    public DataBits getDataBits() {
        return serial.getDataBits();
    }

    public void setDataBits(DataBits dataBits) {
        serial.setDataBits(dataBits);
    }

    public StopBits getStopBits() {
        return serial.getStopBits();
    }

    public void setStopBits(StopBits stopBits) {
        serial.setStopBits(stopBits);
    }

    public Parity getParity() {
        return serial.getParity();
    }

    public void setParity(Parity parity) {
        serial.setParity(parity);
    }

    public boolean isSetDtr() {
        return serial.isSetDtr();
    }

    public void setSetDtr(boolean setDtr) {
        serial.setSetDtr(setDtr);
    }

    public boolean isSetRts() {
        return serial.isSetRts();
    }

    public void setSetRts(boolean setRts) {
        serial.setSetRts(setRts);
    }

    public String getIpAddress() {
        return tcp.getIpAddress();
    }

    public void setIpAddress(String ipAddress) {
        tcp.setIpAddress(ipAddress);
    }

    public int getPort() {
        return tcp.getPort();
    }

    public void setPort(int port) {
        tcp.setPort(port);
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[]{new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new AbstractReferenceDriverConfigurationWizard(this);
    }

    
    // Replaces each backslash escaped sequence within a string with its actual unicode character
    public static String unescape(String s) {
        int i = 0;
        char c;
        int len = s.length();
        StringBuffer sb = new StringBuffer(len);
        while (i<len) {
             c = s.charAt(i++);
             if (c == '\\') {
                  if (i<len) {
                       c = s.charAt(i++);
                       if ((c == 'u') || (c == 'U')) {
                          try {
                               c = (char) Integer.parseInt(s.substring(i,i+4),16);
                               i += 4;
                          }
                          catch (Exception e) {
                              //the escaped unicode character doesn't have the correct form (four hexidecimal digits) so just pass it along
                              //as a string
                              sb.append('\\');
                          }
                       }
                       else if ((c == 't') || (c == 'T')) {
                           c = 0x0009; //unicode tab
                       }
                       else if ((c == 'b') || (c == 'B')) {
                           c = 0x0008; //unicode backspace
                       }
                       else if ((c == 'n') || (c == 'N')) {
                           c = 0x000A; //unicode line feed
                       }
                       else if ((c == 'r') || (c == 'R')) {
                           c = 0x000D; //unicode carriage return
                       }
                       else if ((c == 'f') || (c == 'F')) {
                           c = 0x000C; //unicode form feed
                       }
                       else {
                           //in all other cases just pass the backslash along
                           sb.append('\\');
                       }
                  }
             }
        sb.append(c);
        }
        return sb.toString();
    }
}
