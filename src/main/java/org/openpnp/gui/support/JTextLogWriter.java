package org.openpnp.gui.support;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.Writer;

public class JTextLogWriter implements Writer {
    private JTextArea textArea;

    private int lineLimit = 1000;

    public JTextLogWriter(JTextArea textArea) {
        this.textArea = textArea;
    }

    public void setLineLimit(int lineLimit) {
        this.lineLimit = lineLimit;
        trim();
    }

    public int getLineLimit() {
        return lineLimit;
    }

    @Override
    public Set<LogEntryValue> getRequiredLogEntryValues() {
        return EnumSet.of(LogEntryValue.RENDERED_LOG_ENTRY);
    }

    @Override
    public void init(final Configuration configuration) throws IOException {}

    @Override
    public void write(final LogEntry logEntry) throws IOException {
        String entry = logEntry.getRenderedLogEntry();
        SwingUtilities.invokeLater(() -> {
            textArea.append(entry);
            trim();
        });
    }

    @Override
    public void flush() throws IOException {}

    @Override
    public void close() throws IOException {}

    private void trim() {
        try {
            if (lineLimit > 0) {
                while (textArea.getLineCount() > lineLimit + 1) {
                    int end = textArea.getLineEndOffset(0);
                    textArea.replaceRange("", 0, end);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
