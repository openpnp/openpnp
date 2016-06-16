package org.openpnp.gui.support;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class UiAppender extends AppenderSkeleton {
    private JTextArea textArea;
    
    private int lineLimit = 1000;
    
    public UiAppender(JTextArea textArea) {
        this.textArea = textArea;
    }
    
    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
    
    public void setLineLimit(int lineLimit) {
        this.lineLimit = lineLimit;
    }

    @Override
    protected void append(LoggingEvent event) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(getLayout().format(event));
            try {
                while (textArea.getLineCount() > lineLimit + 1) {
                    int end = textArea.getLineEndOffset(0);
                    textArea.replaceRange("", 0, end);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
