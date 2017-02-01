package org.openpnp.machine.reference.driver.wizards;

import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Font;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GcodeDriver.Command;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.border.EtchedBorder;
import java.awt.Color;
import javax.swing.JTextField;
import org.pmw.tinylog.Logger;

public class GcodeDriverConsole extends AbstractConfigurationWizard {
    private final GcodeDriver driver;

    public GcodeDriverConsole(GcodeDriver driver) {
        this.driver = driver;
        
        historyLen = 0;
        historyMaxLen = 5;
        history = new String[historyMaxLen];
        historyCursor = 0;
        
        JPanel gcodeConsole = new JPanel();
        gcodeConsole.setBorder(
          new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), 
            "Gcode console",TitledBorder.LEADING, 
            TitledBorder.TOP, 
            null, 
            new Color(0, 0, 0)
          )
        );
        contentPanel.add(gcodeConsole);
        
        gcodeConsole.setLayout(new FormLayout(
                new ColumnSpec[] {
                    FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                    FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                    FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                },
                new RowSpec[] {
                    FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"),
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                }
            )
        );
        
        JScrollPane scrollPane = new JScrollPane();
        gcodeConsole.add(scrollPane, "2, 2, 5, 1, fill, fill");
        
        textAreaConsole = new JTextArea();
        textAreaConsole.setFont(new Font("Monospaced", Font.PLAIN, 13));
        textAreaConsole.setEditable(false);
        textAreaConsole.setLineWrap(true);
        textAreaConsole.setRows(5);
        scrollPane.setViewportView(textAreaConsole);
        
        lblCmdLine = new JLabel("Command line:");
        gcodeConsole.add(lblCmdLine, "2, 4");
        
        cmdLineTextField = new JTextField();
        gcodeConsole.add(cmdLineTextField,"4, 4");
        
        cmdLineTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
            
              switch (e.getKeyCode()) {
                case 0:  return;
                
                case KeyEvent.VK_UP: // arrow key UP, older history
                  if (historyCursor < historyLen - 1) {
                    historyCursor++;
                  }
                  cmdLineTextField.setText(history[historyCursor]);
                  break;
                  
                case KeyEvent.VK_DOWN:  // arrow key DOWN, more recent history
                  if (historyCursor > 0) {
                    historyCursor--;
                  }
                  cmdLineTextField.setText(history[historyCursor]);
                  break;
                  
                default:
                  if (e.getKeyChar() == '\n') {
                    sendGcodeConCmd();
                  }
                  else {
                      super.keyTyped(e);
                  }
                  break;
              }
            }
          }
        );

        sendGcodeConCmdBtn = new JButton(sendGcodeConCmdAction);
        gcodeConsole.add(sendGcodeConCmdBtn, "6, 4");

    }
    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        
		}
    private JTextArea textAreaConsole;
    private JLabel lblCmdLine;
    private JTextField cmdLineTextField;
    private JButton sendGcodeConCmdBtn;
    
    private String history[];
    private int historyLen;
    private int historyMaxLen;
    private int historyCursor;
        
    private String getHistoryAtIdx() {
      return history[historyCursor];
    }
    
    private void sendGcodeConCmd() {
    
      String cmd = cmdLineTextField.getText();
      int moveLen = 0;
      
      // are we repeating the same command? Store it in history only if different than last one
      if (!cmd.equals(history[0])) {
        if (historyLen < historyMaxLen) {
          moveLen = historyLen++;
        } else {
          moveLen = historyMaxLen - 1;
        }
        // shift history one place to the past. Copy is done in-place.
        System.arraycopy(history, 0, history, 1, moveLen);
        // store current command at 0
        history[0] = cmd;
      }
      // reset historyCursor to the present
      historyCursor = 0;
      
      // most controllers prefer commands to be in upper case.
      cmd = cmd.toUpperCase();
      // display the command in the console
      textAreaConsole.append("> " + cmd + "\n");
      // Send command and get responses
      try {
        for(String line: driver.sendCommand(cmd,5000)) {
          textAreaConsole.append(line + "\n");
        }
      }
      catch (Exception ex) {
        Logger.debug("Gcode console error: " + ex);
      }
    }
    
    private Action sendGcodeConCmdAction = new AbstractAction("Send") {
        @Override
        public void actionPerformed(ActionEvent e) {
        sendGcodeConCmd();
      }
    };
}
