package org.openpnp.machine.reference.driver;

import java.io.IOException;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.SimulationModeMachine.SimulationMode;
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

public abstract class AbstractReferenceDriver extends AbstractDriver {
    @Attribute(required = false)
    protected MotionControlType motionControlType = MotionControlType.ToolpathFeedRate; 

    @Element(required = false)
    protected SerialPortCommunications serial = new SerialPortCommunications();

    @Element(required = false)
    protected TcpCommunications tcp = new TcpCommunications();
    
    @Element(required = false)
    protected SimulatedCommunications simulated = new SimulatedCommunications();

    public enum CommunicationsType {
        serial, // lower case for legacy support.
        tcp
    }
    @Attribute(required = false, name = "communications")
    protected CommunicationsType communicationsType = CommunicationsType.serial;
    
    @Attribute(required = false)
    protected boolean connectionKeepAlive = false;

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
        tcp.setDriver(this);
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

    @Override
    public MotionControlType getMotionControlType() {
        return motionControlType;
    }

    public void setMotionControlType(MotionControlType motionControlType) {
        Object oldValue = this.motionControlType;
        this.motionControlType = motionControlType;
        firePropertyChange("motionControlType", oldValue, motionControlType);
    }

    public CommunicationsType getCommunicationsType() {
        return communicationsType;
    }

    public void setCommunicationsType(CommunicationsType communicationsType) {
        // If the communications type is changing we need to disconnect the old one first.
        if (communicationsType == null || communicationsType != this.communicationsType) {
            try {
                disconnect();
            }
            catch (Exception e) {
                Logger.error(e);
            }
        }
        this.communicationsType = communicationsType;
    }

    public SerialPortCommunications getSerial() {
        return serial;
    }

    public TcpCommunications getTcp() {
        return tcp;
    }

    public boolean isConnectionKeepAlive() {
    	return connectionKeepAlive;
    }
    
    public void setConnectionKeepAlive(boolean connectionKeepAlive) {
        Object oldValue = this.connectionKeepAlive;
        this.connectionKeepAlive = connectionKeepAlive;
        firePropertyChange("connectionKeepAlive", oldValue, connectionKeepAlive);
    }

    public boolean isInSimulationMode() {
        SimulationModeMachine machine = SimulationModeMachine.getSimulationModeMachine();
        return (machine != null && machine.getSimulationMode() != SimulationMode.Off);
    }

    public ReferenceDriverCommunications getCommunications() {
        if (isInSimulationMode()) {
            // Switch off keep-alive, to allow for dynamic switching. 
            setConnectionKeepAlive(false);
            simulated.setDriver(this);
            return simulated;
        }
        switch (communicationsType) {
            case serial: {
                return getSerial();
            }
            case tcp: {
                tcp.setDriver(this);
                return tcp;
            }
            default: {
                Logger.error("Invalid communications method attempted to be set. Defaulting to serial.");
                return getSerial();
            }
        }
    }

    public String getPortName() {
        return getSerial().getPortName();
    }

    public void setPortName(String portName) {
        getSerial().setPortName(portName);
    }

    public int getBaud() {
        return getSerial().getBaud();
    }

    public void setBaud(int baud) {
        getSerial().setBaud(baud);
    }

    public FlowControl getFlowControl() {
        return getSerial().getFlowControl();
    }

    public void setFlowControl(FlowControl flowControl) {
        Object oldValue = this.flowControl;
        getSerial().setFlowControl(flowControl);
        firePropertyChange("flowControl", oldValue, flowControl);
    }

    public DataBits getDataBits() {
        return getSerial().getDataBits();
    }

    public void setDataBits(DataBits dataBits) {
        getSerial().setDataBits(dataBits);
    }

    public StopBits getStopBits() {
        return getSerial().getStopBits();
    }

    public void setStopBits(StopBits stopBits) {
        getSerial().setStopBits(stopBits);
    }

    public Parity getParity() {
        return getSerial().getParity();
    }

    public void setParity(Parity parity) {
        getSerial().setParity(parity);
    }

    public boolean isSetDtr() {
        return getSerial().isSetDtr();
    }

    public void setSetDtr(boolean setDtr) {
        getSerial().setSetDtr(setDtr);
    }

    public boolean isSetRts() {
        return getSerial().isSetRts();
    }

    public void setSetRts(boolean setRts) {
        getSerial().setSetRts(setRts);
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
