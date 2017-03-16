package org.openpnp.gui.support;

import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.Writer;

import javax.swing.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A LogEntry List Model which contains LogEntries from tinylog
 */
public class LogEntryListModel extends AbstractListModel<LogEntry> implements Writer {

    private List<LogEntry> originalLogEntries = new ArrayList<>();
    private List<LogEntry> filteredLogEntries = new ArrayList<>(originalLogEntries);
    private HashSet<LogEntryFilter> filters = new HashSet<>();

    public static class LogEntryFilter {
        Predicate<LogEntry> filter;

        public LogEntryFilter() {
            this.filter = (logEntry) -> true;
        }

        public LogEntryFilter(Predicate<LogEntry> filter) {
            this.filter = filter;
        }

        Predicate<LogEntry> getFilter() {
            return filter;
        }

        public void setFilter(Predicate<LogEntry> filter) {
            this.filter = filter;
        }
    }

    private static final int LINE_LIMIT = 10000;

    public List<LogEntry> getOriginalLogEntries() {
        return originalLogEntries;
    }

    public List<LogEntry> getFilteredLogEntries() {
        return filteredLogEntries;
    }

    @Override
    public int getSize() {
        return filteredLogEntries.size();
    }

    @Override
    public LogEntry getElementAt(int index) {
        return filteredLogEntries.get(index);
    }

    public void addFilter(LogEntryFilter filter) {
        this.filters.add(filter);
        filter();
    }

    public void removeFilter(LogEntryFilter filter) {
        this.filters.remove(filter);
        filter();
    }

    @Override
    public Set<LogEntryValue> getRequiredLogEntryValues() {
        return EnumSet.of(LogEntryValue.RENDERED_LOG_ENTRY);
    }

    @Override
    public void init(Configuration configuration) throws Exception {

    }

    @Override
    public void write(LogEntry logEntry) throws Exception {
        originalLogEntries.add(logEntry);
        trim();
    }

    public void clear() {
        this.originalLogEntries.clear();
        filter();
    }

    public void filter() {
        // Reduce all filters to a single one and apply it to our logEntries
        this.filteredLogEntries = originalLogEntries.stream().filter(
                filters.stream().map(LogEntryFilter::getFilter).reduce(Predicate::and).orElse(t -> false)
        ).collect(Collectors.toList());

        SwingUtilities.invokeLater(() -> fireContentsChanged(this, 0, filteredLogEntries.size() - 1));
    }

    private void trim() {
        if (originalLogEntries.size() > LINE_LIMIT) {
            originalLogEntries.subList(0, originalLogEntries.size() - LINE_LIMIT).clear();
        }
        filter();
    }

    @Override
    public void flush() throws Exception {

    }

    @Override
    public void close() throws Exception {

    }
}
