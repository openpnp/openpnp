package org.openpnp.gui.support;

import org.pmw.tinylog.LogEntry;

import javax.swing.*;
import java.awt.*;

public class LogEntryListCellRenderer extends JTextField implements ListCellRenderer<LogEntry> {
    final Color colorTrace = new Color(64, 128, 64);
    final Color colorDebug = new Color(0, 0, 0);
    final Color colorInfo = new Color(00, 0x5B, 0xD9); // the OpenPNP blue
    final Color colorWarning = new Color(255, 0, 0);
    final Color colorError = new Color(255, 0, 0);
    final Color colorErrorBg = new Color(255, 255, 220);
    
    @Override
    public Component getListCellRendererComponent(JList<? extends LogEntry> list, LogEntry logEntry, int index, boolean isSelected, boolean cellHasFocus) {

        if (logEntry == null) {
            return this;
        }

        this.setText(logEntry.getRenderedLogEntry());
        this.setFont(new Font("Monospaced", Font.PLAIN, 13));
        this.setBorder(null);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            switch(logEntry.getLevel()) {
                case ERROR:
                    setBackground(colorErrorBg);
                    break;
                default:
                    setBackground(list.getBackground());
            }
            switch(logEntry.getLevel()) {
                case TRACE:
                    setForeground(colorTrace);
                    break;
                case DEBUG:
                    setForeground(colorDebug);
                    break;
                case INFO:
                    setForeground(colorInfo);
                    break;
                case WARNING:
                    setForeground(colorWarning);
                    break;
                case ERROR:
                    setForeground(colorError);
                    break;
                default:
                    setForeground(list.getForeground());
                    break;
            }
        }

        return this;
    }
}